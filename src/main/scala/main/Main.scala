package main

import akka.cluster.Cluster
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory
import akka.cluster.seed.ZookeeperClusterSeed
import com.typesafe.scalalogging.StrictLogging
import kamon.Kamon

object Main extends App with StrictLogging {
  val config = ConfigFactory.load()
  logger.info(s"config is ${config}")
  implicit val system = ActorSystem("my-app", config)
  Kamon.start()

  val cluster = Cluster(system)
  val zooCluster = ZookeeperClusterSeed(system)

  zooCluster.join()

  val chatSystem = ChatSystem.start()
  val chatSystemStats = system.actorOf(Props(new ShardsMonitorActor("rooms", chatSystem)), "chatSystemMonitor")
  val server = new HttpServer(chatSystem, config)
  server.run()

  import java.lang.management._
  import javax.management._
  val mbs = ManagementFactory.getPlatformMBeanServer()
  val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
  mbs.getAttributes(name, Array[String]("ProcessCpuLoad"))
}
