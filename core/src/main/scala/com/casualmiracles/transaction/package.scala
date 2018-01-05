package com.casualmiracles

import cats.data.{EitherT, ReaderWriterStateT}

package object transaction {
  type TransactionStateF[F[_], A] = ReaderWriterStateT[F, List[String], List[String], List[PostCommit], A]
  type TransactionF[F[_], E, A] = EitherT[TransactionStateF[F, ?], E, A]

  type Result[E, A] = (List[String], List[PostCommit], Either[E, A])
}
