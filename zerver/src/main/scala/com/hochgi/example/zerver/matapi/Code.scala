package com.hochgi.example.zerver.matapi

import com.hochgi.example.endpoints.Code.{FooEndpoint, WordCountSlidingWindowEndpoint}
import com.hochgi.example.logic.util.JsonProcess.RefWithUpdatingFiber
import com.hochgi.example.logic.util.Dummy
import sttp.tapir.ztapir.ZServerEndpoint
import zio.{Task, URLayer, ZIO, ZLayer}

trait Code {

  val foo: FooEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicPure[Task]{ case (i, j) => Right(Dummy.foo(i, j)) }

  def jsonWordCountSlidingWindow: WordCountSlidingWindowEndpoint => ZServerEndpoint[Any, Any]
}
case class CodeImpl(refWithUpdatingFiber: RefWithUpdatingFiber) extends Code {
  override val jsonWordCountSlidingWindow: WordCountSlidingWindowEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicSuccess[Task] { _ =>
      for {
        wc <- refWithUpdatingFiber.ref.get
      } yield wc.grouped
    }
}
object CodeImpl {
  val live: URLayer[RefWithUpdatingFiber, CodeImpl] =
    ZLayer(ZIO.serviceWith[RefWithUpdatingFiber](CodeImpl.apply))
}
