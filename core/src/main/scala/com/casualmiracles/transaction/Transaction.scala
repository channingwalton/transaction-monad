package com.casualmiracles.transaction

import cats.Monad

final case class Transaction[F[_] : Monad, A, B](runF: F[Run[A, B]]) {

  private val monadF = implicitly[Monad[F]]

  def fold[C](fa: A => C, fb: B => C): F[C] =
    monadF.map(runF)(_.fold(fa, fb))

  def isLeft: F[Boolean] =
    monadF.map(runF)(_.isLeft)

  def isRight: F[Boolean] =
    monadF.map(runF)(_.isRight)

  def swap: Transaction[F, B, A] =
    Transaction(monadF.map(runF)(_.swap))

  def getOrElse[BB >: B](default: => BB): F[BB] =
    monadF.map(runF)(_.getOrElse(default))

  def getOrElseF[BB >: B](default: => F[BB]): F[BB] = {
    monadF.flatMap(runF) { run ⇒
      run.res match {
        case Left(_) => default
        case Right(b) => monadF.pure(b)
      }
    }
  }

  /**
    * Append a function to run on success
    */
  def onSuccess(f: () ⇒ Unit): Transaction[F, A, B] =
    Transaction(monadF.map(runF)(_.appendSuccess(OnSuccess(f))))

  /**
    * Append a function to run on failure
    */
  def onFailure(f: () ⇒ Unit): Transaction[F, A, B] =
    Transaction(monadF.map(runF)(_.appendFailure(OnFailure(f))))

  def map[C](f: B ⇒ C): Transaction[F, A, C] =
    Transaction(monadF.map(runF)(_.map(f)))

  def flatMap[C](f: B ⇒ Transaction[F, A, C]): Transaction[F, A, C] =
    Transaction {
      monadF.flatMap(runF) {
        thisRun: Run[A, B] ⇒ {
          thisRun.res.fold(
            // this is a left so we don't flatMap it but it does need to be
            // cast to the appropriate type which is safe because its a Left, the Right
            // doesn't actually exist
            _ ⇒ this.asInstanceOf[Transaction[F, A, C]].runF,

            // this is a right so we can flatMap it
            (thisRightValue: B) ⇒ {
              val newTransaction: Transaction[F, A, C] = f(thisRightValue)

              // carry the existing post commit actions over to the result
              monadF.map(newTransaction.runF) { newRun: Run[A, C] ⇒
                Run(newRun.res, thisRun.onFailure ++ newRun.onFailure, thisRun.onSuccess ++ newRun.onSuccess)
              }
            }
          )
        }
      }
    }

  /**
    * The Kestrel combinator - apply a monadic function and discard the result while keeping the effect.
    */
  def flatTap[C](f: B ⇒ Transaction[F, A, C]): Transaction[F, A, B] =
    flatMap(a ⇒ f(a).map(_ ⇒ a))

  /**
    * Apply a function to the value and discard the result.
    */
  def tap[C](f: B ⇒ C): Transaction[F, A, B] =
    map(a ⇒ { f(a);  a })

  /**
    * Map the value to Unit.
    */
  def void: Transaction[F, A, Unit] =
    map(_ ⇒ ())

  /**
    * If T is an Option[C] then map None to an error and Some to the value.
    */
  def mapOption[C](err: A)(implicit ev: B =:= Option[C]): Transaction[F, A, C] =
    flatMap(_.fold(Transaction.failure[F, A, C](err))(Transaction.success(_)))

  /**
    * Run this transaction, executing side-effects on success or failure.
    * Note that if the attempt to run fails with an Exception,
    * no side-effects will be run.
    */
  def unsafeAttemptRun(implicit runner: TransactionRunner[F]): RunResult[A, B] = {
    runner.unsafeRun(this) match {
      case Left(t) ⇒ RunResult.Error(t)

      case Right(Run(Left(e), f, _)) ⇒
        f.unsafeRun()
        RunResult.Failure(e)

      case Right(Run(Right(a), _, s)) ⇒
        s.unsafeRun()
        RunResult.Success(a)
    }
  }
}

object Transaction {

  def fromOption[F[_]: Monad, E, A](value: Option[A], e: ⇒ E): Transaction[F, E, A] =
    fromEither[F, E, A](value.fold[Either[E, A]](Left(e))(Right(_)))

  def fromEither[F[_], E, A](value: Either[E, A])(implicit monad: Monad[F]): Transaction[F, E, A] =
    Transaction(monad.pure(Run(value)))

  def success[F[_]: Monad, E, A](value: A): Transaction[F, E, A] =
    fromEither(Right(value))

  def failure[F[_]: Monad, E, A](err: E): Transaction[F, E, A] =
    fromEither(Left[E, A](err))

  def lift[F[_], E, A](value: F[A])(implicit monadF: Monad[F]): Transaction[F, E, A] =
    Transaction(monadF.map(value)(v ⇒ Run(Right[E, A](v))))

  def onSuccess[F[_]: Monad, E](f: () ⇒ Unit): Transaction[F, E, Unit] =
    success[F, E, Unit](()).onSuccess(f)

  def onFailure[F[_]: Monad, E](f: () ⇒ Unit): Transaction[F, E, Unit] =
    success[F, E, Unit](()).onFailure(f)

  implicit def transactionMonad[F[_], E](implicit monadF: Monad[F]): Monad[Transaction[F, E, ?]] =
    new Monad[Transaction[F, E, ?]] {
      override def pure[A](x: A): Transaction[F, E, A] =
        Transaction.success[F, E, A](x)

      override def tailRecM[A, B](a: A)(f: A ⇒ Transaction[F, E, Either[A, B]]): Transaction[F, E, B] =
        Transaction(monadF.tailRecM(a)(a0 => monadF.map(f(a0).runF) {
          case Run(Left(l), c, d)         => Right(Run(Left(l), c, d))
          case Run(Right(Left(a1)), _, _) => Left(a1)
          case Run(Right(Right(b)), c, d) => Right(Run(Right(b), c, d))
        }))

      override def flatMap[A, B](fa: Transaction[F, E, A])(f: A ⇒ Transaction[F, E, B]): Transaction[F, E, B] =
        fa.flatMap(f)
    }
}