package com.hochgi.example.zerver.matapi

import com.hochgi.example.build.Info.{toJson => buildInfoJson}
import com.hochgi.example.datatypes._
import com.hochgi.example.endpoints.Info.{AllConfigEndpoint, BuildEndpoint, ConfigEndpoint}
import com.typesafe.config.{Config, ConfigException => CEx}
import org.apache.commons.lang3.exception.ExceptionUtils
import sttp.tapir.ztapir.ZServerEndpoint
import zio._

import scala.util.{Failure, Success, Try}

trait Info {
  val build: BuildEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicPure[Task](_ => Right(buildInfoJson))

  def allConfig: AllConfigEndpoint => ZServerEndpoint[Any, Any]

  def config: ConfigEndpoint => ZServerEndpoint[Any, Any]
}

case class InfoImpl(conf: Config) extends Info {
  override val allConfig: AllConfigEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicPure[Task](_ => Right(conf))

  override val config: ConfigEndpoint => ZServerEndpoint[Any, Any] =
    _.serverLogicPure[Task] { path =>
        Try(conf.getConfig(path)) match {
          case Success(c)                => Right(c.atPath(path))
          case Failure(e: CEx.Missing)   => Left(NotFound(s"'$path' is missing: ${e.getMessage}"))
          case Failure(e: CEx.WrongType) => Left(ExpectationFailed(s"'$path' is of wrong type: ${e.getMessage}"))
          case Failure(unknown)          => Left(GeneralError(ExceptionUtils.getStackTrace(unknown), 500))
        }
    }
}
object InfoImpl {
  val live: URLayer[Config, InfoImpl] =
    ZLayer(ZIO.service[Config].map(InfoImpl.apply))
}
