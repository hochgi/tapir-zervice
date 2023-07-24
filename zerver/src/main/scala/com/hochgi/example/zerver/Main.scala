package com.hochgi.example.zerver

import com.hochgi.example.zerver.matapi.InfoImpl
import com.typesafe.config.{ConfigFactory, Config => TSConfig}
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.ztapir.ZServerEndpoint
import zio._
import zio.http._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J
import zio.config.typesafe.TypesafeConfigProvider

object Main extends ZIOAppDefault {


  object Services {
    val tsconfigZIO:  UIO[TSConfig]    = ZIO.succeed(ConfigFactory.load())
    val tsconfigLive: ULayer[TSConfig] = ZLayer(tsconfigZIO)
    val configProviderLive: ULayer[ConfigProvider] = {
      val tmp: URLayer[TSConfig, ConfigProvider] = ZLayer(ZIO.service[TSConfig].map(TypesafeConfigProvider.fromTypesafeConfig(_)))
      tsconfigLive >>> tmp
    }
    val serverConfigProviderLive: ZLayer[Any, Nothing, Unit] = for {
      cp <- configProviderLive
      sc <- zio.Runtime.setConfigProvider(cp.get)
    } yield sc
  }

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    SLF4J.slf4j(LogFormat.default)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val serverOptions: ZioHttpServerOptions[Any] =
      ZioHttpServerOptions.customiseInterceptors
        .metricsInterceptor(Routes.prometheusMetrics.metricsInterceptor())
        .options

    val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)

    (
      for {
        app <- ZIO.serviceWith[List[ZServerEndpoint[Any, Any]]](ZioHttpInterpreter(serverOptions).toHttp(_))
        actualPort <- Server.install(app.withDefaultErrorResponse)
        _ <- Console.printLine(s"Go to http://localhost:${actualPort}/docs to open SwaggerUI. Press ENTER key to exit.")
        _ <- Console.readLine
      } yield ()
    ).provide(
      Server.defaultWithPort(port),
      Services.tsconfigLive,
      Services.serverConfigProviderLive,
      InfoImpl.live,
      Routes.live
    ).exitCode
  }
}
