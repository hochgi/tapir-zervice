package com.hochgi.example.endpoints

import com.hochgi.example.datatypes.ManagedError
import sttp.tapir._

import java.lang.Integer.parseInt
import scala.util.Try

object Code {
  type FooEndpoint = PublicEndpoint[(Int, Int), ManagedError, Int, Any]

  def foo: FooEndpoint = {
    Base
      .api
      .name("Serious computation")
      .tag("Code")
      .description("Do some complex stuff")
      .get
      .in("code" / path[Int]("i") / path[Int]("j"))
      .out(stringBody.mapDecode { s =>
        DecodeResult.fromEitherString(s, Try(parseInt(s)).toEither.left.map(_.getMessage))
      }(_.toString))
  }
}
