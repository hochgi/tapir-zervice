package com.hochgi.example.endpoints

import com.hochgi.example.datatypes.ManagedError
import sttp.tapir._
import com.typesafe.config._
import sttp.model.MediaType
import sttp.tapir.Codec.JsonCodec
import sttp.tapir.SchemaType.SProduct

import scala.util.Try

object Info {

  type BuildEndpoint = PublicEndpoint[Unit, ManagedError, String, Any]
  def build: BuildEndpoint = {
    Base
      .api
      .name("Build Info")
      .tag("Info")
      .description("Get static build info")
      .get
      .in("info")
      .out(customCodecJsonBody[String](Codec.json[String](DecodeResult.Value(_))(identity)))
  }

  // https://github.com/lightbend/config/blob/master/HOCON.md#mime-type
  case class Hocon() extends CodecFormat {
    override val mediaType: MediaType = MediaType("application", "hocon")
  }
  private val hoconBody: EndpointIO.Body[String, Config] =
    stringBodyUtf8AnyFormat[Config, Hocon](Codec.string.format(Hocon())
      .map(s => ConfigFactory.parseString(s))(_.root().render()))

  private val parseJSON: String => DecodeResult[Config] = inputString => Try {
    ConfigFactory.parseString(
      inputString,
      ConfigParseOptions
        .defaults()
        .setSyntax(ConfigSyntax.JSON))
  }.fold(DecodeResult.Error(inputString, _), DecodeResult.Value.apply)

  private val formatJSON: Config => String = _.root().render(
    ConfigRenderOptions
      .concise()
      .setJson(true))

  private val configCustomJsonCodec: JsonCodec[Config] = Codec.json[Config](parseJSON)(formatJSON)(Schema(SProduct(Nil), None))

  private val confBase: PublicEndpoint[Unit, ManagedError, Config, Any] = Base
    .api
    .in("conf")
    .tag("Info")
    .get
    .out(oneOfBody(hoconBody, customCodecJsonBody[Config](configCustomJsonCodec)))

  type AllConfigEndpoint = PublicEndpoint[Unit, ManagedError, Config, Any]
  val allConfig: AllConfigEndpoint = confBase
    .name("Config")
    .description("Get entire configuration")

  type ConfigEndpoint = PublicEndpoint[String, ManagedError, Config, Any]
  val config: ConfigEndpoint = confBase
    .name("Config at path")
    .description("Get configuration for some path")
    .get
    .in(path[String].name("path").description("Configuration key path"))
}
