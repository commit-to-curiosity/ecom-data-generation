ThisBuild / scalaVersion := "2.13.18"
ThisBuild / organization := "com.shopsphere"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "ecom-data-generation",

    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.4.3",
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.20.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % "2.20.0"
    ),

    Compile / unmanagedResourceDirectories ++= Seq(
      baseDirectory.value / "conf" / "hocon",
      baseDirectory.value / "conf" / "json",
      baseDirectory.value / "conf" / "yaml"
    )
  )