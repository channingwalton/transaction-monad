package com.casualmiracles.transaction

import cats.Monad

final case class Body[E, A](res: Either[E, A], onFailure: PostCommit = PostCommit(), onSuccess: PostCommit = PostCommit()) {

  def map[B](f: A ⇒ B): Body[E, B] =
    copy(res = res.map(f))

  def appendFailure(pc: PostCommit): Body[E, A] =
    copy(onFailure = onFailure ++ pc)

  def appendSuccess(pc: PostCommit): Body[E, A] =
    copy(onSuccess = onSuccess ++ pc)
}

final case class Transaction[F[_] : Monad, E, A](run: F[Body[E, A]]) {

  private val monadF = implicitly[Monad[F]]

  def postSuccess(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(run)(_.appendSuccess(PostCommit(f))))

  def postFailure(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(run)(_.appendFailure(PostCommit(f))))

  def map[B](f: A ⇒ B): Transaction[F, E, B] =
    Transaction(monadF.map(run)(_.map(f)))

  def flatMap[B](f: A ⇒ Transaction[F, E, B]): Transaction[F, E, B] =
    Transaction {
      monadF.flatMap(run) {
        thisRun: Body[E, A] ⇒ {
          thisRun.res.fold(
            // this is a left so we don't flatMap it but it does need to be
            // cast to the right type which works because its a Left, the Right
            // doesn't actually exist
            _ ⇒ this.asInstanceOf[Transaction[F, E, B]].run,

            // this is a right so we can flatMap it
            (thisRightValue: A) ⇒ {
              val newTransaction: Transaction[F, E, B] = f(thisRightValue)

              // carry the existing post commit actions over to the result
              monadF.map(newTransaction.run) { newRun: Body[E, B] ⇒
                Body(newRun.res, thisRun.onFailure ++ newRun.onFailure, thisRun.onSuccess ++ newRun.onSuccess)
              }
            }
          )
        }
      }
    }
}

object Transaction {
  def const[F[_], E, A](value: A)(implicit monad: Monad[F]): Transaction[F, E, A] =
    Transaction(monad.pure(Body(Right(value), PostCommit(), PostCommit())))
}