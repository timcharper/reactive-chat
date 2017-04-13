name := "Reactive Chat"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.17"
val kamonVersion = "0.6.6"

version := "0.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.play" %% "play-json" % "2.4.3",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.0.5",
  "com.typesafe.akka" %% "akka-distributed-data-experimental" % akkaVersion,
  "io.kamon" %% "kamon-core" % kamonVersion,
  "io.kamon" %% "kamon-statsd" % kamonVersion,
  "io.kamon" %% "kamon-system-metrics" % kamonVersion,
  "io.kamon" %% "kamon-akka-2.4" % kamonVersion,
  "com.sclasen" %% "akka-zk-cluster-seed" % "0.1.8",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "ch.qos.logback" % "logback-classic" % "1.1.7"
)

aspectjSettings

javaOptions in reStart <++= AspectjKeys.weaverOptions in Aspectj

enablePlugins(JavaAppPackaging)
