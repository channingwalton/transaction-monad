package com.casualmiracles.transaction

import cats.Monad

final case class Transaction[F[_] : Monad, E, A](runF: F[Run[E, A]]) {

  private val monadF = implicitly[Monad[F]]

  /**
    * Append a function to run on success
    */
  def onSuccess(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(runF)(_.appendSuccess(OnSuccess(f))))

  /**
    * Append a function to run on failure
    */
  def onFailure(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(runF)(_.appendFailure(OnFailure(f))))

  def map[B](f: A ⇒ B): Transaction[F, E, B] =
    Transaction(monadF.map(runF)(_.map(f)))

  def flatMap[B](f: A ⇒ Transaction[F, E, B]): Transaction[F, E, B] =
    Transaction {
      monadF.flatMap(runF) {
        thisRun: Run[E, A] ⇒ {
          thisRun.res.fold(
            // this is a left so we don't flatMap it but it does need to be
            // cast to the appropriate type which is safe because its a Left, the Right
            // doesn't actually exist
            _ ⇒ this.asInstanceOf[Transaction[F, E, B]].runF,

            // this is a right so we can flatMap it
            (thisRightValue: A) ⇒ {
              val newTransaction: Transaction[F, E, B] = f(thisRightValue)

              // carry the existing post commit actions over to the result
              monadF.map(newTransaction.runF) { newRun: Run[E, B] ⇒
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
  def flatTap[B](f: A ⇒ Transaction[F, E, B]): Transaction[F, E, A] =
    flatMap(a ⇒ f(a).map(_ ⇒ a))

  /**
    * Apply a function to the value and discard the result.
    */
  def tap[B](f: A ⇒ B): Transaction[F, E, A] =
    map(a ⇒ { f(a);  a })

  /**
    * Map the value to Unit.
    */
  def void: Transaction[F, E, Unit] =
    map(_ ⇒ ())

  /**
    * If T is an Option[B] then map None to an error and Some to the value.
    */
  def mapOption[B](err: E)(implicit ev: A =:= Option[B]): Transaction[F, E, B] =
    flatMap(_.fold(Transaction.failure[F, E, B](err))(Transaction.pure(_)))

  /**
    * Run this transaction, executing side-effects on success or failure.
    * Note that if the attempt to run fails with an Exception,
    * no side-effects will be run.
    */
  def unsafeAttemptRun(implicit runner: TransactionRunner[F]): RunResult[E, A] = {
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

  def fromEither[F[_], E, A](value: Either[E, A])(implicit monad: Monad[F]): Transaction[F, E, A] =
    Transaction(monad.pure(Run(value)))

  def pure[F[_]: Monad, E, A](value: A): Transaction[F, E, A] =
    fromEither(Right(value))

  def failure[F[_]: Monad, E, A](err: E): Transaction[F, E, A] =
    fromEither(Left[E, A](err))

  def lift[F[_], E, A](value: F[A])(implicit monadF: Monad[F]): Transaction[F, E, A] =
    Transaction(monadF.map(value)(v ⇒ Run(Right[E, A](v))))

  def onSuccess[F[_]: Monad, E](f: () ⇒ Unit): Transaction[F, E, Unit] =
    pure[F, E, Unit](()).onSuccess(f)

  def onFailure[F[_]: Monad, E](f: () ⇒ Unit): Transaction[F, E, Unit] =
    pure[F, E, Unit](()).onFailure(f)

  implicit def transactionMonad[F[_], E](implicit monadF: Monad[F]): Monad[Transaction[F, E, ?]] =
    new Monad[Transaction[F, E, ?]] {
      override def pure[A](x: A): Transaction[F, E, A] =
        Transaction.pure[F, E, A](x)

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