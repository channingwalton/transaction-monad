package com.casualmiracles.transaction

/**
  * A list of nasty side-effects to run after a transaction has completed.
  */
sealed trait PostRun[T <: PostRun[T]] {
  def fs: List[() ⇒ Unit]

  def run(): Unit =
    fs.foreach(_())

  def ++(pc: T): T
}

case class OnSuccess(fs: List[() ⇒ Unit] = Nil) extends PostRun[OnSuccess] {

  def ++(pc: OnSuccess): OnSuccess =
    OnSuccess(fs ++ pc.fs)
}

case class OnFailure(fs: List[() ⇒ Unit] = Nil) extends PostRun[OnFailure] {

  def ++(pc: OnFailure): OnFailure =
    OnFailure(fs ++ pc.fs)
}

object OnSuccess {
  def apply(f: () ⇒ Unit): OnSuccess =
    OnSuccess(List(f))
}

object OnFailure {
  def apply(f: () ⇒ Unit): OnFailure =
    OnFailure(List(f))
}