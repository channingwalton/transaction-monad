package com.casualmiracles.transaction.examples.doobie

import com.casualmiracles.transaction.Transaction
import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._

object DoobieExample {

  // The T R A N S A C T O R !
  val xa = Transactor.fromDriverManager[IO]("org.postgresql.Driver", "jdbc:postgresql:world", "postgres", "")

  // Fix the error type to a String and the effect to IO
  type ExampleTransaction[A] = Transaction[IO, String, A]

  object Store {

    // something to convert from a ConnectionIO to an ExampleTransaction
    implicit class ToExampleTransaction[A](conn: ConnectionIO[A]) {
      def transaction: ExampleTransaction[A] =
        Transaction.lift(conn.transact(xa))
    }

    def meaningOfLife: ExampleTransaction[Int] =
      42.pure[ConnectionIO].transaction

    def parkShip(ship: String, duration: Int): ExampleTransaction[Int] = {
      val update: ConnectionIO[Int] = sql"insert into car park (ship, duration) values ($ship, $duration)".update.run
      Transaction.lift(update.transact(xa))
    }
  }

  // use the store with a post commit
  def parkTheShipMarvin(ship: String, duration: Int): ExampleTransaction[Boolean] =
    for {
      life ← Store.meaningOfLife
      _ ← Transaction.onSuccess[IO, String](() ⇒ println(s"I know the meaning of life is $life, I have the brain the size of a planet. And I'm a parking attendant."))
      parked ← Store.parkShip(ship, duration)
    } yield parked == 1
}
