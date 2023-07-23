import sbt._

object Dependencies {
  
  // versions

  val tapirVersion = "1.6.2"
  val zioVersion = "2.0.15"
  val zioConfigVersion = "4.0.0-RC16"
  val zioLoggingVersion = "2.1.13"
  val akkaVersion = "2.6.21"
  val akkaHttpVersion = "10.2.10"
  val circeVersion = "0.14.5"
  val logbackVersion = "1.4.8"

  // ZIO
  val zio               = "dev.zio" %% "zio"                 % zioVersion
  val zioStreams        = "dev.zio" %% "zio-streams"         % zioVersion
  val zioJson           = "dev.zio" %% "zio-json"            % "0.6.0"
  val zioLogging        = "dev.zio" %% "zio-logging"         % zioLoggingVersion
  val zioLogSlf4j       = "dev.zio" %% "zio-logging-slf4j"   % zioLoggingVersion
  val zioConfig         = "dev.zio" %% "zio-config"          % zioConfigVersion
  val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
  val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % zioConfigVersion
  val zioConfigRefined  = "dev.zio" %% "zio-config-refined"  % zioConfigVersion

  // akka
  val akkaHttp          = "com.typesafe.akka" %% "akka-http"        % akkaHttpVersion
  val akkaActor         = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaSlf4j         = "com.typesafe.akka" %% "akka-slf4j"       % akkaVersion
  val akkaProtobufV3    = "com.typesafe.akka" %% "akka-protobuf-v3" % akkaVersion
  val akkaStreams       = "com.typesafe.akka" %% "akka-stream"      % akkaVersion

  // tapir
  val tapirCore              = "com.softwaremill.sttp.tapir"   %% "tapir-core"               % tapirVersion  
  val tapirZioHttpServer     = "com.softwaremill.sttp.tapir"   %% "tapir-zio-http-server"    % tapirVersion
  val tapirAkkaHttpServer    = "com.softwaremill.sttp.tapir"   %% "tapir-akka-http-server"   % tapirVersion
  val tapirSwaggerUi         = "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui"         % tapirVersion
  val tapirOpenapiDocs       = "com.softwaremill.sttp.tapir"   %% "tapir-openapi-docs"       % tapirVersion
  val tapirJsonZio           = "com.softwaremill.sttp.tapir"   %% "tapir-json-zio"           % tapirVersion
  val tapirJsonCirce         = "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"         % tapirVersion
  val tapirPrometheusMetrics = "com.softwaremill.sttp.tapir"   %% "tapir-prometheus-metrics" % tapirVersion
  val tapirOpenapiCirceYaml  = "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"       % "0.5.3"


  // circe (JSON)
  val circeCore    = "io.circe" %% "circe-core"    % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion

  // logging
  val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
  val logbackCore    = "ch.qos.logback" % "logback-core"    % logbackVersion

  // metrics & events
  val dropwizardCore = "io.dropwizard.metrics" % "metrics-core" % "4.2.19"
  val akkaHttpMetricsDropwizard = "fr.davit" %% "akka-http-metrics-dropwizard" % "1.7.1"
  val metricsJVM = "com.codahale.metrics" % "metrics-jvm" % "3.0.2"

  // util
  val tsConfig     = "com.typesafe"       % "config"         % "1.4.2"
  val commonsLang3 = "org.apache.commons" % "commons-lang3"  % "3.12.0"
  val jctools      = "org.jctools"        % "jctools-core"   % "4.0.1"
  val mime4j       = "org.apache.james"   % "apache-mime4j"  % "0.8.9"
  val scopt        = "com.github.scopt"  %% "scopt"          % "4.1.0"

  // test
  val tapirSttpStubServer = "com.softwaremill.sttp.tapir"   %% "tapir-sttp-stub-server" % tapirVersion % Test
  val zioTest             = "dev.zio"                       %% "zio-test"               % zioVersion   % Test
  val zioTestSbt          = "dev.zio"                       %% "zio-test-sbt"           % zioVersion   % Test
  val sttpZioJson         = "com.softwaremill.sttp.client3" %% "zio-json"               % "3.8.16"     % Test
  val scalacheck          = "org.scalacheck"                %% "scalacheck"             % "1.17.0"     % Test
  val scalatest           = "org.scalatest"                 %% "scalatest"              % "3.2.16"     % Test
}
