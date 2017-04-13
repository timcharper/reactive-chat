package main

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.OverflowStrategy
import com.typesafe.config.Config
import akka.stream.scaladsl._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class HttpServer(chatSystem: ActorRef, config: Config)(implicit actorSystem: ActorSystem) {
  import MarshallingHelpers._
  implicit val m = ActorMaterializer()

  val route: Route =
    path("room" / Segment) { room =>
      // curl -X PUT localhost:8080/room/5 --data "hello"
      put {
        entity(as[String]) { msg =>
          chatSystem ! ChatSystem.Publish(room, msg)
          complete("ok")
        }
      } ~
      // Subscribe to messages
      // curl localhost:8080/room/5
      get {
        implicit val stringWriter = toPlainTextStream[String]
        complete {
          // Explanation:
          // Source.actorRef[String] provides a stream source blueprint that instantiates an actor when the stream is
          // materialized
          // Every String this newly materialized actor receives is ingested into the materialized source
          Source.actorRef[String](10, OverflowStrategy.dropTail).
            mapMaterializedValue { ref =>
              // This function is called on materialization; we inject a side-effect and send our reference to the chat
              // system so the room can start sending us messages
              chatSystem ! ChatSystem.Subscribe(room, ref)
              ref
            }
        }
      }

    } ~
  // Health check route
  path("health") {
    complete("dokie dokie")
  }


  def run() = {
    Http().bindAndHandle(
      Route.handlerFlow(route),
      config.getString("server.address"),
      config.getInt("server.port"))
  }
}
