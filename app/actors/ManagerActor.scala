package actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.receptionist.Receptionist
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.NotUsed
import play.api.libs.json.JsValue
import play.api.libs.concurrent.ActorModule
import com.google.inject.Provides
import util.KeyGenerator

object ManagerActor extends ActorModule {
  sealed trait Command

  type WSFlow = Flow[JsValue, JsValue, NotUsed]
  case class CreateChatRoom(replyTo: ActorRef[String]) extends Command
  case class FlowQuery(id: String, replyTo: ActorRef[WSFlow]) extends Command
  private case class ListingResponse(listing: Receptionist.Listing, flowQ: FlowQuery) extends Command


  @Provides def apply(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      def listingAdapter(flowQ: FlowQuery): ActorRef[Receptionist.Listing] =
        context.messageAdapter { listing =>
          println(s"listingAdapter:listing: ${listing.toString}")
          ListingResponse(listing, flowQ)
        }

      message match {
        case CreateChatRoom(replyTo) =>
          implicit val mat: Materializer = Materializer.apply(context)
          val newId = KeyGenerator.generateRoomKey
          context.spawn(ChatRoomActor(newId)(mat), "Room" + newId)
          replyTo ! newId
          println("New Chatroom created @ Room" + newId)
        case fq: FlowQuery =>
          context.system.receptionist !
            Receptionist.Find(ChatRoomActor.ChatRoomKey(fq.id), listingAdapter(fq))
          println("Chatroom query found for " + "Room" + fq.id)
        case ListingResponse(listing, flowQ) =>
          val CRKey = ChatRoomActor.ChatRoomKey(flowQ.id)
          listing.serviceInstances(CRKey) foreach { ref: ActorRef[ChatRoomActor.Command] =>
            println("Chatroom located @ Room" + flowQ.id)
            ref ! ChatRoomActor.GetChatFlow(flowQ.replyTo)
          }
      }

      Behaviors.same
    }
  }


}
