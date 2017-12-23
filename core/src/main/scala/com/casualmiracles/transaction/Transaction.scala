package com.casualmiracles.transaction

import cats.Monad

final case class Run[E, A](res: Either[E, A], onFailure: PostCommit = PostCommit(), onSuccess: PostCommit = PostCommit()) {

  def map[B](f: A ⇒ B): Run[E, B] =
    copy(res = res.map(f))

  def appendFailure(pc: PostCommit): Run[E, A] =
    copy(onFailure = onFailure ++ pc)

  def appendSuccess(pc: PostCommit): Run[E, A] =
    copy(onSuccess = onSuccess ++ pc)
}

final case class Transaction[F[_] : Monad, E, A](run: F[Run[E, A]]) {

  private val monadF = implicitly[Monad[F]]

  /**
    * Append a function to run on success
    */
  def postSuccess(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(run)(_.appendSuccess(PostCommit(f))))

  /**
    * Append a function to run on failure
    */
  def postFailure(f: () ⇒ Unit): Transaction[F, E, A] =
    Transaction(monadF.map(run)(_.appendFailure(PostCommit(f))))

  def map[B](f: A ⇒ B): Transaction[F, E, B] =
    Transaction(monadF.map(run)(_.map(f)))

  /**
    * Syntax for map.
    */
  def ∘[B](f: A ⇒ B): Transaction[F, E, B] =
    map(f)

  def flatMap[B](f: A ⇒ Transaction[F, E, B]): Transaction[F, E, B] =
    Transaction {
      monadF.flatMap(run) {
        thisRun: Run[E, A] ⇒ {
          thisRun.res.fold(
            // this is a left so we don't flatMap it but it does need to be
            // cast to the appropriate type which is safe because its a Left, the Right
            // doesn't actually exist
            _ ⇒ this.asInstanceOf[Transaction[F, E, B]].run,

            // this is a right so we can flatMap it
            (thisRightValue: A) ⇒ {
              val newTransaction: Transaction[F, E, B] = f(thisRightValue)

              // carry the existing post commit actions over to the result
              monadF.map(newTransaction.run) { newRun: Run[E, B] ⇒
                Run(newRun.res, thisRun.onFailure ++ newRun.onFailure, thisRun.onSuccess ++ newRun.onSuccess)
              }
            }
          )
        }
      }
    }

  /**
    * Syntax for flatMap
    */
  def >>=[B](f: A ⇒ Transaction[F, E, B]): Transaction[F, E, B] =
    flatMap(f)

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
    flatMap(_.fold(Transaction.failure[F, E, B](err))(Transaction.const(_)))
}

object Transaction {

  def fromEither[F[_], E, A](value: Either[E, A])(implicit monad: Monad[F]): Transaction[F, E, A] =
    Transaction(monad.pure(Run(value)))

  def const[F[_]: Monad, E, A](value: A): Transaction[F, E, A] =
    fromEither(Right(value))

  def failure[F[_]: Monad, E, A](err: E): Transaction[F, E, A] =
    fromEither(Left[E, A](err))

  implicit def transactionMonad[F[_], E](implicit monadF: Monad[F]): Monad[Transaction[F, E, ?]] =
    new Monad[Transaction[F, E, ?]] {
      override def pure[A](x: A): Transaction[F, E, A] =
        Transaction.const[F, E, A](x)

      override def tailRecM[A, B](a: A)(f: A ⇒ Transaction[F, E, Either[A, B]]): Transaction[F, E, B] =
        Transaction(monadF.tailRecM(a)(a0 => monadF.map(f(a0).run) {
          case Run(Left(l), c, d)         => Right(Run(Left(l), c, d))
          case Run(Right(Left(a1)), _, _) => Left(a1)
          case Run(Right(Right(b)), c, d) => Right(Run(Right(b), c, d))
        }))

      override def flatMap[A, B](fa: Transaction[F, E, A])(f: A ⇒ Transaction[F, E, B]): Transaction[F, E, B] =
        fa.flatMap(f)
    }
}