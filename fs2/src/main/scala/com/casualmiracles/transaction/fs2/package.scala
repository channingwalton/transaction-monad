package com.casualmiracles.transaction

import _root_.fs2.Task
import _root_.fs2.interop.cats._

package object fs2 {

  def fromEither[E, A](value: Either[E, A]): Transaction[Task, E, A] =
    Transaction(Task.delay((value, PostCommit(), PostCommit())))

  def const[E, A](value: A): Transaction[Task, E, A] =
    fromEither(Right[E, A](value))

  def failure[E, A](err: E): Transaction[Task, E, A] =
    fromEither(Left[E, A](err))
}
