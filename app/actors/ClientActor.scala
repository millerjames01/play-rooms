package actors

import actors.RoomActor.Leave
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import play.api.libs.json.JsValue
import util.WebsocketResponse.{jsError, jsMessage}
import util.UserAlias

object ClientActor {
  sealed trait Command

  case class Initialize(output: ActorRef[JsValue]) extends Command
  case class IncomingMessage(js: JsValue) extends Command

  sealed trait WebsocketMessage extends Command
  case object CreateRoom extends WebsocketMessage
  case class ConnectToRoom(roomId: String) extends WebsocketMessage
  case object DisconnectFromRoom extends WebsocketMessage
  case class ChatMessage(message: JsValue) extends WebsocketMessage

  sealed trait ConciergeResponse extends Command
  case class RoomAssignment(roomId: String, roomRef: ActorRef[RoomActor.Command]) extends ConciergeResponse
  case object RoomNotFound extends ConciergeResponse

  def apply(userAlias: UserAlias)(implicit concierge: ActorRef[ConciergeActor.Command]): Behavior[Command] = uninitialized(userAlias)(concierge)

  def uninitialized(userAlias: UserAlias)(implicit concierge: ActorRef[ConciergeActor.Command]): Behavior[Command] = Behaviors.receiveMessage {
    case Initialize(output) => standby(userAlias, output)
    case _ =>
      println("Received message in uninitialized room.")
      Behaviors.same
  }

  def standby(userAlias: UserAlias, output: ActorRef[JsValue])
             (implicit concierge: ActorRef[ConciergeActor.Command]): Behavior[Command] =
    Behaviors.receive { case (context, message) => message match {
      case CreateRoom =>
        concierge ! ConciergeActor.CreateRoom(context.self)
        Behaviors.same

      case ConnectToRoom(roomId) =>
        concierge ! ConciergeActor.FindRoom(roomId, context.self)
        Behaviors.same

      case RoomAssignment(roomId, roomRef) =>
        roomRef ! RoomActor.Join(userAlias, context.self)
        output ! jsMessage("room-joined", roomId)
        connected(userAlias, output, roomRef)

      case RoomNotFound =>
        output ! jsError("Room Not Found: We couldn't find the room you were looking for.")
        Behaviors.same

      case DisconnectFromRoom =>
        output ! jsError("Disconnect Error: You are not connected to a room.")
        Behaviors.same

      case ChatMessage(_) =>
        output ! jsError("Send Message Error: You are not connected to a room.")
        Behaviors.same

      case IncomingMessage(_) => Behaviors.same

      case Initialize(_) => Behaviors.same
    }
    }


  def connected(userAlias: UserAlias, output: ActorRef[JsValue], room: ActorRef[RoomActor.Command])
            (implicit concierge: ActorRef[ConciergeActor.Command]): Behavior[Command] =  Behaviors.receiveMessage {
      case CreateRoom =>
        output ! jsError("Create Room Error: You are already connected to a room.")
        Behaviors.same

      case ConnectToRoom(_) =>
        output ! jsError("Connect to Room Error: You are already connected to a room.")
        Behaviors.same

      case _: ConciergeResponse => Behaviors.same

      case DisconnectFromRoom =>
        room ! RoomActor.Leave(userAlias)
        standby(userAlias, output)

      case ChatMessage(js: JsValue) =>
        room ! RoomActor.PostMessage(js)
        Behaviors.same

      case IncomingMessage(ms: JsValue) =>
        output ! ms
        Behaviors.same

      case Initialize(_) => Behaviors.same
  }
}
