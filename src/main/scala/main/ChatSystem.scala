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

class ChatRoom extends Actor with ActorLogging {
  import ChatSystem._

  log.info(s"My ref is {}", self.path)
  private val subscribers = mutable.Set.empty[ActorRef]

  override def receive = {
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
      // Thread.sleep(250)
      log.info("publishing msg {}", msg)
      subscribers.foreach(_ ! msg)
  }
}

class ChatSupervisor extends Actor with ActorLogging {
  val chatRoom = context.actorOf(Props[ChatRoom], "theRoom")

  log.info("hey there! {} is up", context.self.path)

  override val supervisorStrategy = OneForOneStrategy() {
    case _: IllegalArgumentException     => SupervisorStrategy.Resume
    case _: ActorInitializationException => SupervisorStrategy.Stop
    case _: DeathPactException           => SupervisorStrategy.Stop
    case _: Exception                    => SupervisorStrategy.Restart
  }

  def receive = {
    case msg => {
      chatRoom forward msg
    }

  }
}

object ChatSystem {
  case class Subscribe(room: String, ref: ActorRef)
  case class Publish(room: String, text: String)

  val nShards = 100

  def extractEntityId: ShardRegion.ExtractEntityId = {
    case s@ChatSystem.Subscribe(room, ref) =>
      // http://stackoverflow.com/questions/785091/consistency-of-hashcode-on-a-java-string
      (room.toString, s)

    case p@ChatSystem.Publish(room, msg) =>
      (room, p)
  }

  @inline def roomToShardId(room: String) =
    (Math.abs(room.hashCode % nShards)).toString

  def extractShardId: ShardRegion.ExtractShardId = {
    case ChatSystem.Subscribe(room, _) =>
      roomToShardId(room)
    case ChatSystem.Publish(room, _) =>
      roomToShardId(room)
  }

  def start()(implicit system: ActorSystem): ActorRef = {
    ClusterSharding(system).start(
      typeName = "SupervisedChatRoom",
      entityProps = Props[ChatSupervisor],
      settings = ClusterShardingSettings(system),
      // rememberEntities = true,
      extractEntityId = extractEntityId,
      extractShardId = extractShardId)
  }
}
