package com.casualmiracles.transaction

sealed trait Result[E, A] extends Product with Serializable

object Result {

  final case class Success[E, A](logs: List[String], a: A) extends Result[E, A]

  final case class Failure[E, A](logs: List[String], e: E) extends Result[E, A]

  final case class Error[E, A](t: Throwable) extends Result[E, A]

}