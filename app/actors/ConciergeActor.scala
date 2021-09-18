package actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import play.api.libs.concurrent.ActorModule
import com.google.inject.Provides
import util.{ KeyGenerator, UserAlias }

object ConciergeActor extends ActorModule {
  sealed trait Command

  case class CreateClient(userAlias: UserAlias, replyTo: ActorRef[ActorRef[ClientActor.Command]]) extends Command
  case class CreateRoom(replyTo: ActorRef[ClientActor.Command]) extends Command
  case class FindRoom(id: String, replyTo: ActorRef[ClientActor.Command]) extends Command
  case class CloseRoom(id: String) extends Command

  @Provides def apply(rooms: Map[String, ActorRef[RoomActor.Command]]): Behavior[Command] =
    Behaviors.receive { (context, message) => message match {
      case CreateClient(userAlias, replyTo) => {
        val newClient = context.spawn(ClientActor(userAlias)(context.self), userAlias.clientId + userAlias.color)
        replyTo ! newClient
        Behaviors.same
      }

      case CreateRoom(replyTo: ActorRef[ClientActor.Command]) =>
        val newId = KeyGenerator.generateRoomKey
        val roomRef = context.spawn(RoomActor(newId, Map())(context.self), "Room" + newId)
        replyTo ! ClientActor.RoomAssignment(newId, roomRef)
        val newRooms = rooms + ((newId, roomRef))
        println("Rooms: " + newRooms.keys.mkString(" | "))
        this(newRooms)

      case FindRoom(id, replyTo) => rooms.get(id) match {
        case Some(roomRef) =>
          replyTo ! ClientActor.RoomAssignment(id, roomRef)
          Behaviors.same
        case None =>
          replyTo ! ClientActor.RoomNotFound
          Behaviors.same
      }

      case CloseRoom(id) =>
        println("Rooms: " + (rooms - id).keys.mkString(" | "))
        rooms(id) ! RoomActor.Close
        this(rooms - id)
    }
    }
}
