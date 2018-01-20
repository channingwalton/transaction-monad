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
}
