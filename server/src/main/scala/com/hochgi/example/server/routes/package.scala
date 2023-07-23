package com.hochgi.example.server

import com.hochgi.example.datatypes.ManagedError
import sttp.tapir.server.ServerEndpoint.Full

import scala.concurrent.Future

package object routes {
  type EvaluatedEndpoint[I, O] = Full[Unit, Unit, I, ManagedError, O, Any, Future]
//  type ZEvaluatedEndpoint[I, O] = Full[Unit, Unit, I, ManagedError, O, Any, ZIO[]]
}
