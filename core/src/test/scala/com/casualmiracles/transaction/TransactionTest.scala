package com.casualmiracles.transaction

import cats.Id
import cats.instances.list._
import com.casualmiracles.transaction.Result._
import org.scalatest.{EitherValues, FreeSpec, MustMatchers}

class TransactionTest extends FreeSpec with MustMatchers with EitherValues {

  val builder: TransactionBuilder[Id, String] = new TransactionBuilder[Id, String]

  import builder._

  "post commits" - {
    "must be carried by map" in {
      val pc1 = () ⇒ ()

      val t = success(1).postRun(pc1)

      val m = t.map(_ * 2)

      m.value.run(Nil, Nil)._2.size mustBe 1
    }

    "must be concatenated by flatMap" in {
      val pc1 = () ⇒ ()
      val pc2 = () ⇒ ()

      val t1 = success(1).postRun(pc1)
      val t2 = success(1).postRun(pc2)

      val res = t1.flatMap(_ ⇒ t2)

      res.value.run(Nil, Nil)._2.size mustBe 2
  }

  "construct from" - {
    "a success value" in {
      success(1).unsafeRun mustBe Success(Nil, 1)
    }

    "a failure value" in {
      failure("oops").unsafeRun mustBe Failure(Nil, "oops")
    }

    "an either" - {
      "right" in {
        liftEither(Right(1)).unsafeRun mustBe Success(Nil, 1)
      }
      "left" in {
        liftEither(Left("oops")).unsafeRun mustBe Failure(Nil, "oops")
      }
    }
  }

    "an option" - {
      "some" in {
        liftOption(Some(1), "oops").unsafeRun mustBe Success(Nil, 1)
      }
      "none" in {
        liftOption(None, "oops").unsafeRun mustBe Failure(Nil, "oops")
      }
    }
  }

  "logs" - {
    "from syntax" in {
      val t = success(1).log("a log")
      t.unsafeRun mustBe Success(List("a log"), 1)
    }

    "carried by map" in {
      success(1).log("a log").map(_ + 1).unsafeRun mustBe Success(List("a log"), 2)
    }

    "concatenated by flatMap" in {
      success(1).log("log 1").flatMap(a ⇒ success(a * 2).log("log 2")).unsafeRun mustBe Success(List("log 1", "log 2"), 2)
    }
  }
}
