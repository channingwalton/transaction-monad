package com.casualmiracles.transaction

sealed trait Result[E, A] extends Product with Serializable {
  def fold[T](success: A ⇒ T, failure: E ⇒ T, error: Throwable ⇒ T): T =
    this match {
      case Result.Success(a) => success(a)
      case Result.Failure(e) => failure(e)
      case Result.Error(t) => error(t)
    }
}

object Result {

  final case class Success[E, A](a: A) extends Result[E, A]

  final case class Failure[E, A](e: E) extends Result[E, A]

  final case class Error[E, A](t: Throwable) extends Result[E, A]

}