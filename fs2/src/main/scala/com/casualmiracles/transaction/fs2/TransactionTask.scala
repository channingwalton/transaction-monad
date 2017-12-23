package com.casualmiracles.transaction.fs2

import _root_.fs2.Task
import _root_.fs2.interop.cats._
import com.casualmiracles.transaction.{Run, Transaction}

object TransactionTask {

  def fromEither[E, A](value: Either[E, A]): Transaction[Task, E, A] =
    Transaction(Task.delay(Run(value)))

  def const[E, A](value: A): Transaction[Task, E, A] =
    fromEither(Right[E, A](value))

  def failure[E, A](err: E): Transaction[Task, E, A] =
    fromEither(Left[E, A](err))

  /**
    * Run a transaction, executing side-effects on success or failure.
    * Note that if the attempt to run the task completely fails with an Exception,
    * the failure tasks cannot be run since they aren't available.
    *
    * @param transaction to run
    * @tparam E error type
    * @tparam A success typ
    * @return Either a success or a failure. A failure can either be a Throwable or an E.
    */
  def unsafeAttemptRun[E, A](transaction: Transaction[Task, E, A]): Either[Either[Throwable, E], A] =
    transaction.unsafeAttemptRun()

  implicit class TransactionTaskOps[E, A](transaction: Transaction[Task, E, A]) {

    def unsafeAttemptRun(): Either[Either[Throwable, E], A] = {
      val res: Either[Throwable, Run[E, A]] = transaction.run.unsafeAttemptRun()

      res match {
        case Left(t) ⇒
          Left(Left(t))

        case Right(Run(Left(e), f, _)) ⇒
          f.run()
          Left(Right(e))

        case Right(Run(Right(a), _, s)) ⇒
          s.run()
          Right(a)
      }
    }
  }
}