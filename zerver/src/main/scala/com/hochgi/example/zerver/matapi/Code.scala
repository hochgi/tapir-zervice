package com.hochgi.example.zerver.matapi

import com.hochgi.example.endpoints.Code.FooEndpoint
import com.hochgi.example.logic.util.Dummy
import sttp.tapir.ztapir.ZServerEndpoint
import zio.Task

object Code {

  val foo: FooEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicPure[Task]{ case (i, j) => Right(Dummy.foo(i, j)) }
}
