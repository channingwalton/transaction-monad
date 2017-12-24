package com.casualmiracles.transaction.catseffect

import cats.effect.IO
import com.casualmiracles.transaction.{Run, Transaction}

object TransactionIO {

  def fromEither[E, A](value: Either[E, A]): Transaction[IO, E, A] =
    Transaction(IO(Run(value)))

  def const[E, A](value: A): Transaction[IO, E, A] =
    fromEither(Right[E, A](value))

  def failure[E, A](err: E): Transaction[IO, E, A] =
    fromEither(Left[E, A](err))

  /**
    * Run a transaction, executing side-effects on success or failure.
    * Note that if the attempt to run the IO completely fails with an Exception,
    * the failure IOs cannot be run since they aren't available.
    *
    * @param transaction to run
    * @tparam E error type
    * @tparam A success typ
    * @return a RunResult.
    */
  def unsafeAttemptRun[E, A](transaction: Transaction[IO, E, A]): RunResult[E, A] = {
    transaction.run.attempt.unsafeRunSync() match {
      case Left(t) ⇒ RunResult.Error(t)

      case Right(Run(Left(e), f, _)) ⇒
        f.run()
        RunResult.Failure(e)

      case Right(Run(Right(a), _, s)) ⇒
        s.run()
        RunResult.Success(a)
    }
  }
}
