package main

import akka.actor.ActorRef
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

  // We create the local actor system; note that at this point we are not yet in a cluster
  implicit val system = ActorSystem("my-app", config)

  // Launch the necessary Kamon monitoring infrastructure
  Kamon.start()

  // Connect to our cluster, using Zookeeper for leader election (we only want to create ONE cluster if multiple
  // instances are launching in parallel)
  val cluster = Cluster(system)
  val zooCluster = ZookeeperClusterSeed(system)
  zooCluster.join()


  // Start our sharded chat actors; We can send a Chat or Subscribe message and it will be routed the the appropriate
  // destinations
  val chatSystem: ActorRef = ChatSystem.start()

  // Launch an actor to monitor the shards and entities for this host; report via a Kamon histogram
  val chatSystemStats = system.actorOf(Props(new ShardsMonitorActor("rooms", chatSystem)), "chatSystemMonitor")

  // Launch our HTTP interface
  val server = new HttpServer(chatSystem, config)
  server.run()
}
