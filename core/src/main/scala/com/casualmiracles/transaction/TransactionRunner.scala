package com.casualmiracles.transaction

import cats.Id

/**
  * A typeclass for running a transaction in some effect, F.
  */
trait TransactionRunner[F[_]] {

  /**
    * @param transaction to run
    * @tparam E error type
    * @tparam A success type
    * @return Throwable (non fatal) or a Run[E, A]
    */
  def unsafeRun[E, A](transaction: TransactionF[F, E, A]): Either[Throwable, RunResult[E, A]]
}

object TransactionRunner {
  implicit def identityRunner: TransactionRunner[Id] = new TransactionRunner[Id] {
    override def unsafeRun[E, A](transaction: TransactionF[Id, E, A]): Either[Throwable, RunResult[E, A]] =
      Right(transaction.value.runF(Nil, Nil))
  }
}