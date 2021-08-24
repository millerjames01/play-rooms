package actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop, Scheduler}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.NotUsed
import play.api.libs.json.JsValue

object ChatRoomActor {
  sealed trait Command

  type WSFlow = Flow[JsValue, JsValue, NotUsed]
  case class GetChatFlow(replyTo: ActorRef[WSFlow]) extends Command
  private case class ListingResponse(listing: Receptionist.Listing) extends Command

  def ChatRoomKey(id: String): ServiceKey[Command] = ServiceKey(id)

  def apply(id: String)(implicit materializer: Materializer): Behavior[Command] = Behaviors.setup { context =>
    context.system.receptionist ! Receptionist.Register(ChatRoomKey(id), context.self)

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

    Behaviors.receiveMessage { message =>
      message match {
        case GetChatFlow(replyTo) =>
          println("Returning the chat flow")
          replyTo ! chatFlow
          Behaviors.same
      }
    }
  }
}
