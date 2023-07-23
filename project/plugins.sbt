addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.github.sbt" % "sbt-git" % "2.0.1")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
addSbtPlugin("io.stryker-mutator" % "sbt-stryker4s" % "0.14.3")

ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .dependsOn(specgen)

lazy val specgen = (project in file("specgen"))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "spec-gen",
    sbtPlugin := true
  )