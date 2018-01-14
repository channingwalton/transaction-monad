package com.casualmiracles.transaction.examples.doobie

import com.casualmiracles.transaction.TransactionBuilder
import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import com.casualmiracles.transaction.catseffect.TransactionIO
import doobie.h2.H2Transactor

object DoobieExample extends App {

  val builder = new TransactionBuilder[IO, String]()

  // This will import the Transaction[_] type, and the list monoid
  import builder._

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

  object Store {

    // something to convert from a ConnectionIO to an ExampleTransaction
    implicit class ToExampleTransaction[A](conn: ConnectionIO[A]) {
      def transaction: Transaction[A] =
        TransactionIO.lift(conn.transact(xa))
    }

    def meaningOfLife: Transaction[Int] =
      42.pure[ConnectionIO].transaction

    def parkShip(ship: String, duration: Int): Transaction[Boolean] = {
      val update: ConnectionIO[Boolean] = sql"insert into carpark (ship, duration) values ($ship, $duration)".update.run.map(_ == 1)
      update.transaction
    }
  }

  // use the store with a post commit
  def parkTheShipMarvin(ship: String, duration: Int): Transaction[Boolean] = {
    def complainAbout(meaningOfLife: Int): Transaction[Unit] =
      builder.postRun(
        () ⇒ println(s"I know the meaning of life is $meaningOfLife, I have the brain the size of a planet. And I'm a parking attendant.")
      )

    // using syntax >>= (flatMap) and >> which is flatMap discarding its argument
    (Store.meaningOfLife >>= complainAbout) >> Store.parkShip(ship, duration)
  }

  // Park a ship
  import com.casualmiracles.transaction.catseffect.runner

  val res = parkTheShipMarvin("enterprise", 1).unsafeRun

  // the console will show the result after the println above in parkTheShipMarvin
  println(res)
  /*
  I know the meaning of life is 42, I have the brain the size of a planet. And I'm a parking attendant.
  Success(List(),true)
   */
}
