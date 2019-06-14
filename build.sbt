name := "gziptest"

version := "0.1"

scalaVersion := "2.12.8" /*"2.13.0"*/

scalacOptions ++= Seq("-Xlint", "-unchecked", "-deprecation", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "ammonite-ops" % "1.6.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.23" /*"2.6.0-M3"*/,
  "io.circe" %% "circe-parser" % "0.12.0-M3",
)
