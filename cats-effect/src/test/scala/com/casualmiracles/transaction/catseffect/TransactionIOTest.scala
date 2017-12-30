package com.casualmiracles.transaction.catseffect

import org.scalatest.{EitherValues, FreeSpec, MustMatchers}
import cats.effect.IO
import com.casualmiracles.transaction.{RunResult, Transaction}

class TransactionIOTest extends FreeSpec with MustMatchers with EitherValues {

  "Success" in {
    assertTransaction(
      TransactionIO.success[String, String]("hi"),
      RunResult.Success[String, String]("hi"), true, false)
  }

  "Failure" - {
    "when the result is a failure" in {
      assertTransaction(
        TransactionIO.failure[String, String]("oops"),
        RunResult.Failure[String, String]("oops"), false, true)
    }

    "when a non-fatal exception is thrown" in {
      val exception = new RuntimeException()
      assertTransaction(
        TransactionIO.success[String, String]("ok").map(_ ⇒ throw exception),
        RunResult.Error(exception), false, false)
    }
  }

  private def assertTransaction[E, A](trans: Transaction[IO, E, A], result: RunResult[E, A], onSuccessRun: Boolean, onFailureRun: Boolean) = {
    var onSuccess = false
    var onFailure = false

    val transWithFunctions = trans.onSuccess(() ⇒ onSuccess = true).onFailure(() ⇒ onFailure = true)

    transWithFunctions.unsafeAttemptRun mustBe result

    onSuccess mustBe onSuccessRun
    onFailure mustBe onFailureRun
  }
}
