package com.hochgi.example.zerver

import org.scalacheck.{Arbitrary, Gen, Properties}
import org.scalacheck.Prop.forAll
import zio.{ConfigProvider, IO, Unsafe}
import zio.config.typesafe._
import zio.http.Server.Config.ResponseCompressionConfig
import zio.http.Server.RequestStreaming
import zio.http.{SSLConfig, Server}

import scala.concurrent.duration.Duration


/**
 * Verify Server.Config is derivable from HOCON.
 * This test ensures that the zio-http server config structure doesn't change,
 * when we e.g. upgrade version, thus we can rely on its structure for a drop in place configuration.
 */
class ServerConfigCheck extends Properties("Server.Config") {

  def grabUnsafely[E, R](effect: IO[E, R]): Either[E, R] = Unsafe.unsafe { implicit unsafe =>
    val asEither = effect.fold[Either[E, R]](Left.apply, Right.apply)
    zio.Runtime.default.unsafe.run(asEither).getOrThrow()
  }

  def dataHocon(indent: String): Gen[String] = {
    val generate = "generate"
    Gen.oneOf(generate, "Path", "Resource").flatMap {
      case `generate` => Gen.const("= " + generate)
      case suffix => for {
        cert <- Gen.alphaNumStr
        if cert.nonEmpty
        key <- Gen.alphaNumStr
        if key.nonEmpty
      } yield s"""{
       |$indent  cert$suffix = $cert
       |$indent  key$suffix = $key
       |$indent}""".stripMargin
    }
  }

  def sslConfigHocon(indent: String): Gen[String] = for {
    provider <- Gen.oneOf("jdk", "openssl")
    behaviour <- Gen.oneOf("accept", "fail", "redirect")
    data <- dataHocon(indent)
  } yield s"""provider = $provider
        |${indent}behaviour = $behaviour
        |${indent}data $data""".stripMargin

  val hostGen: Gen[String] = {
    val ipv4AddressGen: Gen[String] = {
      for {
        byte1 <- Gen.choose(0, 255)
        byte2 <- Gen.choose(0, 255)
        byte3 <- Gen.choose(0, 255)
        byte4 <- Gen.choose(0, 255)
      } yield s"$byte1.$byte2.$byte3.$byte4"
    }

    val ipv6AddressGen: Gen[String] = Gen
      .listOfN(8, Gen.choose(0, 65535))
      .map(_.map(_.toHexString).mkString("\"[", ":", "]\""))

    Gen.oneOf(ipv4AddressGen, ipv6AddressGen, Gen.const("localhost"))
  }

  def compressionOptionsHocon(indent: String): Gen[String] = for {
    level           <- Gen.option(Gen.choose(0, 9))
    bits            <- Gen.option(Gen.choose(9, 15))
    mem             <- Gen.option(Gen.choose(1, 9))
    compressionType <- Gen.oneOf("gzip", "deflate")
  } yield {
    val sb = new StringBuilder("{\n")
    level.foreach { s =>
      sb ++= indent
      sb ++= "  level = "
      sb ++= s.toString
      sb += '\n'
    }
    bits.foreach { s =>
      sb ++= indent
      sb ++= "  bits = "
      sb ++= s.toString
      sb += '\n'
    }
    mem.foreach { s =>
      sb ++= indent
      sb ++= "  mem = "
      sb ++= s.toString
      sb += '\n'
    }
    sb ++= indent
    sb ++= "  type = "
    sb ++= compressionType
    sb += '\n'
    sb ++= indent
    sb ++= "}"
    sb.result()
  }

  def responseCompressionHocon(indent: String): Gen[String] = for {
    contentThreshold <- Gen.choose(0, Int.MaxValue)
    options <- Gen.nonEmptyListOf(compressionOptionsHocon(indent + "  "))
  } yield {
    val sb = new StringBuilder()
    sb ++= indent
    sb ++= "content-threshold = "
    sb ++= String.valueOf(contentThreshold)
    sb += '\n'
    if (options.nonEmpty) {
      sb ++= indent
      sb ++= "options = [\n "
      sb ++= indent
      var sep = ' '
      options.foreach { opt =>
        sb += sep
        sep = ','
        sb ++= opt
      }
      sb += '\n'
      sb ++= indent
      sb += ']'
    }
    sb.result()
  }

  def requestStreamingHocon(indent: String) = for {
    enabled <- Gen.option(Arbitrary.arbitrary[Boolean])
    maximumContentLength <- Gen.option(Gen.posNum[Int])
  } yield {
    val sb = new StringBuilder()
    enabled.foreach { b =>
      sb ++= indent
      sb ++= "enabled = "
      sb ++= String.valueOf(b)
      sb += '\n'
    }
    maximumContentLength.foreach { l =>
      sb ++= indent
      sb ++= "maximum-content-length = "
      sb ++= String.valueOf(l)
      sb += '\n'
    }
    sb.result()
  }

  def formatDuration(d: Duration) = {
    val c = d.toCoarsest
    val u = c.unit
    val l = c.length
    s"$l ${u.toString.toLowerCase}"
  }

  val hoconDuration: Gen[String] = Gen.duration.suchThat(_.isFinite).map(formatDuration)

  def serverConfigHocon(indent: String): Gen[String] = for {
    sslConfig               <- Gen.option(sslConfigHocon(indent))
    host                    <- Gen.option(hostGen)
    port                    <- Gen.option(Gen.choose(0, 65535))
    acceptContinue          <- Gen.option(Arbitrary.arbitrary[Boolean])
    keepAlive               <- Gen.option(Arbitrary.arbitrary[Boolean])
    requestDecompression    <- Gen.option(Gen.oneOf("no", "strict", "nonstrict"))
    responseCompression     <- Gen.option(responseCompressionHocon(indent + "  "))
    requestStreaming        <- Gen.option(requestStreamingHocon(indent + "  "))
    maxHeaderSize           <- Gen.option(Gen.posNum[Int])
    logWarningOnFatalError  <- Gen.option(Arbitrary.arbitrary[Boolean])
    gracefulShutdownTimeout <- Gen.option(hoconDuration)
    idleTimeout             <- Gen.option(hoconDuration)
  } yield {
    val sb = new StringBuilder()
    def appendOpt(opt: Option[String], key: Option[(String, Boolean)]): Unit = opt.foreach { hocon =>
      key.foreach { case (k, composite) =>
        sb ++= k
        if (composite) sb ++= " {\n"
        else sb ++= " = "
      }
      sb ++= hocon
      sb += '\n'
      key.foreach { case (_, composite) =>
        if (composite)
          sb ++= "}\n"
      }
    }

    appendOpt(sslConfig, None)
    appendOpt(host, Some("binding-host" -> false))
    appendOpt(port.map(String.valueOf), Some("binding-port" -> false))
    appendOpt(acceptContinue.map(String.valueOf), Some("accept-continue" -> false))
    appendOpt(keepAlive.map(String.valueOf), Some("keep-alive" -> false))
    appendOpt(requestDecompression, Some("request-decompression" -> false))
    appendOpt(responseCompression, Some("response-compression" -> true))
    appendOpt(requestStreaming, Some("request-streaming" -> true))
    appendOpt(maxHeaderSize.map(String.valueOf), Some("max-header-size" -> false))
    appendOpt(logWarningOnFatalError.map(String.valueOf), Some("log-warning-on-fatal-error" -> false))
    appendOpt(gracefulShutdownTimeout, Some("graceful-shutdown-timeout" -> false))
    appendOpt(idleTimeout, Some("idle-timeout" -> false))

    sb.result()
  }

  property("SSLConfig") = forAll(sslConfigHocon("")) { hocon =>
    val io = ConfigProvider.fromHoconString(hocon).load(SSLConfig.config)
    val either = grabUnsafely(io)
    either.isRight
  }

  property("ResponseCompressionConfig") = forAll(responseCompressionHocon("")) { hocon =>
    val io = ConfigProvider.fromHoconString(hocon).load(ResponseCompressionConfig.config)
    val either = grabUnsafely(io)
    either.isRight
  }

  property("RequestStreaming") = forAll(requestStreamingHocon("")) { hocon =>
    val io = ConfigProvider.fromHoconString(hocon).load(RequestStreaming.config)
    val either = grabUnsafely(io)
    either.isRight
  }

  property("Server.Config") = forAll(serverConfigHocon("")) { hocon =>
    println(hocon + "\n\n\n\n")
    val io = ConfigProvider.fromHoconString(hocon).load(Server.Config.config)
    val either = grabUnsafely(io)
    either.isRight
  }
}
