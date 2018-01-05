package com.casualmiracles.transaction.experiments

import cats.data.{EitherT, ReaderWriterState}
import cats.instances.list._
import cats.syntax.all._
import cats.{Applicative, Id}

/**
  * Is the MonadTransformer Stack easier?
  *
  * This is a stack of EitherT over ReaderWriterState that supports
  * text logging, post commit functions as state and a value of Either[Throwable, A],
  * with Id as the effect.
  *
  * This was based on slide 88+ in https://speakerdeck.com/mpilquist/scalaz-state-monad
  */
object MTStack {
  final case class PostCommit(description: String, item: () => Unit)

  type TransactionState[A] = ReaderWriterState[List[String], List[String], List[PostCommit], A]
  type ET[F[_], A] = EitherT[F, Throwable, A]
  type Transaction[A] = ET[TransactionState, A]

  object PCStateES {
    def apply[A](s: TransactionState[Either[Throwable, A]]): Transaction[A] = EitherT(s)

    def liftEither[A](e: Either[Throwable, A]): Transaction[A] = apply(Applicative[TransactionState].point(e))

    def liftS[A](s: TransactionState[A]): Transaction[A] = EitherT.liftF(s)

    def log(msg: String): Transaction[Unit] = liftS {
      ReaderWriterState[List[String], List[String], List[PostCommit], Unit] {
        case (_, s) => (List(msg), s, ()).pure[Id]
      }
    }

    def postCommit(pc: PostCommit): Transaction[Unit] =
      liftS {
        ReaderWriterState[List[String], List[String], List[PostCommit], Unit] {
          case (_, s) => (Nil, s :+ pc, ()).pure[Id]
        }
      }

    implicit class PCStateESSyntax[A](s: Transaction[A]) {
      def add(pc: PostCommit): Transaction[A] =
        PCStateES(s.value.modify(pcs ⇒ pc :: pcs))
    }
  }

  def run[A](pcs: Transaction[A]): Either[Throwable, A] = {
    val res = pcs.value.run(Nil, Nil).value
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
}

object MTStackTest extends App {
  import MTStack._
  import PCStateES._

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