lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.13.3"
    )),
    name := "scalatest-example"
  )

libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    "dev.zio" %% "zio" % "1.0.3",
    "com.wix" %% "greyhound-future" % "0.1.3",
    "io.grpc" % "grpc-netty" % "1.33.1",
    "io.grpc" % "grpc-services" % "1.33.1",
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % "0.11.0-M3",
    "org.scalatest" %% "scalatest" % "3.2.2" % Test
)