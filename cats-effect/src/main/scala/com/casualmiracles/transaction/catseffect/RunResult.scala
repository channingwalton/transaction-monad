package com.casualmiracles.transaction.catseffect

import com.casualmiracles.transaction.catseffect.RunResult.{Error, Failure, Success}

sealed trait RunResult[E, A] extends Product with Serializable {
  def fold[T](success: A ⇒ T, failure: E ⇒ T, error: Throwable ⇒ T): T =
    this match {
      case Success(a) => success(a)
      case Failure(e) => failure(e)
      case Error(t) => error(t)
    }
}

object RunResult {

  final case class Success[E, A](a: A) extends RunResult[E, A]

  final case class Failure[E, A](e: E) extends RunResult[E, A]

  final case class Error[E, A](t: Throwable) extends RunResult[E, A]

}
