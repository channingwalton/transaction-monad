package com.casualmiracles.transaction

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
  def unsafeRun[E, A](transaction: Transaction[F, E, A]): Either[Throwable, Run[E, A]]
}
