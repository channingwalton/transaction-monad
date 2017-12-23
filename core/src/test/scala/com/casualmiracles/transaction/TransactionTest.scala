package com.casualmiracles.transaction

import cats.Id
import org.scalatest.{EitherValues, FreeSpec, MustMatchers}

class TransactionTest extends FreeSpec with MustMatchers with EitherValues {

  type TestTransaction[A] = Transaction[Id, String, A]

  def testTransaction[A](a: A): TestTransaction[A] =
    Transaction[Id, String, A](Run(Right[String, A](a), PostRun(), PostRun()))

  "post commits" - {
    "must be carried by map" in {
      val pc1 = () ⇒ ()
      val pc2 = () ⇒ ()

      val t = Transaction[Id, String, Int](Run(Right[String, Int](1), PostRun(List(pc1)), PostRun(List(pc2))))

      val m = t.map(_ * 2)

      m.run.onFailure.fs.size mustBe 1
      m.run.onFailure.fs.head mustBe pc1
      m.run.onSuccess.fs.size mustBe 1
      m.run.onSuccess.fs.head mustBe pc2
    }

    "must be concatenated by flatMap" in {
      val pc1 = () ⇒ ()
      val pc2 = () ⇒ ()
      val pc3 = () ⇒ ()
      val pc4 = () ⇒ ()

      val t1 = Transaction.pure[Id, String, Int](1).onSuccess(pc1).onFailure(pc2)
      val t2 = Transaction.pure[Id, String, Int](1).onSuccess(pc3).onFailure(pc4)

      val res = t1.flatMap(_ ⇒ t2)

      res.run.onFailure.fs.size mustBe 2
      res.run.onFailure.fs mustBe List(pc2, pc4)
      res.run.onSuccess.fs.size mustBe 2
      res.run.onSuccess.fs mustBe List(pc1, pc3)
    }
  }
}
