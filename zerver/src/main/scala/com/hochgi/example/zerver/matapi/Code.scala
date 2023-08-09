package com.hochgi.example.zerver.matapi

import com.hochgi.example.endpoints.Code.{FooEndpoint, WordCountSlidingWindowEndpoint}
import com.hochgi.example.logic.util.{Dummy, JsonProcess}
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{Task, URLayer, ZIO, ZLayer}

trait Code {

  val foo: FooEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicPure[Task]{ case (i, j) => Right(Dummy.foo(i, j)) }

  def jsonWordCountSlidingWindow: WordCountSlidingWindowEndpoint => ZServerEndpoint[Any, Any]
}
case class CodeImpl(jsonProcess: JsonProcess) extends Code {
  override val jsonWordCountSlidingWindow: WordCountSlidingWindowEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicSuccess[Task] { _ =>
      for {
        wc <- jsonProcess.ref.get
      } yield wc.grouped
    }
}
object CodeImpl {
  val live: URLayer[JsonProcess, CodeImpl] =
    ZLayer(ZIO.serviceWith[JsonProcess](CodeImpl.apply))
}
