package com.casualmiracles.transaction

import cats.Id
import com.casualmiracles.transaction.RunResult.{Failure, Success}
import org.scalatest.{EitherValues, FreeSpec, MustMatchers}

class TransactionTest extends FreeSpec with MustMatchers with EitherValues {

  type TestTransaction[A] = Transaction[Id, String, A]

  def testTransaction[A](a: A): TestTransaction[A] =
    Transaction[Id, String, A](Run(Right[String, A](a), OnFailure(), OnSuccess()))

  "post commits" - {
    "must be carried by map" in {
      val pc1 = () ⇒ ()
      val pc2 = () ⇒ ()

      val t = Transaction[Id, String, Int](Run(Right[String, Int](1), OnFailure(List(pc1)), OnSuccess(List(pc2))))

      val m = t.map(_ * 2)

      m.runF.onFailure.fs.size mustBe 1
      m.runF.onFailure.fs.head mustBe pc1
      m.runF.onSuccess.fs.size mustBe 1
      m.runF.onSuccess.fs.head mustBe pc2
    }

    "must be concatenated by flatMap" in {
      val pc1 = () ⇒ ()
      val pc2 = () ⇒ ()
      val pc3 = () ⇒ ()
      val pc4 = () ⇒ ()

      val t1 = Transaction.success[Id, String, Int](1).onSuccess(pc1).onFailure(pc2)
      val t2 = Transaction.success[Id, String, Int](1).onSuccess(pc3).onFailure(pc4)

      val res = t1.flatMap(_ ⇒ t2)

      res.runF.onFailure.fs.size mustBe 2
      res.runF.onFailure.fs mustBe List(pc2, pc4)
      res.runF.onSuccess.fs.size mustBe 2
      res.runF.onSuccess.fs mustBe List(pc1, pc3)
    }
  }

  "construct from" - {
    "a success value" in {
      Transaction.success(1).unsafeAttemptRun mustBe Success(1)
    }

    "a failure value" in {
      Transaction.failure("oops").unsafeAttemptRun mustBe Failure("oops")
    }

    "an either" - {
      "right" in {
        Transaction.fromEither(Right(1)).unsafeAttemptRun mustBe Success(1)
      }
      "left" in {
        Transaction.fromEither(Left("oops")).unsafeAttemptRun mustBe Failure("oops")
      }
    }

    "an option" - {
      "some" in {
        Transaction.fromOption(Some(1), "oops").unsafeAttemptRun mustBe Success(1)
      }
      "none" in {
        Transaction.fromOption(None, "oops").unsafeAttemptRun mustBe Failure("oops")
      }
    }
  }
}
