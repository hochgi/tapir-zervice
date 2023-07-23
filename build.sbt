import Dependencies._

import java.io.File
import sbt.internal.util.complete._, Parsers._

import sbt._
import Keys._

val scala212n213CrossVersions = List("2.13.11", "2.12.18")
val scala213CrossVersion      = scala212n213CrossVersions.init

val sharedSettings = {
  val copyStrykerToRoot = taskKey[Unit]("copyStrykerToRoot")
  Seq(
    version := "0.1.0-SNAPSHOT",
    organization := "com.hochgi",
    scalaVersion := scala213CrossVersion.head,
    crossScalaVersions := scala213CrossVersion,
    scalacOptions ++= {
      val safeForCrossParams = Seq(
        "-encoding", "UTF-8",    // source files are in UTF-8
        "-deprecation",          // warn about use of deprecated APIs
        "-unchecked",            // warn about unchecked type parameters
        "-feature",              // warn about misused language features
        "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
        "-Xlint"                 // enable handy linter warnings
      )

      scalaBinaryVersion.value match {
        case "2.11" => safeForCrossParams :+ "-Ypartial-unification" :+ "-language:existentials"
        case "2.12" => safeForCrossParams :+ "-Ypartial-unification"
        case "2.13" => safeForCrossParams
        case binver => throw new NotImplementedError(s"Build not defined for scala $binver")
      }
    },
    Compile / unmanagedSourceDirectories := {
      val original = (Compile / unmanagedSourceDirectories).value
      val sharedSourceDir = baseDirectory.value / "src" / "main"
      val crossDir = scalaBinaryVersion.value match {
        case "2.12" => "scala-2.12"
        case "2.13" => "scala-2.13"
        case binver => throw new NotImplementedError(s"Build not defined for scala $binver")
      }
      (original :+ (sharedSourceDir / crossDir)).distinct
    },
    publish / skip := true,
    versionScheme := Some("semver-spec"),
    copyStrykerToRoot := {
      val basedir: File = baseDirectory.value
      val strykerConf: File = basedir / "stryker4s.conf"
      val rootdir = basedir.toPath.getParent.toFile
      val rootStrykerConf = rootdir / "stryker4s.conf"
      if (rootStrykerConf.exists()) throw new IllegalStateException("root stryker4s conf exist!")
      else if (!strykerConf.exists()) throw new IllegalStateException("base stryker4s conf does not exist!")
      else IO.copyFile(strykerConf, rootStrykerConf)
    },
    libraryDependencies ++= Seq(
      scalacheck,
      scalatest
    )
  )
}

lazy val datatypes = (project in file("datatypes"))
  .settings(
    sharedSettings,
    name := "datatypes",
    crossScalaVersions := scala212n213CrossVersions,
    publish / skip := false
  )

lazy val endpoints = (project in file("endpoints"))
  .dependsOn(datatypes)
  .settings(
    sharedSettings,
    name := "endpoints",
    crossScalaVersions := scala212n213CrossVersions,
    publish / skip := false,
    libraryDependencies ++= Seq(
      tapirCore,
      tapirJsonCirce,
      circeCore,
      circeGeneric,
      tsConfig
    )
  )

lazy val logic = (project in file("logic"))
  .dependsOn(datatypes)
  .settings(
    sharedSettings,
    name := "logic",
    libraryDependencies ++= Seq(
      logbackCore,
      logbackClassic % Test
    )
  )

/**
 * This module could have been a part of the server module in theory.
 * It does not add new dependencies in Compile scope, thus is safe.
 * It cannot be part of endpoints module since it does add dependencies we don't want in a client/driver module.
 * The decision to keep it separate comes from the fact the this module does nont depend on logic,
 * thus it ensures the API spec will not accidentally include any dependency or definition from logic module.
 */
lazy val matapi = (project in file("materialize-api"))
  .enablePlugins(BuildInfoPlugin, GitVersioning, SpecGen)
  .dependsOn(endpoints)
  .settings(
    sharedSettings,
    name := "materialize-api",
    libraryDependencies ++= Seq(
      tapirOpenapiCirceYaml,
      tapirOpenapiDocs,
      scopt % Spec // scopt is only used in the build, to generate the openapi.yaml,
                   // thus we only scope it to the special Spec config which makes it
                   // available only there, and it does not contaminate server dependencies
                   // which depends on matapi's other definitions in Compile configuration scope.
    ),
    buildInfoPackage := "com.hochgi.example.build",
    buildInfoObject := "Info",
    buildInfoOptions ++= Seq(BuildInfoOption.ToJson, BuildInfoOption.BuildTime),
    buildInfoKeys := Seq[BuildInfoKey](
      version,
      scalaVersion,
      sbtVersion,
      git.gitCurrentBranch,
      git.gitHeadCommit,
      git.gitHeadCommitDate,
      git.gitUncommittedChanges
    ),
    Spec / specGenMain := "com.hochgi.example.matapi.GenerateOpenAPI",
    Spec / specGenArgs := Seq("-s", "plain", (target.value / "openapi.yaml").getAbsolutePath),
  )

lazy val server = (project in file("server"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(logic, matapi)
  .settings(
    sharedSettings,
    name := "server",
    libraryDependencies ++= Seq(
      akkaHttpMetricsDropwizard,
      akkaHttp,
      akkaActor,
      akkaSlf4j,
      akkaProtobufV3,
      akkaStreams,
      commonsLang3,
      logbackClassic,
      metricsJVM,
      tapirCore,
      tapirAkkaHttpServer,
      tapirSwaggerUi,
      tapirOpenapiDocs,
      tapirJsonCirce
    ),
    Universal / mappings ++= {
      val resources = (Compile / resourceDirectory).value
      val cnf = resources / "application.conf"
      val log = resources / "logback.xml"
      Seq(
        cnf -> "conf/application.conf",
        log -> "conf/logback.xml")
    },
    Universal / javaOptions ++= Seq(
      "-Dconfig.override_with_env_vars=true",
      "-Dlogback.configurationFile=conf/logback.xml",
      "-Dconfig.file=conf/application.conf"
    )
  )

lazy val zerver = (project in file("zerver"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(logic, matapi)
  .settings(
    sharedSettings,
    name := "zerver",
    libraryDependencies ++= Seq(
      tapirZioHttpServer,
      tapirPrometheusMetrics,
      tapirJsonZio,
      logbackClassic,
      zioLogSlf4j,
      zioConfig,
      zioConfigMagnolia,
      zioConfigTypesafe,
      commonsLang3,
      metricsJVM,
      tapirCore,
      tapirSwaggerUi,
      tapirOpenapiDocs,
      "org.apache.arrow" % "flight-sql-jdbc-driver" % "12.0.1"
    ),
    Universal / mappings ++= {
      val resources = (Compile / resourceDirectory).value
      val cnf = resources / "application.conf"
      val log = resources / "logback.xml"
      Seq(
        cnf -> "conf/application.conf",
        log -> "conf/logback.xml")
    },
    Universal / javaOptions ++= Seq(
      "-Dconfig.override_with_env_vars=true",
      "-Dlogback.configurationFile=conf/logback.xml",
      "-Dconfig.file=conf/application.conf"
    )
  )


val allModules: Seq[sbt.ProjectReference] = Seq(
  datatypes,
  endpoints,
  logic,
  matapi,
  server,
  zerver)

val moduleNames: Seq[String] = allModules.map(_.project match {
  case LocalProject(projectName) => projectName
  case ProjectRef(_, projectName) => projectName
  case _ => ???
})

val rmStrykerFromRoot = taskKey[Unit]("rmStrykerFromRoot")
val projBare = Parser.oneOf(moduleNames.map(Parser.literal))
val projName  = Space ~> projBare

lazy val root = (project in file("."))
  .aggregate(datatypes, endpoints, matapi, logic, server)
  .settings(
    // opt out of aggregation of tasks we need to wire only in root
    baseDirectory / aggregate := false,
    // crossScalaVersions must be set to Nil on the aggregating project
    crossScalaVersions := Nil,
    publish / skip := true,
    rmStrykerFromRoot := IO.delete(baseDirectory.value / "stryker4s.conf"),
    commands += Command("strykerFor")(_ => projName){ case (state, projName) =>
      s"$projName/copyStrykerToRoot" ::
        s"project $projName"         ::
        "stryker"                    ::
        "project root"               ::
        "rmStrykerFromRoot"          :: state
    }
  )
