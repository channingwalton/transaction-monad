package com.casualmiracles.transaction.catseffect

import cats.Monad
import cats.effect.IO
import com.casualmiracles.transaction.{Run, Transaction}

object TransactionIO {

  def fromEither[E, A](value: Either[E, A]): Transaction[IO, E, A] =
    Transaction(IO(Run(value)))

  def success[E, A](value: A): Transaction[IO, E, A] =
    fromEither(Right[E, A](value))

  def failure[E, A](err: E): Transaction[IO, E, A] =
    fromEither(Left[E, A](err))

  def lift[E, A](value: IO[A]): Transaction[IO, E, A] =
    Transaction.lift(value)

  def onSuccess[E](f: () ⇒ Unit): Transaction[IO, E, Unit] =
    Transaction.onSuccess(f)

  def onFailure[E](f: () ⇒ Unit): Transaction[IO, E, Unit] =
    Transaction.onFailure(f)

  implicit def transactionIOMonad[E]: Monad[Transaction[IO, E, ?]] =
    Transaction.transactionMonad[IO, E]
}
