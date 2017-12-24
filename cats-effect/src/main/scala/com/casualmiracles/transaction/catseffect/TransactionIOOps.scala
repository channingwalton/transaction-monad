package com.casualmiracles.transaction.catseffect

import cats.effect.IO
import com.casualmiracles.transaction.Transaction

trait TransactionIOOps {

  implicit class Ops[E, A](transaction: Transaction[IO, E, A]) {
    def unsafeAttemptRun(): RunResult[E, A] =
      TransactionIO.unsafeAttemptRun(transaction)
  }
}

object TransactionIOOps extends TransactionIOOps
