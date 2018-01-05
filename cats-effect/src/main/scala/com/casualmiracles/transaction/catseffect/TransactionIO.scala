package com.casualmiracles.transaction.catseffect

import cats.effect.IO
import com.casualmiracles.transaction.{TransactionBuilder, TransactionF}

object TransactionIO {

  private def builder[E] = new TransactionBuilder[IO, E]

  def fromEither[E, A](value: Either[E, A]): TransactionF[IO, E, A] =
    builder[E].liftEither(value)

  def success[E, A](value: A): TransactionF[IO, E, A] =
    fromEither(Right[E, A](value))

  def failure[E, A](err: E): TransactionF[IO, E, A] =
    fromEither(Left[E, A](err))

  def lift[E, A](value: IO[A]): TransactionF[IO, E, A] =
    builder[E].liftF(value)

//  def onSuccess[E](f: () ⇒ Unit): TransactionF[IO, E, Unit] =
//    Transaction.onSuccess(f)
//
//  def onFailure[E](f: () ⇒ Unit): TransactionF[IO, E, Unit] =
//    Transaction.onFailure(f)
//
}
