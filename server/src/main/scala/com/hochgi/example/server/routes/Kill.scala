package com.hochgi.example.server.routes

import akka.actor.ActorSystem
import akka.actor.typed.Scheduler
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractHost, get, path}
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object Kill extends LazyLogging {

  val notAllowed: HttpResponse = {
    val body = "{\"success\":false,\"reason\":\"kill API can be called only from 'localhost'\"}\n"
    HttpResponse(
      status = StatusCodes.Forbidden,
      entity = HttpEntity(ContentTypes.`application/json`, body))
  }

  def route(terminate:                () => Unit)
           (implicit actorSystem:     ActorSystem,
            scheduler:                Scheduler,
            ec:                       ExecutionContext): Route = get {
    path("kill") {
      extractHost { hostName =>
        if (hostName != "localhost") complete(notAllowed) // poor man's safe guard - not a real solution ;)
        else complete {
          scheduler.scheduleOnce (
            5.milli,
            () => actorSystem
              .terminate()
              .transform {
                case Success(_) => Try(terminate())
                case Failure(e) => Try(terminate()).transform(_ => Failure(e), { t =>
                  t.addSuppressed(e)
                  Failure(t)
                })
              }.onComplete {
              // race condition to be printed. Server may already shut down by now...
              case Success(_) => logger.info("Server killed!")
              case Failure(e) => logger.error("Server assassination failed!", e)
            })
          logger.info("Kill command initiated!")
          HttpResponse(status = StatusCodes.OK, entity = "Bye bye!\n")
        }
      }
    }
  }
}
