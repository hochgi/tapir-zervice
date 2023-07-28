package com.hochgi.example.zerver

import com.hochgi.example.zerver.matapi.{CodeImpl, InfoImpl}
import com.hochgi.example.logic.util.JsonProcess
import com.typesafe.config.{ConfigFactory, Config => TSConfig}
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import sttp.tapir.ztapir.ZServerEndpoint
import zio._
import zio.http._
import zio.logging.LogFormat
import zio.logging.backend.SLF4J
import zio.config.typesafe.TypesafeConfigProvider

object Main extends ZIOAppDefault {


  override val bootstrap: ZLayer[Any, Nothing, Unit] =
    SLF4J.slf4j(LogFormat.default) ++ (Services.tsconfigLive >>> Services.configProviderLive >>> Services.setConfigProviderLive)

  object Services {
    // A layer with all of HOCON config object
    val tsconfigLive: ULayer[TSConfig] = ZLayer(ZIO.succeed(ConfigFactory.load()))

    // given a relevant (as in rooted properly) ts config, supply a ConfigProvider
    val configProviderLive: URLayer[TSConfig, ConfigProvider] = ZLayer(ZIO.service[TSConfig].map(TypesafeConfigProvider.fromTypesafeConfig(_)))

    // set the ConfigProvider
    val setConfigProviderLive: ZLayer[ConfigProvider, Nothing, Unit] =
      ZLayer(ZIO.service[ConfigProvider]).flatMap(zenv => zio.Runtime.setConfigProvider(zenv.get))
  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val serverOptions: ZioHttpServerOptions[Any] =
      ZioHttpServerOptions.customiseInterceptors
        .metricsInterceptor(Routes.prometheusMetrics.metricsInterceptor())
        .options

    (
      for {
        app <- ZIO.serviceWith[List[ZServerEndpoint[Any, Any]]](ZioHttpInterpreter(serverOptions).toHttp(_))
        actualPort <- Server.install(app.withDefaultErrorResponse)
        _ <- Console.printLine(s"Go to http://localhost:${actualPort}/docs to open SwaggerUI. Press ENTER key to exit.")
        _ <- Console.readLine
      } yield ()
    ).provide(
      Services.tsconfigLive,
      JsonProcess.live,
      Server.configured(),
      InfoImpl.live,
      CodeImpl.live,
      Routes.live
    ).exitCode
  }
}
