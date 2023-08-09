package com.hochgi.example.zerver.undoc

import sttp.tapir.ztapir.ZServerEndpoint
import sttp.tapir.{endpoint => ep}
import zio.{Fiber, Task}
import zio.process.CommandError
final case class Kill(fiberToInterrupt: Fiber[CommandError, Unit]) {

  val endpoint: ZServerEndpoint[Any, Any] =
    ep.in("kill")
      .get
      .serverLogicSuccess[Task](_ => fiberToInterrupt.interrupt.unexit.mapError(_.getCause))
}
