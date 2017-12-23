package com.casualmiracles.transaction

/**
  * A list of nasty side-effects to run after a transaction has completed.
  * @param fs the nasty functions
  */
case class PostRun(fs: List[() ⇒ Unit] = Nil) {
  def run(): Unit =
    fs.foreach(_())

  /**
    * Append PostRun's functions to this
    */
  def ++(pc: PostRun): PostRun =
    PostRun(fs ++ pc.fs)
}

object PostRun {
  def apply(f: () ⇒ Unit): PostRun =
    new PostRun(List(f))
}