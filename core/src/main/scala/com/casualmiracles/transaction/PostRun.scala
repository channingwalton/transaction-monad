package com.casualmiracles.transaction

final case class PostRun(description: String, f: () ⇒ Unit) extends Product with Serializable
