package com.casualmiracles.transaction.catseffect

import org.scalatest.{EitherValues, FreeSpec, MustMatchers}
import TransactionIO._

class TransactionIOTest extends FreeSpec with MustMatchers with EitherValues {

  "Success" in {
    TransactionIO.const[String, String]("hi").unsafeAttemptRun().right.value mustBe "hi"
  }

  "Failure" - {
    "when the result is a failure" in {
      TransactionIO.failure[String, String]("oops").unsafeAttemptRun().left.value.right.value mustBe "oops"
    }
  }
}
