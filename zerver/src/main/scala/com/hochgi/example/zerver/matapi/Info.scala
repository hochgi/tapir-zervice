package com.hochgi.example.zerver.matapi

import com.hochgi.example.build.Info.{toJson => buildInfoJson}
import com.hochgi.example.datatypes._
import com.hochgi.example.endpoints.Info.{AllConfigEndpoint, BuildEndpoint, ConfigEndpoint}
import com.typesafe.config.{Config, ConfigException => CEx}
import org.apache.commons.lang3.exception.ExceptionUtils
import sttp.tapir.ztapir.ZServerEndpoint
import zio._

import scala.util.{Failure, Success, Try}

object Info {

  val build: BuildEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicPure[Task](_ => Right.apply(buildInfoJson))

  val zioConfig: URIO[Config, Config] = ZIO.environmentWithZIO[Config] { env =>
    ZIO.succeed(env.get)
  }

  val allConfig: AllConfigEndpoint => ZServerEndpoint[Config, Any] =
    allConfigEndpoint => allConfigEndpoint.serverLogicSuccess[URIO[Config, _]](_ => zioConfig)

  def config: ConfigEndpoint => ZServerEndpoint[Any, Any] =
    configEndpoint => configEndpoint.serverLogic[URIO[Config, _]]{ path =>
      zioConfig.map { conf =>
        Try(conf.getConfig(path)) match {
          case Success(c) => Right(c.atPath(path))
          case Failure(e: CEx.Missing) => Left(NotFound(s"'$path' is missing: ${e.getMessage}"))
          case Failure(e: CEx.WrongType) => Left(ExpectationFailed(s"'$path' is of wrong type: ${e.getMessage}"))
          case Failure(unknown) => Left(GeneralError(ExceptionUtils.getStackTrace(unknown), 500))
        }
      }
    }

}
