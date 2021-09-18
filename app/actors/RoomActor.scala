package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import play.api.libs.json.JsValue
import util.UserAlias
import util.WebsocketResponse.{ jsMessage, userJoinMessage }

object RoomActor {
  sealed trait Command

  case class Join(userAlias: UserAlias, replyTo: ActorRef[ClientActor.Command]) extends Command
  case class PostMessage(message: JsValue) extends Command
  case class Leave(userAlias: UserAlias) extends Command
  case object Close extends Command

  def apply(id: String, clients: Map[UserAlias, ActorRef[ClientActor.Command]])
           (implicit concierge: ActorRef[ConciergeActor.Command]): Behavior[Command] =
    Behaviors.receiveMessage {
      case Join(userAlias, replyTo) =>
        val joinMessage = userJoinMessage(userAlias)
        replyTo ! ClientActor.IncomingMessage(joinMessage)
        clients.values foreach (_ ! ClientActor.IncomingMessage(joinMessage))
        this(id, clients + ((userAlias, replyTo)))

      case PostMessage(message) =>
        clients.values foreach (_ ! ClientActor.IncomingMessage(message))
        Behaviors.same

      case Leave(userAlias) =>
        val newClients = clients removed userAlias
        val leaveMessage = jsMessage("client-left", userAlias.clientId + "has left the room")
        newClients.values foreach (_ ! ClientActor.IncomingMessage(leaveMessage))
        if (newClients.isEmpty) concierge ! ConciergeActor.CloseRoom(id)
        this(id, newClients)

      case Close => Behaviors.stopped
    }
}
