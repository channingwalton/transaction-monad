# The Transaction Monad

[![Build Status](https://travis-ci.org/channingwalton/transaction-monad.svg?branch=master)](https://travis-ci.org/channingwalton/transaction-monad)


The Transaction Monad represents a transactional operation that may or may not be successful (much like Either),
with functions to run depending on the success of the transaction.

## Motivation

In several projects we ended up with a monad transformer stack of the form:

    final case class PostCommit(description: String, item: () => Unit) 
    type IOWriter[A] = WriterT[IO, List[PostCommit], A]
    type Transaction[T] = EitherT[IOWriter, String, T]

Where _IO_ is an effect like scalaz or cats-effect _IO_, and _PostCommit_ is a function to run when the Transaction
runs successfully.

The PostCommit functions tend to be things like sending an email or updating another system after a workflow transition has occurred. We don't want the
email sent if the workflow transition failed to be successfully committed. (e.g. "Thanks for the $1M you've deposited today." when the deposit failed.)

However, this effect stack is a little cumbersome so this project wraps that up for you.

## Transaction Builder

The [TransactionBuilder](core/src/main/scala/com/casualmiracles/transaction/TransactionBuilder.scala) helps you build Transaction instances for
a given effect and also to run the transaction.

[TransactionTest](core/src/test/scala/com/casualmiracles/transaction/TransactionTest.scala) gives some examlples of use.

The [TransactionRunner](core/src/main/scala/com/casualmiracles/transaction/TransactionRunner.scala) is used to run a transaction for a given effect.

## Cats Effect

The core module is supplemented by a second module, _cats-effect_, that provides _TransactionIO_
to build and run a _Transaction[cats.effect.IO, E, A]_, and a [TransactionRunner[IO]](core/src/main/scala/com/casualmiracles/transaction/TransactionRunner.scala).

# Using the Transaction Monad

```scala
libraryDependencies ++= Seq(
  "com.casualmiracles" %% "transaction-core" % "0.0.12",
  "org.typelevel" %% "cats-core" % "1.0.0")
```

```scala
libraryDependencies ++= Seq(
  "com.casualmiracles" %% "transaction-cats-effect" % "0.0.12",
  "org.typelevel" %% "cats-effect" % "0.5")
```
