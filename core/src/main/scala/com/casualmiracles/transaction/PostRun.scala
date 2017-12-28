package com.casualmiracles.transaction

/**
  * A list of nasty side-effects to run after a transaction has completed.
  */
sealed trait PostRun {
  def fs: List[() ⇒ Unit]

  def run(): Unit =
    fs.foreach(_())
}

case class OnSuccess(fs: List[() ⇒ Unit] = Nil) extends PostRun {

  /**
    * Append PostRun's functions to this
    */
  def ++(pc: OnSuccess): OnSuccess =
    OnSuccess(fs ++ pc.fs)
}

case class OnFailure(fs: List[() ⇒ Unit] = Nil) extends PostRun {

  /**
    * Append PostRun's functions to this
    */
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