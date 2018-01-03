package com.casualmiracles.transaction

final case class Run[A, B](res: Either[A, B], onFailure: OnFailure = OnFailure(), onSuccess: OnSuccess = OnSuccess()) {

  def exists(f: B ⇒ Boolean): Boolean =
    res.exists(f)

  def forall(f: B ⇒ Boolean): Boolean =
    res.forall(f)

  def getOrElse[BB >: B](default: => BB): BB =
    res.getOrElse(default)

  def fold[C](fe: A ⇒ C, fb: B ⇒ C): C =
    res.fold(fe, fb)

  def isRight: Boolean =
    res.isRight

  def isLeft: Boolean =
    res.isLeft

  def map[C](f: B ⇒ C): Run[A, C] =
    copy(res = res.map(f))

  def appendFailure(pc: OnFailure): Run[A, B] =
    copy(onFailure = onFailure ++ pc)

  def appendSuccess(pc: OnSuccess): Run[A, B] =
    copy(onSuccess = onSuccess ++ pc)

  def swap: Run[B, A] =
    copy(res.swap)
}
