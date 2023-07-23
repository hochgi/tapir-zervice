package com.hochgi.example.matapi

import com.hochgi.example.{endpoints => ep}
import com.hochgi.example.build
import sttp.tapir.{AnyEndpoint, Endpoint}
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.apispec.openapi.OpenAPI

import scala.util.Try

object EvalEndpoints {

  type EvaluatedEndpoint[T] = (AnyEndpoint, T)
  case class EvaluatedAPI[T](endpoints: List[EvaluatedEndpoint[T]], openApiDocs: OpenAPI)

  def eval[A, B, C, D, E, F](ep: Endpoint[A, B, C, D, E],
                             fn: Endpoint[A, B, C, D, E] => F): EvaluatedEndpoint[F] = ep -> fn(ep)

  def pairInput[I, O](f: I => O): I => (I, O) = i => i -> f(i)

  /**
   * This method is a single shared place to configure swagger.
   * It should be used from both the server, and the materialize-api module,
   * whose sole purpose is to generate swagger yaml during build.
   * Forcing all places to use the same shared method ensures
   * build & runtime swagger definitions stays consistent.
   */
  def evalAll[T](evalBuild:     ep.Info.BuildEndpoint            => T,
                 evalAllConfig: ep.Info.AllConfigEndpoint        => T,
                 evalConfig:    ep.Info.ConfigEndpoint           => T,
                 evalCodeFoo:   ep.Code.FooEndpoint              => T): EvaluatedAPI[T] = {
    val endpoints = List(
      pairInput(evalBuild)(ep.Info.build),
      pairInput(evalAllConfig)(ep.Info.allConfig),
      pairInput(evalConfig)(ep.Info.config),
      pairInput(evalCodeFoo)(ep.Code.foo))

    val openApiDocs: OpenAPI = OpenAPIDocsInterpreter().toOpenAPI(
      es      = endpoints.map(_._1),
      title   = "example API",
      version = build.Info.version)

    EvaluatedAPI(endpoints, openApiDocs)
  }
}
