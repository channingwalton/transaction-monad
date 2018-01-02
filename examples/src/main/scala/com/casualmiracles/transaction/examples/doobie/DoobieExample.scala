package com.casualmiracles.transaction.examples.doobie

import cats.Monad
import com.casualmiracles.transaction.{RunResult, Transaction}
import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import doobie.h2.H2Transactor

object DoobieExample extends App {

  // The T R A N S A C T O R !
  val xa: H2Transactor[IO] = H2Transactor.newH2Transactor[IO]("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "").unsafeRunSync()

  val create: Update0 =
    sql"""
    CREATE TABLE carpark (
      ship VARCHAR NOT NULL UNIQUE,
      duration  INT
    )
  """.update
  create.run.transact(xa).unsafeRunSync

  // Fix the error type to a String and the effect to IO
  type ExampleTransaction[A] = Transaction[IO, String, A]

  // we need a Monad[ExampleTransaction] for the syntax used below - just because we can
  implicit def transactionMonad: Monad[ExampleTransaction] = Transaction.transactionMonad[IO, String]

  object Store {

    // something to convert from a ConnectionIO to an ExampleTransaction
    implicit class ToExampleTransaction[A](conn: ConnectionIO[A]) {
      def transaction: ExampleTransaction[A] =
        Transaction.lift(conn.transact(xa))
    }

    def meaningOfLife: ExampleTransaction[Int] =
      42.pure[ConnectionIO].transaction

    def parkShip(ship: String, duration: Int): ExampleTransaction[Boolean] = {
      val update: ConnectionIO[Boolean] = sql"insert into carpark (ship, duration) values ($ship, $duration)".update.run.map(_ == 1)
      update.transaction
    }
  }

  // use the store with a post commit
  def parkTheShipMarvin(ship: String, duration: Int): ExampleTransaction[Boolean] = {
    def complainAbout(meaningOfLife: Int): ExampleTransaction[Unit] =
      Transaction.onSuccess[IO, String](() ⇒ println(s"I know the meaning of life is $meaningOfLife, I have the brain the size of a planet. And I'm a parking attendant."))

    // using syntax >>= (flatMap) and >> which is flatMap discarding its argument
    (Store.meaningOfLife >>= complainAbout) >> Store.parkShip(ship, duration)
  }

  // Park a ship
  import com.casualmiracles.transaction.catseffect.runner

  val res: RunResult[String, Boolean] = parkTheShipMarvin("enterprise", 1).unsafeAttemptRun

  // the console will show the result after the println above in parkTheShipMarvin
  println(res)
}
