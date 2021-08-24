package actors

import javax.inject._

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, PostStop, Scheduler }
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.stream._
import akka.stream.scaladsl._
import akka.util.Timeout
import akka.{ Done, NotUsed }
import org.slf4j.Logger
import play.api.libs.json._
import play.api.libs.concurrent.ActorModule
import com.google.inject.Provides
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try
import scala.io.Source
import scala.util.Random

import actors.ChatRoomActor


object ManagerActor extends ActorModule {
  sealed trait Command

  type WSFlow = Flow[JsValue, JsValue, NotUsed]
  case object CreateChatRoom extends Command
  case class FlowQuery(id: String, replyTo: ActorRef[WSFlow]) extends Command
  private case class ListingResponse(listing: Receptionist.Listing, flowQ: FlowQuery) extends Command


  @Provides def apply()(implicit materializer: Materializer): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      def listingAdapter(flowQ: FlowQuery): ActorRef[Receptionist.Listing] =
        context.messageAdapter { listing =>
          println(s"listingAdapter:listing: ${listing.toString}")
          ListingResponse(listing, flowQ)
        }

      message match {
        case CreateChatRoom => {
          val newId = generateRoomKey
          context.spawn(ChatRoomActor(newId), "Room" + newId)
          println("New Chatroom created @ Room" + newId)
        }
        case fq: FlowQuery => {
          context.system.receptionist !
            Receptionist.Find(ChatRoomActor.ChatRoomKey(fq.id), listingAdapter(fq))
          println("Chatroom query found for " + "Room" + fq.id)
        }
        case ListingResponse(listing, flowQ) => {
          val CRKey = ChatRoomActor.ChatRoomKey(flowQ.id)
          listing.serviceInstances(CRKey) foreach { ref: ActorRef[ChatRoomActor.Command] =>
            println("Chatroom located @ Room" + flowQ.id)
            ref ! ChatRoomActor.GetChatFlow(flowQ.replyTo)
          }
        }
      }

      Behaviors.same
    }
  }

  private def generateRoomKey: String = {
    val keys = for (_ <- 1 to 3) yield nouns(Random.nextInt(nouns.length)).capitalize
    keys.mkString
  }

  private lazy val nouns = {
    val nounsSource = Source.fromFile("app/resources/nouns.txt")
    val list = nounsSource.getLines().toIndexedSeq
    nounsSource.close
    list
  }
}
