package com.casualmiracles.transaction

import cats.Monad


final case class Transaction[F[_] : Monad, E, A](run: F[(Either[E, A], List[() ⇒ Unit], List[() ⇒ Unit])]) {
  private val monadF = implicitly[Monad[F]]

  def postSuccess(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(run)(v ⇒ (v._1, f :: v._2, v._3)))

  def postFailure(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(run)(v ⇒ (v._1, v._2, f :: v._3)))

  def map[B](f: A ⇒ B): Transaction[F, E, B] =
    Transaction(monadF.map(run)(v ⇒ (v._1.map(f), v._2, v._3)))

  def flatMap[B](f: A ⇒ Transaction[F, E, B]): Transaction[F, E, B] = {

    def runFunc(r: (Either[E, A], List[() ⇒ Unit], List[() ⇒ Unit])): F[(Either[E, B], List[() ⇒ Unit], List[() ⇒ Unit])] = {
      def handRight(t: Transaction[F, E, B]): F[(Either[E, B], List[() ⇒ Unit], List[() ⇒ Unit])] = {
        monadF.map(t.run) { runRight ⇒
          (runRight._1, r._2 ::: runRight._2, r._3 ::: runRight._3)
        }
      }

      def handleLeft(l: E): F[(Either[E, B], List[() ⇒ Unit], List[() ⇒ Unit])] = {
        monadF.pure((Left(l), r._2, r._3))
      }

      val newTrans: Either[E, Transaction[F, E, B]] = r._1.map(f)

      newTrans.fold(handleLeft, handRight)
    }

    val newRun: F[(Either[E, B], List[() ⇒ Unit], List[() ⇒ Unit])] =
      monadF.flatMap(run)(runFunc)

    Transaction(newRun)
  }
}