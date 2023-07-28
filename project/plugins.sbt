import sbt.Defaults.sbtPluginExtra

import sbt.Keys._


/*
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.14.3")
// addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1") TODO: verify zio-sbt-ecosystem pulls a compatible version. Try to upgrade
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "1.0.0")
addSbtPlugin("dev.zio" % "zio-sbt-ecosystem" % "0.4.0-alpha.12")
 */

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.14.3")
libraryDependencies += {
  val sbtV = (pluginCrossBuild / sbtBinaryVersion).value
  val scalaV = (update / scalaBinaryVersion).value
  val excludeOldGitModuleID = ("dev.zio" % "zio-sbt-ecosystem" % "0.4.0-alpha.12").exclude("com.typesafe.sbt","sbt-git")
  sbtPluginExtra(excludeOldGitModuleID, sbtV, scalaV)
}
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")

ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .dependsOn(specgen)

lazy val specgen = (project in file("specgen"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "spec-gen",
    sbtPlugin := true
  )