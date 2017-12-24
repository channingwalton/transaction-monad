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

where _Run_ is

    final case class Run[E, A](res: Either[E, A], onFailure: PostRun = PostRun(), onSuccess: PostRun = PostRun()) {

The core module is supplemented by a second module, _cats-effect_, that provides _TransactionIO_
to build and run a _Transaction[cats.effect.IO, E, A]_.

_TransactionIO.unsafeAttemptRun()_ will run a transaction, run appropriate _PostRun_ operations, and return
a _RunResult_.


# Using the Transaction Monad

er, not published yet. If sensible people think this is a good idea then I will publish it.