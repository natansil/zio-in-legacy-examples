val zioGrpcVersion = "0.4.0"

addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0-RC2")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.8"
libraryDependencies += "com.thesamet.scalapb.zio-grpc" %% "zio-grpc-codegen" % zioGrpcVersion
