package com.casualmiracles.transaction

import cats.data.EitherT
import cats.Monad

/**
  * This is a builder for the monad transformer stack of EitherT over ReaderWriterStateT
  * that helps you construct TransactionF without having to specify all the types
  * all the time which is noisy and tedious.
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

  type TransState[A]  = TransactionStateF[F, A]
  type Transaction[A] = EitherT[TransState, E, A]


  def point[A](a: A): Transaction[A] =
    pointF(a)

  def success[A](a: A): Transaction[A] =
    successF(a)

  def failure[A](e: E): Transaction[A] =
    failureF(e)

  def liftEither[A](e: Either[E, A]): Transaction[A] =
    liftEitherF(e)

  def liftOption[A](option: Option[A], error: E): Transaction[A] =
    liftOptionF(option, error)

  def lift[A](s: F[A]): Transaction[A] =
    liftF(s)

  def log(msg: String): Transaction[Unit] =
    logF(msg)

  def postRun(f: () ⇒ Unit): Transaction[Unit] =
    postRunF(f)

  def postRun(pc: PostRun): Transaction[Unit] =
    postRunF(pc)

  implicit class TransactionSyntax[A](transaction: Transaction[A]) {

    def postRun(f: () ⇒ Unit): Transaction[A] =
      postRun(PostRun(f))

    def postRun(pc: PostRun): Transaction[A] =
      applyF(transaction.value.modify(pcs ⇒ pc :: pcs))

    def log(msg: String): Transaction[A] =
      applyF(transaction.value.mapWritten(_ :+ msg))

    /**
      * Apply a monadic function and discard the result while keeping the effect - the so-called Kestrel combinator.
      */
    def flatTap[B](f: A => Transaction[B]): Transaction[A] =
      transaction.flatMap(t => f(t).map(_ => t))

    /**
      * alias for [[flatTap]]
      */
    def <|[B](f: A => Transaction[B]): Transaction[A] =
      flatTap(f)

    /**
      * Apply a function to the value returning the original transaction
      */
    def tap(f: A => Unit): Transaction[A] =
      transaction.map(t => { f(t); t })

    /**
      * Discard the value.
      */
    def void: Transaction[Unit] =
      transaction.map(_ => ())

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
