package main

import akka.actor.ActorRef
import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import kamon.Kamon
import scala.concurrent.duration._

/**
  * Polls every three seconds to ask Akka how many entities and shards live on this node.
  * Reports data via Kamon histograms
  *
  * @param name The prefix name of the metric
  * @param shardRegionRef the shardRegion we wish to monitor
  */
class ShardsMonitorActor(name: String, shardRegionRef: ActorRef) extends Actor with ActorLogging {
  val shardCount = Kamon.metrics.histogram(s"${name}-shard-count")
  val entityCount = Kamon.metrics.histogram(s"${name}-entity-count")

  override def preStart:Unit = {
    import context.dispatcher
    val s = context.system.scheduler.schedule(3.seconds, 3.seconds, self, 'pulse)
  }

  def receive = {
    case 'pulse =>
      log.info("hey I'm pulsing")
      shardRegionRef ! ShardRegion.GetShardRegionState

    case state: ShardRegion.CurrentShardRegionState =>
      val entities = state.shards.foldLeft(0) { (c, shard) => c + shard.entityIds.size }
      shardCount.record(state.shards.size.toLong)
      entityCount.record(entities.toLong)

      log.info(s"Shard has ${state.shards.size} shards. First 10: ${state.shards.map(_.shardId).take(10).mkString(",")}")
      log.info(s"Shard has ${entities} entities.")
  }

}
