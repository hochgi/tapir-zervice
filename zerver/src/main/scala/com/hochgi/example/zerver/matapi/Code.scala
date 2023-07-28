package com.hochgi.example.zerver.matapi

import com.hochgi.example.endpoints.Code.{FooEndpoint, WordCountSlidingWindowEndpoint}
import com.hochgi.example.logic.util.{Dummy, WordCountState}
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{Task, URLayer, ZIO, ZLayer}
import zio.stm.TRef

trait Code {

  val foo: FooEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicPure[Task]{ case (i, j) => Right(Dummy.foo(i, j)) }

  def jsonWordCountSlidingWindow: WordCountSlidingWindowEndpoint => ZServerEndpoint[Any, Any]
}
case class CodeImpl(tRef: TRef[WordCountState]) extends Code {
  override val jsonWordCountSlidingWindow: WordCountSlidingWindowEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicSuccess[Task] { _ =>
      for {
        wc <- tRef.get.commit
      } yield wc.grouped
    }
}
object CodeImpl {
  val live: URLayer[TRef[WordCountState], CodeImpl] =
    ZLayer(ZIO.service[TRef[WordCountState]].map(CodeImpl.apply))
}
