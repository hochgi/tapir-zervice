package com.hochgi.example.zerver

import com.typesafe.config.{Config => TSConfig, ConfigFactory}
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio._
import zio.http._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J
import zio.config.typesafe.TypesafeConfigProvider

object Main extends ZIOAppDefault {

  val tsconfigZIO:               UIO[TSConfig]                               = ZIO.succeed(ConfigFactory.load())
  val configProviderZIO:         ZIO[Any, Throwable, ConfigProvider]         = tsconfigZIO.map(TypesafeConfigProvider.fromTypesafeConfig(_))
  val tsconfigLayer:             ZLayer[Any, Nothing, TSConfig]              = ZLayer(tsconfigZIO)
  val configProviderLayer:       ZLayer[TSConfig, Throwable, ConfigProvider] = ZLayer[TSConfig, Throwable, ConfigProvider](configProviderZIO)
  val serverConfigProviderLayer: ZLayer[TSConfig, Throwable, Unit]           = configProviderLayer.flatMap(env => zio.Runtime.setConfigProvider(env.get))

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    SLF4J.slf4j(LogFormat.default)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val serverOptions: ZioHttpServerOptions[Any] =
      ZioHttpServerOptions.customiseInterceptors
        .metricsInterceptor(Routes.prometheusMetrics.metricsInterceptor())
        .options

    val app = ZioHttpInterpreter(serverOptions).toHttp(Routes.all)

    val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8080)

    (
      for {
        actualPort <- Server.install(app.withDefaultErrorResponse)
        _ <- Console.printLine(s"Go to http://localhost:${actualPort}/docs to open SwaggerUI. Press ENTER key to exit.")
        _ <- Console.readLine
      } yield ()
    ).provide(
      Server.defaultWithPort(port),
      tsconfigLayer,
      configProviderLayer,
      serverConfigProviderLayer
    ).exitCode
  }
}
