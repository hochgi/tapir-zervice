package com.hochgi.build.specgen

import sbt.Keys._
import sbt._

object SpecGen extends AutoPlugin {

  object autoImport {
    val Spec = config("spec").extend(Runtime)
    val specGenMain = settingKey[String]("main class (FQCN) to run")
    val specGenArgs = settingKey[Seq[String]]("arguments to pass runner")
    val specGenMake = taskKey[Unit]("run code/resource generation from spec configuration")
  }

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = inConfig(Spec)(Defaults.configSettings ++ Seq(
    sourceDirectory := baseDirectory.value / "src" / "spec",
    unmanagedSourceDirectories := Seq((Spec / sourceDirectory).value),
    compile := compile.dependsOn(Compile / compile).value,
    specGenMain := "please set a main class to run",
    specGenArgs := Seq.empty,
    specGenMake := {
      val logger = streams.value.log
      val cp = Attributed.data((Spec / fullClasspath).value)
      (Spec / runner).value.run(specGenMain.value, cp, specGenArgs.value, logger).get
    }
  )) :+ (ivyConfigurations := overrideConfigs(Spec)(ivyConfigurations.value))
}