package com.casualmiracles.transaction.catseffect

import cats.effect.IO
import com.casualmiracles.transaction.{Run, Transaction, TransactionRunner}

object TransactionIORunner extends TransactionRunner[IO] {
  override def unsafeRun[E, A](transaction: Transaction[IO, E, A]): Either[Throwable, Run[E, A]] =
    transaction.runF.attempt.unsafeRunSync()
}
