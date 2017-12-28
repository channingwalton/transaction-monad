package com.casualmiracles.transaction.catseffect

import org.scalatest.{EitherValues, FreeSpec, MustMatchers}
import TransactionIOOps._
import cats.effect.IO
import com.casualmiracles.transaction.Transaction

class TransactionIOTest extends FreeSpec with MustMatchers with EitherValues {

  "Success" in {
    var onSuccess = false
    var onFailure = false
    val trans = TransactionIO.success[String, String]("hi").onSuccess(() ⇒ onSuccess = true).onFailure(() ⇒ onFailure = true)

    onSuccess mustBe false
    onFailure mustBe false

    trans.unsafeAttemptRun() mustBe RunResult.Success("hi")

    onSuccess mustBe true
    onFailure mustBe false
  }

  "Failure" - {
    "when the result is a failure" in {
      var onSuccess = false
      var onFailure = false

      val trans = TransactionIO.failure[String, String]("oops").onSuccess(() ⇒ onSuccess = true).onFailure(() ⇒ onFailure = true)

      trans.unsafeAttemptRun() mustBe RunResult.Failure("oops")

      onSuccess mustBe false
      onFailure mustBe true
    }

    "when a non-fatal exception is thrown" in {
      val exception = new RuntimeException()

      var onSuccess = false
      var onFailure = false

      val trans: Transaction[IO, String, String] = TransactionIO.success[String, String]("ok").onSuccess(() ⇒ onSuccess = true).onFailure(() ⇒ onFailure = true).map(_ ⇒ throw exception)

      trans.unsafeAttemptRun() mustBe RunResult.Error(exception)

      onSuccess mustBe false
      onFailure mustBe false
    }
  }
}
