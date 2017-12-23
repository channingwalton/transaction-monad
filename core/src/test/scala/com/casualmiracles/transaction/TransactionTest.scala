package com.casualmiracles.transaction

import cats.Id
import org.scalatest.{EitherValues, FreeSpec, MustMatchers}

class TransactionTest extends FreeSpec with MustMatchers with EitherValues {

  type TestTransaction[A] = Transaction[Id, String, A]

  def testTransaction[A](a: A): TestTransaction[A] =
    Transaction[Id, String, A]((Right[String, A](a), PostCommit(), PostCommit()))

  "post commits" - {
    "must be carried by map" in {
      val pc1 = () ⇒ ()
      val pc2 = () ⇒ ()

      val t = Transaction[Id, String, Int]((Right[String, Int](1), PostCommit(List(pc1)), PostCommit(List(pc2))))

      val m = t.map(_ * 2)

      m.run._2.fs.size mustBe 1
      m.run._2.fs.head mustBe pc1
      m.run._3.fs.size mustBe 1
      m.run._3.fs.head mustBe pc2
    }

    "must be concatenated by flatMap" in {
      val pc1 = () ⇒ ()
      val pc2 = () ⇒ ()
      val pc3 = () ⇒ ()
      val pc4 = () ⇒ ()

      val t1 = Transaction.const[Id, String, Int](1).postSuccess(pc1).postFailure(pc2)
      val t2 = Transaction.const[Id, String, Int](1).postSuccess(pc3).postFailure(pc4)

      val res = t1.flatMap(_ ⇒ t2)

      res.run._2.fs.size mustBe 2
      res.run._2.fs mustBe List(pc1, pc3)
      res.run._3.fs.size mustBe 2
      res.run._3.fs mustBe List(pc2, pc4)
    }
  }
}