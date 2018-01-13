package com.casualmiracles.transaction

final case class PostRun(f: () ⇒ Unit) extends Product with Serializable
