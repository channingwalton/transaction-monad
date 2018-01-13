package com.casualmiracles.transaction

import cats.Id
import cats.instances.list._
import com.casualmiracles.transaction.Result._
import org.scalatest.{EitherValues, FreeSpec, MustMatchers}

class TransactionTest extends FreeSpec with MustMatchers with EitherValues {

  val builder: TransactionBuilder[Id, String] = new TransactionBuilder[Id, String]

  import builder.TransactionSyntax

  "post commits" - {
    "must be carried by map" in {
      val pc1 = () ⇒ ()

      val t = builder.point(1).postRun("ok", pc1)

      val m = t.map(_ * 2)

      m.value.run(Nil, Nil)._2.size mustBe 1
    }

    "must be concatenated by flatMap" in {
      val pc1 = () ⇒ ()
      val pc2 = () ⇒ ()

      val t1 = builder.point(1).postRun("first", pc1)
      val t2 = builder.point(1).postRun("second", pc2)

      val res = t1.flatMap(_ ⇒ t2)

      res.value.run(Nil, Nil)._2.size mustBe 2
  }

  "construct from" - {
    "a success value" in {
      builder.point(1).unsafeRun mustBe Success(1)
    }

    "a failure value" in {
      builder.failure("oops").unsafeRun mustBe Failure("oops")
    }

    "an either" - {
      "right" in {
        builder.liftEither(Right(1)).unsafeRun mustBe Success(1)
      }
      "left" in {
        builder.liftEither(Left("oops")).unsafeRun mustBe Failure("oops")
      }
    }
  }

    "an option" - {
      "some" in {
        builder.liftOption(Some(1), "oops").unsafeRun mustBe Success(1)
      }
      "none" in {
        builder.liftOption(None, "oops").unsafeRun mustBe Failure("oops")
      }
    }
  }
}
