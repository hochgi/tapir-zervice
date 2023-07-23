package com.hochgi.example.server.routes

import com.typesafe.config.{Config, ConfigException => CEx}
import com.hochgi.example.datatypes._
import com.hochgi.example.endpoints.Info.{AllConfigEndpoint, BuildEndpoint, ConfigEndpoint}
import com.hochgi.example.build.Info.{toJson => buildInfoJson}
import org.apache.commons.lang3.exception.ExceptionUtils

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object InfoRoutes {

  val build: BuildEndpoint => EvaluatedEndpoint[Unit, String] =
    buildEndpoint => buildEndpoint.serverLogicPure[Future](_ => Right.apply(buildInfoJson))

  def allConfig(config: Config): AllConfigEndpoint => EvaluatedEndpoint[Unit, Config] =
    allConfigEndpoint => allConfigEndpoint.serverLogicPure[Future](_ => Right(config))

  def config(config: Config): ConfigEndpoint => EvaluatedEndpoint[String, Config] =
    configEndpoint => configEndpoint.serverLogicPure[Future] { path =>
      Try(config.getConfig(path)) match {
        case Success(c)                => Right(c.atPath(path))
        case Failure(e: CEx.Missing)   => Left(NotFound(s"'$path' is missing: ${e.getMessage}"))
        case Failure(e: CEx.WrongType) => Left(ExpectationFailed(s"'$path' is of wrong type: ${e.getMessage}"))
        case Failure(unknown)          => Left(GeneralError(ExceptionUtils.getStackTrace(unknown), 500))
      }
    }
}
