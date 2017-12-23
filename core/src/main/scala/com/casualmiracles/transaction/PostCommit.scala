package com.casualmiracles.transaction

/**
  * A list of nasty side-effects to run after a transaction has completed.
  * @param fs the nasty functions
  */
case class PostCommit(fs: List[() ⇒ Unit] = Nil) {
  def run(): Unit =
    fs.foreach(_())

  def ::(f: () ⇒ Unit): PostCommit =
    PostCommit(f :: fs)

  def ++(pc: PostCommit): PostCommit =
    PostCommit(fs ++ pc.fs)
}
