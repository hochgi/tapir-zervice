package com.hochgi.example.zerver

import com.hochgi.example.matapi.EvalEndpoints
import com.hochgi.example.matapi.EvalEndpoints.EvaluatedAPI
import com.hochgi.example.zerver.matapi.{Info, Code}
import sttp.apispec.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.{SwaggerUI, SwaggerUIOptions}
import sttp.tapir.ztapir.ZServerEndpoint
import zio._

object Routes {
  val prometheusMetrics: PrometheusMetrics[Task] = PrometheusMetrics.default[Task]()

  val live: URLayer[Info with Code, List[ZServerEndpoint[Any, Any]]] = ZLayer {
    for {
      info <- ZIO.service[Info]
      code <- ZIO.service[Code]
    } yield {
      val metricsEndpoint: ZServerEndpoint[Any, Any] = prometheusMetrics.metricsEndpoint
      val EvaluatedAPI(endpoints, openAPI) = EvalEndpoints.evalAll(info.build, info.allConfig, info.config, code.foo, code.jsonWordCountSlidingWindow)
      val apiEndpoints: List[ZServerEndpoint[Any, Any]] = endpoints.map(_._2)
      val docsAsYaml: String = openAPI.toYaml
      val uiOptions: SwaggerUIOptions = SwaggerUIOptions.default.copy(pathPrefix = List("doc"))
      val swaggerUIEndPs = SwaggerUI[Task](docsAsYaml, uiOptions)
      apiEndpoints ++ swaggerUIEndPs ++ List(metricsEndpoint)
    }
  }
}
