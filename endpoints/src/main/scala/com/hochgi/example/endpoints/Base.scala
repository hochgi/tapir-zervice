package com.hochgi.example.endpoints

import io.circe.generic.auto._
import com.hochgi.example.datatypes._
import sttp.model.StatusCode
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{EndpointOutput, PublicEndpoint, oneOf, oneOfDefaultVariant, statusCode, endpoint => ep}

object Base {

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
