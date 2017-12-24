package com.casualmiracles.transaction.catseffect

sealed trait RunResult[E, A] extends Product with Serializable

object RunResult {

  final case class Success[E, A](a: A) extends RunResult[E, A]

  final case class Failure[E, A](e: E) extends RunResult[E, A]

  final case class Error[E, A](t: Throwable) extends RunResult[E, A]

}
