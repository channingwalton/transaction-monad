package com.casualmiracles.transaction.experiments

import cats.data.{EitherT, ReaderWriterStateT}
import cats.instances.list._
import cats.syntax.all._
import cats.{Applicative, Id, Monad}

final case class PostCommit(description: String, item: () => Unit)

/**
  * Is the MonadTransformer Stack easier?
  *
  * This is a stack of EitherT over ReaderWriterState that supports
  * text logging, post commit functions as state and a value of Either[Throwable, A],
  * with Id as the effect.
  *
  * This was based on slide 88+ in https://speakerdeck.com/mpilquist/scalaz-state-monad
  */
class TransactionBuilder[F[_]: Monad] {

  type TransactionState[A] = ReaderWriterStateT[F, List[String], List[String], List[PostCommit], A]
  type Transaction[A] = EitherT[TransactionState, Throwable, A]

  object Transaction {
    def apply[A](s: TransactionState[Either[Throwable, A]]): Transaction[A] = EitherT(s)

    def liftEither[A](e: Either[Throwable, A]): Transaction[A] = apply(Applicative[TransactionState].point(e))

    def liftS[A](s: TransactionState[A]): Transaction[A] = EitherT.liftF(s)

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
        Transaction(s.value.modify(pcs ⇒ pc :: pcs))
    }
  }
}

object TransactionTest extends App {
  val transactionBuilder = new TransactionBuilder[Id]
  import transactionBuilder._
  import transactionBuilder.Transaction._

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