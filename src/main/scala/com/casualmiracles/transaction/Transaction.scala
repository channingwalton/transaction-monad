package com.casualmiracles.transaction

import cats.Monad

final case class Transaction[F[_] : Monad, E, A](run: F[(Either[E, A], PostCommit, PostCommit)]) {

  type Run[T] = (Either[E, T], PostCommit, PostCommit)

  private val monadF = implicitly[Monad[F]]

  def postSuccess(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(run)(v ⇒ (v._1, f :: v._2, v._3)))

  def postFailure(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(run)(v ⇒ (v._1, v._2, f :: v._3)))

  def map[B](f: A ⇒ B): Transaction[F, E, B] =
    Transaction(monadF.map(run)(v ⇒ (v._1.map(f), v._2, v._3)))

  def flatMap[B](f: A ⇒ Transaction[F, E, B]): Transaction[F, E, B] =
    Transaction {
      monadF.flatMap(run) {
        thisRun: Run[A] ⇒ {
          thisRun._1.fold(
            // this is a left so we don't flatMap it but it does need to be
            // cast to the right type which works because its a Left, the Right
            // doesn't actually exist
            _ ⇒ this.asInstanceOf[Transaction[F, E, B]].run,

            // this is a right so we can flatMap it
            (thisRightValue: A) ⇒ {
              val newTransaction: Transaction[F, E, B] = f(thisRightValue)

              // carry the existing post commit actions over to the result
              monadF.map(newTransaction.run) { newRun: Run[B] ⇒
                (newRun._1, thisRun._2 ::: newRun._2, thisRun._3 ::: newRun._3)
              }
            }
          )
        }
      }
    }

}

object Transaction {
  def const[F[_], E, A](value: A)(implicit monad: Monad[F]): Transaction[F, E, A] =
    Transaction(monad.pure((Right(value), PostCommit(), PostCommit())))
}