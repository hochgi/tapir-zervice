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


  override val bootstrap: ZLayer[Any, Nothing, Unit with ConfigProvider] =
    SLF4J.slf4j(LogFormat.default) ++ Services.configProviderLive

  object Services {
    // A layer with all of HOCON config object
    val tsconfigLive: ULayer[TSConfig] = ZLayer(ZIO.succeed(ConfigFactory.load()))

    // Given a config object, extract the part that is relevant for zio-http server
    val serverTSConfigLive: ULayer[TSConfig] = {
      val tmp: URLayer[TSConfig, TSConfig] = ZLayer(ZIO.service[TSConfig].map(_.getConfig("hochgi.example.zerver")))
      tsconfigLive >>> tmp
    }

    // given a relevant (as in rooted properly) ts config, supply a ConfigProvider
    val configProviderLive: ULayer[ConfigProvider] = {
      val tmp: URLayer[TSConfig, ConfigProvider] = ZLayer(ZIO.service[TSConfig].map(TypesafeConfigProvider.fromTypesafeConfig(_)))
      serverTSConfigLive >>> tmp
    }

//    // use ZIO.config to supply a HOCON configured Server.Config for zio-http
    val serverConfigLive: ULayer[Server.Config] = ZLayer {
      ZIO
        .configProviderWith[ConfigProvider, Nothing, Server.Config] { defaultConfigProvider =>
          ZIO.service[ConfigProvider]
            .flatMap(_.load(Server.Config.config))
            .catchAll { configErr =>
              val tsFailedTryWithConfigProvider = for {
                ce <- defaultConfigProvider.load(Server.Config.config)
                _ <- ZIO.log("Bad configuration (Falling back to default ConfigProvider): " + configErr.getMessage())
              } yield ce

              tsFailedTryWithConfigProvider.catchAll(defaultConfigErr => for {
                _ <- ZIO.log("Bad configuration from default ConfigProvider (Falling back to ZIO defaults): " + defaultConfigErr.getMessage())
              } yield Server.Config.default)
            }
        }
        .provide(configProviderLive)
    }
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
      Services.serverConfigLive,
      Services.tsconfigLive,
      Server.live,
      InfoImpl.live,
      Routes.live
    ).exitCode
  }
}
