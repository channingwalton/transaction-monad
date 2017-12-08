package com.casualmiracles.transaction

import cats.Id
import cats.laws.discipline.MonadTests
import cats.tests.CatsSuite

class TransactionTest extends CatsSuite {

  type TestTransaction[A] = Transaction[Id, String, A]

  checkAll("Transaction.MonadLaws", MonadTests[Option].monad[Int, Int, String])
}
