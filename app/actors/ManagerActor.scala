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

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try
import scala.io.Source
import scala.util.Random

import actors.ChatRoomActor


object ManagerActor {
  sealed trait Command

  type WSFlow = Flow[JsValue, JsValue, NotUsed]
  case object CreateChatRoom extends Command
  case class GetChatFlow(id: String, replyTo: ActorRef[WSFlow]) extends Command
  private case class ListingResponse(listing: Receptionist.Listing, id: String, replyTo: ActorRef[WSFlow]) extends Command


  def apply(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      def listingAdapter(id: String, replyTo: ActorRef[WSFlow]): ActorRef[Receptionist.Listing] =
        context.messageAdapter { listing =>
          println(s"listingAdapter:listing: ${listing.toString}")
          ListingResponse(listing, id, replyTo)
        }

      message match {
        case CreateChatRoom => {
          val newId = generateRoomKey
          context.spawn(ChatRoomActor(newId), "Room" + newId)
        }
        case GetChatFlow(id, replyTo) => {
          context.system.receptionist !
            Receptionist.Find(ChatRoomActor.ChatRoomKey(id), listingAdapter(id, replyTo))
        }
        case ListingResponse(listing, id, replyTo) => {
          val CRKey = ChatRoomActor.ChatRoomKey(id)
          listing.serviceInstances(CRKey) foreach { ref: ActorRef[ChatRoomActor.Command] =>
            ref ! ChatRoomActor.GetChatFlow(replyTo)
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
    val nounsSource = Source.fromFile("app/assets/nouns.txt")
    val list = nounsSource.getLines().toIndexedSeq
    nounsSource.close
    list
  }
}
