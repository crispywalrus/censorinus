
organization := "llc.flyingwalrus"
name := "scala-statsd"
scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.12.8","2.13.0","2.11.12")

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-language:_",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard"
)

val silencerVersion = "1.4.1"

libraryDependencies ++= Seq(
  compilerPlugin("com.github.ghik" %% "silencer-plugin" % silencerVersion),
  "com.github.ghik" %% "silencer-lib" % silencerVersion % Provided
) ++ Seq(
  "org.scalactic" %% "scalactic" % "3.0.8",
  "org.scalatest" %% "scalatest" % "3.0.8",
  "org.scalacheck" %% "scalacheck" % "1.14.0"
).map(_ % Test)
