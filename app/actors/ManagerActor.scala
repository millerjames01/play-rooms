package actors

import actors.ChatRoomActor.WSFlow
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.Receptionist
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Source}
import akka.NotUsed
import play.api.libs.json.JsValue
import play.api.libs.concurrent.ActorModule
import com.google.inject.Provides
import util.KeyGenerator

object ManagerActor extends ActorModule {
  sealed trait Command

  type WSFlow = Flow[JsValue, JsValue, NotUsed]
  type Room = (WSFlow, List[String])
  case class CreateChatRoom(replyTo: ActorRef[String]) extends Command
  case class FlowQuery(id: String, replyTo: ActorRef[WSFlow]) extends Command
  case class Leave(roomId: String, clientId: String) extends Command
  case class CreateRoom(replyTo: ActorRef[String]) extends Command
  case class JoinRoom(id: String, clientId: String, replyTo: ActorRef[WSFlow]) extends Command
  case class LeaveRoom(id: String, clientId: String) extends Command

  @Provides def apply(map: Map[String, Room]): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case CreateChatRoom(replyTo) =>
          implicit val mat: Materializer = Materializer.apply(context)
          val newId = KeyGenerator.generateRoomKey

          val (chatSink, chatSource) = {
            val source = MergeHub.source[JsValue]
              .log("source")
              .recoverWithRetries(-1, { case _: Exception => Source.empty })

            val sink = BroadcastHub.sink[JsValue]
            source.toMat(sink)(Keep.both).run()
          }

          val chatFlow: WSFlow = {
            Flow[JsValue].via(Flow.fromSinkAndSource(chatSink, chatSource)).log(newId + "Flow")
          }

          val chatRoom: Room = (chatFlow, List())
          replyTo ! newId
          val newMap = map + ((newId, chatRoom))
          printRoomMap(newMap)
          this(newMap)
        case FlowQuery(id, replyTo) =>
          map.get(id) match {
            case Some(room) => replyTo ! room._1
            case None => {}
          }
          println("Chatroom query found for " + "Room" + id)
          Behaviors.same
        case JoinRoom(id, clientId, replyTo) =>
          map.get(id) match {
            case Some(room) =>
              replyTo ! room._1
              val newRoom: Room = (room._1, clientId :: room._2)
              printRoomMap(map.updated(id, newRoom))
              this(map.updated(id, newRoom))
            case None => Behaviors.same
          }
        case Leave(roomId, clientId) =>
          map.get(roomId) match {
            case Some(room) =>
              val newRoom: Room = (room._1, room._2.filterNot(_ == clientId))
              val newMap =
                if(newRoom._2 == Nil) map.removed(roomId)
                else map.updated(roomId, newRoom)
              printRoomMap(newMap)
              this(newMap)
            case None => Behaviors.same
          }
      }
    }
  }

  private def printRoomMap(map: Map[String, Room]) = {
    println("Current Rooms")
    println("=============")
    map foreach {
      case (s, room) => println(s + ": " + room._2.mkString(", "))
    }
    println()
  }
}
