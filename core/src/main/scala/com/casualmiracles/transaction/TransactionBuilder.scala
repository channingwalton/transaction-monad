package com.casualmiracles.transaction

import cats.data.{EitherT, ReaderWriterStateT}
import cats.instances.list._
import cats.syntax.all._
import cats.{Applicative, Id, Monad}

final case class PostCommit(description: String, item: () => Unit)

/**
  * This is a stack of EitherT over ReaderWriterStateT that supports
  * text logging, post commit functions as state and a value of Either[Throwable, A],
  * with F as the effect.
  */
class TransactionBuilder[F[_], E](implicit monadF: Monad[F]) {

  type TransactionState[A] = TransactionStateF[F, A]
  type Transaction[A] = TransactionF[F, E, A]

  def apply[A](s: TransactionState[Either[E, A]]): Transaction[A] = EitherT(s)

  def liftEither[A](e: Either[E, A]): Transaction[A] = apply(Applicative[TransactionState].point(e))

  def liftS[A](s: TransactionState[A]): Transaction[A] = EitherT.liftF(s)

  def liftF[A](s: F[A]): Transaction[A] = apply {
    ReaderWriterStateT[F, List[String], List[String], List[PostCommit], Either[E, A]] {
      case (_, st) => monadF.map(s)(v ⇒ (List.empty[String], st, Right[E, A](v)))
    }
  }

  def log(msg: String): Transaction[Unit] = liftS {
    ReaderWriterStateT[F, List[String], List[String], List[PostCommit], Unit] {
      case (_, s) => (List(msg), s, ()).pure[F]
    }
  }

  def postCommit(pc: PostCommit): Transaction[Unit] =
    liftS {
      ReaderWriterStateT[F, List[String], List[String], List[PostCommit], Unit] {
        case (_, s) => (List.empty[String], s :+ pc, ()).pure[F]
      }
    }

  implicit class PCStateESSyntax[A](s: Transaction[A]) {
    def add(pc: PostCommit): Transaction[A] =
      apply(s.value.modify(pcs ⇒ pc :: pcs))
  }
}

object TransactionTest extends App {
  val transactionBuilder = new TransactionBuilder[Id, Throwable]
  import transactionBuilder._

  def run[A](pcs: Transaction[A]): Either[Throwable, A] = {
    val res = pcs.value.run(Nil, Nil)
      println(("LOGS:" :: res._1).mkString("\n"))
    println("\nPostCommit functions:")
    res._2.foreach {
      case PostCommit(desc, f) ⇒
        println(s"Running: $desc")
        f()
        println(s"Ran: $desc")
    }
    res._3
  }

  val x =
    for {
      _ <- log("Hi")
      y ← liftEither(Right("Some stuff")).add(PostCommit("did some stuff", () ⇒ println("  Did some stuff.")))
      _ <- log("Bye")
      _ ← postCommit(PostCommit("all done", () ⇒ println("  We did it!")))
    } yield y

  println("\nResult:\n" + run(x))

/*
LOGS:
Hi
Bye

PostCommit functions:
Running: did some stuff
  Did some stuff.
Ran: did some stuff
Running: all done
  We did it!
Ran: all done

Result:
Right(Some stuff)
*/
}