package com.casualmiracles.transaction.catseffect

import org.scalatest.{EitherValues, FreeSpec, MustMatchers}
import cats.effect.IO
import com.casualmiracles.transaction.{Result, TransactionBuilder, TransactionF}

class TransactionIOTest extends FreeSpec with MustMatchers with EitherValues {

  val builder: TransactionBuilder[IO, String] = TransactionIO.builder[String]
  import builder._

  "Success" in {
    assertTransaction(
      TransactionIO.success[String, String]("hi"),
      Result.Success[String, String]("hi"), true)
  }

  "Failure" - {
    "when the result is a failure" in {
      assertTransaction(
        TransactionIO.failure[String, String]("oops"),
        Result.Failure[String, String]("oops"), false)
    }

    "when a non-fatal exception is thrown" in {
      val exception = new RuntimeException()
      assertTransaction(
        TransactionIO.success[String, String]("ok").map(_ ⇒ throw exception),
        Result.Error(exception), false)
    }
  }

  private def assertTransaction[E, A](trans: TransactionF[IO, String, A], result: Result[E, A], onSuccessRun: Boolean) = {
    var onSuccess = false

    val transWithFunctions = trans.postRun("", () ⇒ onSuccess = true)

    transWithFunctions.unsafeRun mustBe result

    onSuccess mustBe onSuccessRun
  }
}
