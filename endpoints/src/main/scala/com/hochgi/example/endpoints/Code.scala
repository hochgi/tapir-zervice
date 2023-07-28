package com.hochgi.example.endpoints

import com.hochgi.example.datatypes.ManagedError
import sttp.tapir.{DecodeResult, PublicEndpoint}
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir._

import java.lang.Integer.parseInt
import scala.util.Try

object Code {
  type FooEndpoint = PublicEndpoint[(Int, Int), ManagedError, Int, Any]

  val baseCode: PublicEndpoint[Unit, ManagedError, Unit, Any] = Base
    .api
    .tag("Code")
    .in("code")

  val foo: FooEndpoint = baseCode
    .name("Serious computation")
    .description("Do some complex stuff")
    .get
    .in("foo" / path[Int]("i") / path[Int]("j"))
    .out(stringBody.mapDecode { s =>
      DecodeResult.fromEitherString(s, Try(parseInt(s)).toEither.left.map(_.getMessage))
    }(_.toString))

  type WordCountSlidingWindowEndpoint = PublicEndpoint[Unit, ManagedError, Map[String, Map[String, Int]], Any]

  val jsonWordCountSlidingWindow: WordCountSlidingWindowEndpoint = baseCode
    .name("Word count")
    .description("Word count sliding window from json process")
    .get
    .in("wc")
    .out(jsonBody[Map[String, Map[String, Int]]])
}
