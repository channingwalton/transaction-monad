package com.casualmiracles

import cats.{ Applicative, Functor, Monad }
import cats.data.{ EitherT, ReaderWriterStateT }
import cats.kernel.Monoid
import cats.syntax.applicative._

package object transaction {

  /** This is here so that when you `import com.casualmiracles.transaction._` you will
    * not suffer the pain of misleading compiler errors about missing implicit
    * instances for Monad[...] which are really because the Monoid[List[String\]\] is missing.
    */
  implicit val listMonoid: Monoid[List[String]] = cats.instances.all.catsKernelStdMonoidForList

  type TransactionStateF[F[_], A] = ReaderWriterStateT[F, List[String], List[String], List[PostRun], A]

  /**
    * The Transaction type
    * @tparam F the effect
    * @tparam E the error type
    * @tparam A the value type
    */
  type TransactionF[F[_], E, A] = EitherT[TransactionStateF[F, ?], E, A]

  /**
    * A convenient type that describes the final result after the effect has been discharged.
    */
  type RunResult[E, A] = (List[String], List[PostRun], Either[E, A])

  /**
    * An alias for successF.
    */
  def pointF[F[_]: Applicative, E, A](a: A): TransactionF[F, E, A] =
    successF(a)

  /**
    * Construct a successful TransactionF. The value will be on the Right.
    */
  def successF[F[_]: Applicative, E, A](a: A): TransactionF[F, E, A] =
    liftEitherF(Right[E, A](a))

  /**
    * Construct a failed TransactionF. The value will be on the Right.
    */
  def failureF[F[_]: Applicative, E, A](e: E): TransactionF[F, E, A] =
    liftEitherF(Left[E, A](e))

  /**
    * Construct a TransactionF with an Either.
    */
  def liftEitherF[F[_], E, A](e: Either[E, A])(implicit monadF: Applicative[F]): TransactionF[F, E, A] =
    applyF(ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]]((c, p) ⇒ monadF.point((c, p, e))))

  private def applyF[F[_], E, A](s: ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]]): TransactionF[F, E, A] =
    EitherT(s)

  /**
    * Construct a TransactionF from an Option.
    * If a Some it will be a successful TransactionF, if None it will be an unsuccessful TransactionF with the provided error.
    */
  def liftOptionF[F[_]: Applicative, E, A](option: Option[A], error: E): TransactionF[F, E, A] =
    liftEitherF(option.fold[Either[E, A]](Left(error))(Right(_)))

  /**
    * Construct a successful TransactionF with a value in an effect F.
    */
  def liftF[F[_], E, A](s: F[A])(implicit monadF: Applicative[F]): TransactionF[F, E, A] = applyF {
    ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]] {
      case (_, st) => monadF.map(s)(v ⇒ (List.empty[String], st, Right[E, A](v)))
    }
  }

  /**
    * Construct a TransactionF with a log value.
    */
  def logF[F[_]: Applicative, E, A](msg: String): TransactionF[F, E, Unit] = liftSF {
    ReaderWriterStateT[F, List[String], List[String], List[PostRun], Unit] {
      case (_, s) => (List(msg), s, ()).pure[F]
    }
  }

  /**
    * Construct a TransactionF with a post run value.
    */
  def postRunF[F[_]: Applicative, E, A](f: () ⇒ Unit): TransactionF[F, E, Unit] =
    postRunF[F, E, A](PostRun(f))

  /**
    * Construct a TransactionF with a post run value.
    */
  def postRunF[F[_]: Applicative, E, A](pc: PostRun): TransactionF[F, E, Unit] =
    liftSF {
      ReaderWriterStateT[F, List[String], List[String], List[PostRun], Unit] {
        case (_, s) => (List.empty[String], s :+ pc, ()).pure[F]
      }
    }

  private def liftSF[F[_]: Functor, E, A](s: ReaderWriterStateT[F, List[String], List[String], List[PostRun], A]): TransactionF[F, E, A] =
    EitherT.liftF(s)

  /**
    * Run the transaction with the provided runner.
    */
  def unsafeRunF[F[_], E, A](transaction: TransactionF[F, E, A])(implicit runner: TransactionRunner[F]): Result[E, A] =
    runner.unsafeRun(transaction) match {
      case Left(t) ⇒ Result.Error(t)

      case Right((logs, _, Left(e))) ⇒
        Result.Failure(logs, e)

      case Right((logs, f, Right(a))) ⇒
        f.foreach(_.f())
        Result.Success(logs, a)
    }

  /**
    * Syntax for working with TransactionF instances.
    */
  implicit class TransactionSyntax[F[_]: Monad, E, A](transaction: TransactionF[F, E, A]) {

    def postRun(f: () ⇒ Unit): TransactionF[F, E, A] =
      postRun(PostRun(f))

    def postRun(pc: PostRun): TransactionF[F, E, A] =
      applyF(transaction.value.modify(pcs ⇒ pc :: pcs))

    def log(msg: String): TransactionF[F, E, A] =
      applyF(transaction.value.mapWritten(_ :+ msg))

    /**
      * Apply a monadic function and discard the result while keeping the effect - the so-called Kestrel combinator.
      */
    def flatTap[B](f: A => TransactionF[F, E, B]): TransactionF[F, E, A] =
      transaction.flatMap(t => f(t).map(_ => t))

    /**
      * alias for [[flatTap]]
      */
    def <|[B](f: A => TransactionF[F, E, B]): TransactionF[F, E, A] =
      flatTap(f)

    /**
      * Apply a function to the value returning the original transaction
      */
    def tap(f: A => Unit): TransactionF[F, E, A] =
      transaction.map(t => { f(t); t })

    /**
      * Discard the value.
      */
    def void: TransactionF[F, E, Unit] =
      transaction.map(_ => ())

    def unsafeRun(implicit runner: TransactionRunner[F]): Result[E, A] =
      unsafeRunF(transaction)
  }
}
