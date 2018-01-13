package com.casualmiracles.transaction

import cats.data.{EitherT, ReaderWriterStateT}
import cats.instances.list._
import cats.syntax.all._
import cats.Monad

/**
  * This is a stack of EitherT over ReaderWriterStateT that supports
  * text logging, post commit functions as state and a value of Either[Throwable, A],
  * with F as the effect.
  */
class TransactionBuilder[F[_], E](implicit val monadF: Monad[F]) {

  type Transaction[A] = EitherT[ReaderWriterStateT[F, List[String], List[String], List[PostRun], ?], E, A]

  def apply[A](s: ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]]): Transaction[A] =
    EitherT(s)

  def point[A](a: A): Transaction[A] =
    liftEither(Right[E, A](a))

  def liftEither[A](e: Either[E, A]): Transaction[A] = {
    val value: ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]] = ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]]((c, p) ⇒ monadF.point((c, p, e)))
    apply(value)
  }

  def liftS[A](s: ReaderWriterStateT[F, List[String], List[String], List[PostRun], A]): Transaction[A] = {
    EitherT.liftF(s)
  }

  def liftF[A](s: F[A]): Transaction[A] = apply {
    ReaderWriterStateT[F, List[String], List[String], List[PostRun], Either[E, A]] {
      case (_, st) => monadF.map(s)(v ⇒ (List.empty[String], st, Right[E, A](v)))
    }
  }

  def log(msg: String): Transaction[Unit] = liftS {
    ReaderWriterStateT[F, List[String], List[String], List[PostRun], Unit] {
      case (_, s) => (List(msg), s, ()).pure[F]
    }
  }

  def postRun(description: String, f: () ⇒ Unit): Transaction[Unit] =
    postRun(PostRun(description, f))

  def postRun(pc: PostRun): Transaction[Unit] =
    liftS {
      ReaderWriterStateT[F, List[String], List[String], List[PostRun], Unit] {
        case (_, s) => (List.empty[String], s :+ pc, ()).pure[F]
      }
    }

  implicit class TransactionSyntax[A](transaction: Transaction[A]) {

    def postRun(description: String, f: () ⇒ Unit): Transaction[A] =
      postRun(PostRun(description, f))

    def postRun(pc: PostRun): Transaction[A] =
      apply(transaction.value.modify(pcs ⇒ pc :: pcs))

//TODO the log and PostRun descriptions need to be used?
    def unsafeRun(implicit runner: TransactionRunner[F]): Result[E, A] =
      runner.unsafeRun(transaction) match {
        case Left(t) ⇒ Result.Error(t)

        case Right((_, _, Left(e))) ⇒
          Result.Failure(e)

        case Right((_, f, Right(a))) ⇒
          f.foreach(_.f())
          Result.Success(a)
      }
  }
}