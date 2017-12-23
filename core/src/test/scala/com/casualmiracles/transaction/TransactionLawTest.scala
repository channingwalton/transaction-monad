package com.casualmiracles.transaction

import cats._
import cats.laws.discipline._
import cats.tests._
import org.scalacheck._

class TransactionLawTest extends CatsSuite {

  type TestTransaction[A] = Transaction[Id, String, A]

  implicit def transactionMonad: Monad[TestTransaction] = Transaction.transactionMonad[Id, String]

  def testTransaction[A](a: A): TestTransaction[A] =
    Transaction[Id, String, A](Run(Right[String, A](a)))

  implicit def dataEqForTransaction[A]: Eq[TestTransaction[A]] =
    (x: TestTransaction[A], y: TestTransaction[A]) =>
      x.run.res == y.run.res

  implicit def arbitraryTransaction[A: Arbitrary]: Arbitrary[TestTransaction[A]] =
    Arbitrary(
      Arbitrary.arbitrary[A].map(testTransaction)
    )


  checkAll("Transaction.MonadLaws", MonadTests[TestTransaction].monad[Int, String, Double])
}
