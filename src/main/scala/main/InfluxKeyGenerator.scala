package main

import kamon.statsd.PercentEncoder
import com.typesafe.config.Config

class InfluxKeyGenerator(config: Config) extends kamon.statsd.MetricKeyGenerator {
  def hostName: String =
    java.net.InetAddress.getLocalHost().getHostName().split('.').headOption.getOrElse("none")

  val configSettings =
    config.getConfig("kamon.statsd.simple-metric-key-generator")

  val application =
    configSettings.getString("application")

  val includeHostname =
    configSettings.getBoolean("include-hostname")

  val hostnameOverride =
    configSettings.getString("hostname-override")

  val normalizer =
    createNormalizer(configSettings.getString("metric-name-normalization-strategy"))

  def createNormalizer(strategy: String): String => String = strategy match {
    case "percent-encode" =>
      PercentEncoder.encode
    case "normalize" =>
      (s: String) => s.replace(" ", "_").replace("/", "_").replace(".", "_").replace(": ", "-")
  }

  val normalizedHostname =
    if (hostnameOverride.equals("none")) normalizer(hostName)
    else normalizer(hostnameOverride)

  val baseName: String =
    if (includeHostname) s"$application.$normalizedHostname"
    else application

  import kamon.metric.{Entity,MetricKey}

  def generateKey(entity: Entity, metricKey: MetricKey): String = {
    s"${metricKey.name},category=${entity.category},host=${normalizedHostname},entityname=${normalizer(entity.name)}"
  }
}
