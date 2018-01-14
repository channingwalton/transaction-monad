package com.casualmiracles.transaction

import cats.data.{EitherT, ReaderWriterStateT}
import cats.syntax.all._
import cats.Monad
import cats.kernel.Monoid

/**
  * This is a builder for the monad transformer stack of EitherT over ReaderWriterStateT.
  *
  * Once you've constructed this builder, you can import `builder._` so that
  * you can use its methods, its syntax, and get the list monoid so you don't
  * need to remember to import it from cats. That last point is important
  * because if you don't get the list monoid then you will get complex
  * complication errors about missing implicit instances for Monad that
  * will mislead and frustrate you.
  *
  */
class TransactionBuilder[F[_], E](implicit val monadF: Monad[F]) {

  type TransState[A] = TransactionStateF[F, A]
  type Transaction[A] = EitherT[TransState, E, A]

  /** This is here so that when the builder is used, you can `import builder._`
   * and not suffer the pain of misleading compiler errors about missing implicit
   * instances for Monad[...]
   */
  implicit val listMonoid: Monoid[List[String]] = cats.instances.all.catsKernelStdMonoidForList

  def point[A](a: A): Transaction[A] =
    success(a)

  def success[A](a: A): Transaction[A] =
    liftEither(Right[E, A](a))

  def failure[A](e: E): Transaction[A] =
    liftEither(Left[E, A](e))

  def liftEither[A](e: Either[E, A]): Transaction[A] =
    apply(ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]]((c, p) ⇒ monadF.point((c, p, e))))

  def liftOption[A](option: Option[A], error: E): Transaction[A] =
    liftEither(option.fold[Either[E, A]](Left(error))(Right(_)))

  def liftF[A](s: F[A]): Transaction[A] = apply {
    ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]] {
      case (_, st) => monadF.map(s)(v ⇒ (List.empty[String], st, Right[E, A](v)))
    }
  }

  def log(msg: String): Transaction[Unit] = liftS {
    ReaderWriterStateT[F, List[String], List[String], List[PostRun], Unit] {
      case (_, s) => (List(msg), s, ()).pure[F]
    }
  }

  def postRun(f: () ⇒ Unit): Transaction[Unit] =
    postRun(PostRun(f))

  def postRun(pc: PostRun): Transaction[Unit] =
    liftS {
      ReaderWriterStateT[F, List[String], List[String], List[PostRun], Unit] {
        case (_, s) => (List.empty[String], s :+ pc, ()).pure[F]
      }
    }

  private def liftS[A](s: ReaderWriterStateT[F, List[String], List[String], List[PostRun], A]): Transaction[A] =
    EitherT.liftF(s)

  private def apply[A](s: ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]]): Transaction[A] =
    EitherT(s)

  implicit class TransactionSyntax[A](transaction: Transaction[A]) {

    def postRun(f: () ⇒ Unit): Transaction[A] =
      postRun(PostRun(f))

    def postRun(pc: PostRun): Transaction[A] =
      apply(transaction.value.modify(pcs ⇒ pc :: pcs))

    def log(msg: String): Transaction[A] =
      apply(transaction.value.mapWritten(_ :+ msg))

    def unsafeRun(implicit runner: TransactionRunner[F]): Result[E, A] =
      runner.unsafeRun(transaction) match {
        case Left(t) ⇒ Result.Error(t)

        case Right((logs, _, Left(e))) ⇒
          Result.Failure(logs, e)

        case Right((logs, f, Right(a))) ⇒
          f.foreach(_.f())
          Result.Success(logs, a)
      }
  }
}