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
        r: Run[A] ⇒ {
          r._1.map(f).fold(
            (l: E) ⇒ monadF.pure((Left(l), r._2, r._3)),
            (t: Transaction[F, E, B]) ⇒ monadF.map(t.run) { runRight: Run[B] ⇒
              (runRight._1, r._2 ::: runRight._2, r._3 ::: runRight._3)
            }
          )
        }
      }
    }
}