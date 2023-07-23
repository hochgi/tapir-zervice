package com.hochgi.example.server.routes

import com.hochgi.example.endpoints.Code.FooEndpoint
import com.hochgi.example.logic.util.Dummy

import scala.concurrent.Future

object CodeRoutes {

  val foo: FooEndpoint => EvaluatedEndpoint[(Int, Int), Int] =
    fooEndpoint => fooEndpoint.serverLogicPure[Future]{ case (i, j) => Right(Dummy.foo(i, j)) }
}
