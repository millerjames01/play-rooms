package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.NotUsed
import play.api.libs.json.JsValue

object ChatRoomActor {
  sealed trait Command

  type WSFlow = Flow[JsValue, JsValue, NotUsed]
  case class GetChatFlow(replyTo: ActorRef[WSFlow]) extends Command

  def apply(id: String, clients: List[String])(implicit materializer: Materializer): Behavior[Command] = Behaviors.setup { context =>

    val (chatSink, chatSource) = {
      val source = MergeHub.source[JsValue]
        .log("source")
        .recoverWithRetries(-1, { case _: Exception => Source.empty })

      val sink = BroadcastHub.sink[JsValue]
      source.toMat(sink)(Keep.both).run()
    }

    val chatFlow: WSFlow = {
      Flow[JsValue].via(Flow.fromSinkAndSource(chatSink, chatSource)).log(id + "Flow")
    }

    Behaviors.receiveMessage {
      case GetChatFlow(replyTo) =>
        replyTo ! chatFlow
        Behaviors.same
    }
  }
}
