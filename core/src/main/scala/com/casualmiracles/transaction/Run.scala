package com.casualmiracles.transaction

final case class Run[E, A](res: Either[E, A], onFailure: OnFailure = OnFailure(), onSuccess: OnSuccess = OnSuccess()) {

  def map[B](f: A ⇒ B): Run[E, B] =
    copy(res = res.map(f))

  def appendFailure(pc: OnFailure): Run[E, A] =
    copy(onFailure = onFailure ++ pc)

  def appendSuccess(pc: OnSuccess): Run[E, A] =
    copy(onSuccess = onSuccess ++ pc)
}
