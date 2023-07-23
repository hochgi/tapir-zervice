package com.hochgi.example.matapi
import com.hochgi.example.matapi.EvalEndpoints.EvaluatedAPI
import io.circe.yaml.Printer.StringStyle
import scopt.OParser
import sttp.apispec.openapi.circe.yaml.RichOpenAPI

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import scala.util.{Failure, Success}

case class Arguments(file:        Path                = Paths.get("swagger.yaml"),
                     stringStyle: Option[StringStyle] = None)

object GenerateOpenAPI extends App {
  val builder = OParser.builder[Arguments]
  val aParser = {
    import builder._
    implicit val stringStyleRead: scopt.Read[Option[StringStyle]] = scopt.Read.reads(_.toLowerCase match {
      case "plain"                          => Some(StringStyle.Plain)
      case "doublequoted" | "double-quoted" => Some(StringStyle.DoubleQuoted)
      case "singlequoted" | "single-quoted" => Some(StringStyle.SingleQuoted)
      case "literal"                        => Some(StringStyle.Literal)
      case "folded"                         => Some(StringStyle.Folded)
      case _                                => None
    })
    OParser.sequence(
      programName("[from sbt] swagger/run"),
      help('h', "help").text("prints this usage text"),
      opt[Option[StringStyle]]('s', "string-style")
        .valueName("<FLAVOR>")
        .action((ss, arguments) => arguments.copy(stringStyle = ss))
        .text("string-style set the flavor of the yaml formatting\n" +
              "Optional values: <Plain|DoubleQuoted|SingleQuoted|Literal|Folded>"),
      arg[File]("<FILE>")
      .required()
      .action((f, arguments) => arguments.copy(file = f.toPath))
      .text("output file for swagger yaml")
    )
  }

  OParser.parse(aParser, args, Arguments()) match {
    case None =>
    case Some(arguments) =>
      val EvaluatedAPI(_, openApiDocs) =
        EvalEndpoints.evalAll(
          evalBuild     = _ => (),
          evalAllConfig = _ => (),
          evalConfig    = _ => (),
          evalCodeFoo   = _ => ())
      val yamlStr = arguments.stringStyle.fold(openApiDocs.toYaml)(openApiDocs.toYaml)
      Files.write(arguments.file, yamlStr.getBytes(StandardCharsets.UTF_8))
  }
}
