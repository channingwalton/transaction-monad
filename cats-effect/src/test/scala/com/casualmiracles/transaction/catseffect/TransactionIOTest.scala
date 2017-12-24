package com.casualmiracles.transaction.catseffect

import org.scalatest.{EitherValues, FreeSpec, MustMatchers}
import TransactionIOOps._

class TransactionIOTest extends FreeSpec with MustMatchers with EitherValues {

  "Success" in {
    TransactionIO.const[String, String]("hi").unsafeAttemptRun().right.value mustBe "hi"
  }

  "Failure" - {
    "when the result is a failure" in {
      TransactionIO.failure[String, String]("oops").unsafeAttemptRun().left.value.right.value mustBe "oops"
    }

    "when a non-fatal exception is thrown" in {
      val exception = new RuntimeException()
      TransactionIO.const[String, String]("ok").map(_ ⇒ throw exception).unsafeAttemptRun().left.value.left.value mustBe exception
    }
  }
}
