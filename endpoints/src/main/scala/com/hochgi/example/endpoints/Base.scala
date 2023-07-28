package com.hochgi.example.endpoints

import com.hochgi.example.datatypes._
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.ztapir._
import sttp.tapir.{EndpointOutput, PublicEndpoint, oneOf, oneOfDefaultVariant, statusCode, endpoint => ep}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

object Base {

  implicit val encoder: JsonEncoder[GeneralError.Msg] = DeriveJsonEncoder.gen[GeneralError.Msg]
  implicit val decoder: JsonDecoder[GeneralError.Msg] = DeriveJsonDecoder.gen[GeneralError.Msg]

  val generalError: EndpointOutput[ManagedError] = jsonBody[GeneralError.Msg]
    .map(_.message)(GeneralError.Msg)
    .description("General Error")
    .and(statusCode.map(_.code)(StatusCode.unsafeApply))
    .mapTo[GeneralError]
    .map[ManagedError](identity[ManagedError](_)) { managedError =>
      GeneralError(managedError.message, managedError.statusCode)
    }

  val api: PublicEndpoint[Unit, ManagedError, Unit, Any] =
    ep.in("api")
      .errorOut(
        oneOf[ManagedError](
          oneOfDefaultVariant[ManagedError](
            generalError)))
}
