package main

import akka.actor.ActorRef
import akka.actor.{Actor, ActorLogging}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.ClusterShardingStats
import kamon.Kamon
import scala.concurrent.duration._

class ShardsMonitorActor(name: String, actorRef: ActorRef) extends Actor with ActorLogging {
  val shardCount = Kamon.metrics.histogram(s"${name}-shard-count")
  val entityCount = Kamon.metrics.histogram(s"${name}-entity-count")

  override def preStart:Unit = {
    import context.dispatcher
    val s = context.system.scheduler.schedule(3.seconds, 3.seconds, self, 'pulse)
  }

  def receive = {
    case 'pulse =>
      log.info("hey I'm pulsing")
      actorRef ! ShardRegion.GetShardRegionState

    case state: ShardRegion.CurrentShardRegionState =>
      val entities = state.shards.foldLeft(0) { (c, shard) => c + shard.entityIds.size }
      shardCount.record(state.shards.size.toLong)
      entityCount.record(entities.toLong)

      log.info(s"Shard has ${state.shards.size} shards. First 50: ${state.shards.map(_.shardId).take(50).mkString(",")}")
      log.info(s"Shard has ${entities} entities.")
  }

}
