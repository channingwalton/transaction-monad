# The Transaction Monad

[![Build Status](https://travis-ci.org/channingwalton/transaction-monad.svg?branch=master)](https://travis-ci.org/channingwalton/transaction-monad)


The Transaction Monad represents a transactional operation that may or may not be successful (much like Either),
with functions to run depending on the success or failure of the transaction.

## Motivation

In several projects we ended up with a monad transformer stack of the form:

    final case class PostCommit(description: String, item: () => Unit) 
    type IOWriter[A] = WriterT[IO, List[PostCommit], A]
    type Transaction[T] = EitherT[IOWriter, String, T]

Where _IO_ is an effect like scalaz or cats-effect _IO_, and _PostCommit_ is a function to run when the Transaction
runs successfully.

However, this effect stack is a little cumbersome so

## The Monad to Rule Them All

[Transaction](core/src/main/scala/com/casualmiracles/transaction/Transaction.scala) wraps up the stack described above in a new type to make
it simpler to understand and use.

    final case class Transaction[F[_] : Monad, E, A](run: F[Run[E, A]])

where [Run](core/src/main/scala/com/casualmiracles/transaction/Run.scala) wraps an Either value, and functions
that will be run after the transaction is run.

_Transaction.unsafeRun_ will execute the transaction given an implicit
[TransactionRunner](core/src/main/scala/com/casualmiracles/transaction/TransactionRunner.scala).

The core module is supplemented by a second module, _cats-effect_, that provides _TransactionIO_
to build and run a _Transaction[cats.effect.IO, E, A]_, and a [TransactionRunner[IO]](core/src/main/scala/com/casualmiracles/transaction/TransactionRunner.scala).

# Using the Transaction Monad

```scala
libraryDependencies ++= Seq(
  "com.casualmiracles" %% "transaction-core" % "0.0.8",
  "org.typelevel" %% "cats-core" % "1.0.0")
```

```scala
libraryDependencies ++= Seq(
  "com.casualmiracles" %% "transaction-cats-effect" % "0.0.8",
  "org.typelevel" %% "cats-effect" % "0.5")
```