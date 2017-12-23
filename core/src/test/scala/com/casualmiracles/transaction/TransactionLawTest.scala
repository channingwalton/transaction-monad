package com.casualmiracles.transaction

import cats._
import cats.laws.discipline._
import cats.tests._
import org.scalacheck._

class TransactionLawTest extends CatsSuite {

  type TestTransaction[A] = Transaction[Id, String, A]
  def testTransaction[A](a: A): TestTransaction[A] =
    Transaction[Id, String, A]((Right[String, A](a), PostCommit(), PostCommit()))

  implicit val F: Monad[Id] = implicitly[Monad[Id]]
  implicit val catsMonad: Monad[TestTransaction] = new Monad[TestTransaction] {
    override def pure[A](x: A): TestTransaction[A] =
      Transaction.const(x)

    override def flatMap[A, B](fa: TestTransaction[A])(f: A ⇒ TestTransaction[B]): TestTransaction[B] =
      fa.flatMap(f)

    override def tailRecM[A, B](a: A)(f: A ⇒ Transaction[Id, String, Either[A, B]]): Transaction[Id, String, B] =
      Transaction(F.tailRecM(a)(a0 => F.map(f(a0).run) {
        case (Left(l), c, d)         => Right((Left(l), c, d))
        case (Right(Left(a1)), _, _) => Left(a1)
        case (Right(Right(b)), c, d) => Right((Right(b), c, d))
      }))
  }

  implicit def dataEqForTransaction[A]: Eq[TestTransaction[A]] =
    (x: TestTransaction[A], y: TestTransaction[A]) =>
      x.run._1 == y.run._1

  implicit def arbitraryTransaction[A: Arbitrary]: Arbitrary[TestTransaction[A]] =
    Arbitrary(
      Arbitrary.arbitrary[A].map(testTransaction)
    )


  checkAll("Transaction.MonadLaws", MonadTests[TestTransaction].monad[Int, String, Double])
}
