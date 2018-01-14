package com.casualmiracles

import cats.Functor
import cats.data.{ EitherT, ReaderWriterStateT }

package object transaction {

  type TransactionStateF[F[_], A] = ReaderWriterStateT[F, List[String], List[String], List[PostRun], A]
  type TransactionF[F[_], E, A]   = EitherT[TransactionStateF[F, ?], E, A]

  type RunResult[E, A] = (List[String], List[PostRun], Either[E, A])

  implicit class TransactionFSyntax[F[_]: Functor, E, A](trans: TransactionF[F, E, A]) {
    def onSuccess(f: () ⇒ Unit): TransactionF[F, E, A] =
      EitherT { trans.value.modify(_ :+ PostRun(f)) }
  }
}
