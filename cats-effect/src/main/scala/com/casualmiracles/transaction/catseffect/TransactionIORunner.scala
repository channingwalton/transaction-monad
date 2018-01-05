package com.casualmiracles.transaction.catseffect

import cats.effect.IO
import com.casualmiracles.transaction.{Result, TransactionF, TransactionRunner}

object TransactionIORunner extends TransactionRunner[IO] {
  override def unsafeRun[E, A](transaction: TransactionF[IO, E, A]): Either[Throwable, Result[E, A]] =
    transaction.value.run(Nil, Nil).attempt.unsafeRunSync()
}
