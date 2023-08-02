package com.hochgi.example.server

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{Behavior, Scheduler, Terminated}
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.codahale.metrics._
import com.codahale.metrics.jvm._
import com.hochgi.example.datatypes.GeneralError
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fr.davit.akka.http.metrics.core.HttpMetricsSettings
import fr.davit.akka.http.metrics.core.scaladsl.server.HttpMetricsDirectives._
import fr.davit.akka.http.metrics.dropwizard.marshalling.DropwizardMarshallers._
import fr.davit.akka.http.metrics.dropwizard.{DropwizardRegistry, DropwizardSettings}
import com.hochgi.example.server.routes.{CodeRoutes, EvaluatedEndpoint, InfoRoutes, Kill}
import com.hochgi.example.matapi.EvalEndpoints
import sttp.apispec.openapi.circe.yaml._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.swagger.{SwaggerUI, SwaggerUIOptions}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

object Main extends App with LazyLogging {

  // avoid slf4j noise by touching it first from single thread
  logger.debug("Starting server")

  def startDropwizard: (MetricRegistry, Route) = {
    val dropwizard: MetricRegistry = new MetricRegistry()
    dropwizard.register("jvm.gc", new GarbageCollectorMetricSet())
    dropwizard.register("jvm.memory", new MemoryUsageGaugeSet())
    val settings: HttpMetricsSettings = DropwizardSettings.default
    val registry = DropwizardRegistry(dropwizard, settings)
    val dropwizardRoute = (get & path("metrics"))(metrics(registry))
    (dropwizard, dropwizardRoute)
  }

  def apply(): Behavior[NotUsed] = Behaviors.setup { context =>

    implicit val actorSystem: ClassicActorSystem = context.system.classicSystem
    implicit val execContext: ExecutionContext   = actorSystem.dispatcher
    implicit val scheduler:   Scheduler          = context.system.scheduler

    val config: Config                = actorSystem.settings.config.resolve()
    val (dropwizard, dropwizardRoute) = startDropwizard

    val bindingFuture: Future[ServerBinding] = buildServer(
      config          = config,
      dropwizard      = dropwizard,
      dropwizardRoute = dropwizardRoute,
      context         = context,
      actorSystem     = actorSystem,
      execContext     = execContext,
      scheduler       = scheduler
    ).andThen { case Failure(err) =>
      logger.error("Failed to initialize server", err)
      context.system.terminate()
    }

    Behaviors.receiveSignal {
      case (_, Terminated(_)) =>
        bindingFuture.foreach(_.terminate(10.seconds))
        Behaviors.stopped
    }
  }

  def buildServer(config:          Config,
                  dropwizard:      MetricRegistry,
                  dropwizardRoute: Route,
                  context:         ActorContext[NotUsed],
                  actorSystem:     ClassicActorSystem,
                  execContext:     ExecutionContext,
                  scheduler:       Scheduler): Future[ServerBinding] = {

    implicit val providedActorSystem: ClassicActorSystem = actorSystem
    implicit val providedExecContext: ExecutionContext   = execContext
    implicit val providedScheduler:   Scheduler          = scheduler

    // initials to shorten [S]erver[E]ndpoint[A]ny[F]uture
    type SEAF = ServerEndpoint[Any, Future]

    val evaluatedAPI = EvalEndpoints.evalAll(
        evalBuild     = InfoRoutes.build,
        evalAllConfig = InfoRoutes.allConfig(config),
        evalConfig    = InfoRoutes.config(config),
        evalCodeFoo   = CodeRoutes.foo,
        evalCodeJWC   = CodeRoutes.jsonWordCountSlidingWindow)

    val (_, serverEndpoints)        = evaluatedAPI.endpoints.unzip
    val docsAsYaml:          String = evaluatedAPI.openApiDocs.toYaml
    val uiOptions: SwaggerUIOptions = SwaggerUIOptions.default.copy(pathPrefix = List("doc"))
    val swaggerUIEndPs:  List[SEAF] = SwaggerUI[Future](docsAsYaml, uiOptions)
    val swaggerUIRoute:       Route = AkkaHttpServerInterpreter().toRoute(serverEndpoints ::: swaggerUIEndPs)
    val kill:                 Route = Kill.route(() => context.system.terminate())
    val allRoutes:            Route = concat(swaggerUIRoute, kill, dropwizardRoute)
    val serverConfig:        Config = config.getConfig("hochgi.example.server")
    val port:                   Int = serverConfig.getInt("port")
    val host:                String = serverConfig.getString("host")
    Http().newServerAt(host, port).bind(allRoutes)
  }

  akka.actor.typed.ActorSystem(Main(), "example")
}
