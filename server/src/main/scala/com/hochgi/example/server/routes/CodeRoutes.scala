package com.hochgi.example.server.routes

import com.hochgi.example.datatypes.GeneralError
import com.hochgi.example.endpoints.Code.{FooEndpoint, WordCountSlidingWindowEndpoint}
import com.hochgi.example.logic.util.Dummy

import scala.concurrent.Future

object CodeRoutes {

  val foo: FooEndpoint => EvaluatedEndpoint[(Int, Int), Int] =
    fooEndpoint => fooEndpoint.serverLogicPure[Future]{ case (i, j) => Right(Dummy.foo(i, j)) }

  val jsonWordCountSlidingWindow: WordCountSlidingWindowEndpoint =>  EvaluatedEndpoint[Unit, Map[String, Map[String, Int]]] =
    ep => ep.serverLogicPure[Future](_ => Left(GeneralError("???", 501)))
}
