package main

import akka.actor.ActorInitializationException
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.DeathPactException
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.actor.SupervisorStrategy
import akka.actor.Terminated
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ClusterShardingSettings
import akka.cluster.sharding.ShardRegion
import scala.collection.mutable

import akka.actor.{ActorRef, Actor}

/**
  * This is the actor that ultimately handles the logic of our room
  *
  * Keeps track of all subscribers, and relays each received message to all subscribers in th eroom
  */
class ChatRoom extends Actor with ActorLogging {
  import ChatSystem._

  log.info(s"My ref is {}", self.path)
  private val subscribers = mutable.Set.empty[ActorRef]

  override def receive = {
    // Note that this is not a great example. Delivery of remote terminated messages are not guaranteed.
    case Terminated(ref) =>
      log.info("subscriber {} dropped", ref)
      subscribers -= ref

    case s @ Subscribe(_, ref) =>
      log.info("subscribe {} joined", ref)
      if (!subscribers.contains(ref)) {
        subscribers += ref
        context.watch(ref)
      }

    case p @ Publish(_, msg) =>
      log.info("publishing msg {}", msg)
      subscribers.foreach(_ ! msg)
  }
}

/**
  * Define the logic for our actor sharding
  * IE: How do we figure out an entity and shard id given a potential target message?
  */
object ChatSystem {
  case class Subscribe(room: String, ref: ActorRef)
  case class Publish(room: String, text: String)

  private val nShards = 100

  private def extractEntityId: ShardRegion.ExtractEntityId = {
    case s@ChatSystem.Subscribe(room, ref) =>
      // http://stackoverflow.com/questions/785091/consistency-of-hashcode-on-a-java-string
      (room, s)

    case p@ChatSystem.Publish(room, msg) =>
      (room, p)
  }

  private def roomToShardId(room: String) =
    (Math.abs(room.hashCode % nShards)).toString

  private def extractShardId: ShardRegion.ExtractShardId = {
    case ChatSystem.Subscribe(room, _) =>
      roomToShardId(room)
    case ChatSystem.Publish(room, _) =>
      roomToShardId(room)
  }

  /**
    * Starts our shard region for this node in the akka cluster
    *
    * For each extracted entityId, akka will launch exactly one instance of said actor within the cluster
    *
    * @return Shard region gateway
    */
  def start()(implicit system: ActorSystem): ActorRef = {
    ClusterSharding(system).start(
      typeName = "ChatRoom",
      entityProps = Props[ChatRoom], // the recipe to make a chat room!
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)
  }
}
