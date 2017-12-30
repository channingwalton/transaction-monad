# The Transaction Monad

The Transaction Monad represents a transactional operation that may or may not be successful (much like Either),
with post run operations to run depending on success or failure.

In several projects we ended up with a monad transformer stack of the form:

    final case class PostCommit(description: String, item: () => Unit) 
    type IOWriter[A] = WriterT[IO, List[PostCommit], A]
    type Transaction[T] = EitherT[IOWriter, String, T]

Where _IO_ is an effect like scalaz or cats-effect _IO_, and _PostCommit_ is a function to run when the Transaction
is run successfully.

However, this effect stack is a little cumbersome so this project wraps all this up in a single
[Transaction](core/src/main/scala/com/casualmiracles/transaction/Transaction.scala)

    final case class Transaction[F[_] : Monad, E, A](run: F[Run[E, A]])

where _Run_ wraps an Either value, and functions that will be run after the transaction is run.

_Transaction.unsafeRun_ will execute the transaction given an implicit _TransactionRunner_.

The core module is supplemented by a second module, _cats-effect_, that provides _TransactionIO_
to build and run a _Transaction[cats.effect.IO, E, A]_, and a _TransactionRunner[IO]_.

# Using the Transaction Monad

er, not published yet. If sensible people think this is a good idea then I will publish it.