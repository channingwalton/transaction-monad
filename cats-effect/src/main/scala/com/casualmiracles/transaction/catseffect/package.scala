package com.casualmiracles.transaction

import cats.effect.IO

package object catseffect {
  implicit val runner: TransactionRunner[IO] = TransactionIORunner
}
