package controllers

import javax.inject._
import play.api.mvc._
import actors.{ClientActor, ConciergeActor}
import akka.util.Timeout
import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.typed.scaladsl._
import akka.stream.Materializer
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.Configuration
import util.{AliasGenerator, ColorGenerator, UserAlias}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents)
                              (implicit concierge: ActorRef[ConciergeActor.Command],
                               materializer: Materializer,
                               executionContext: ExecutionContext,
                               scheduler: Scheduler,
                               config: Configuration)
  extends BaseController {

  private val logger = play.api.Logger(getClass)

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val randomName = AliasGenerator.generate
    val randomColor = ColorGenerator.generate
    Ok(views.html.index()).withSession(
      "alias" -> randomName,
      "color" -> randomColor
    )
  }

  def connect(clientId: String, color: String): WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { implicit rh: RequestHeader =>
    implicit val timeout = Timeout(2.seconds)

    val clientRef = concierge.ask(replyTo => ConciergeActor.CreateClient(UserAlias(clientId, color), replyTo))

    val futureFlow = clientRef.map { ref =>
      ActorFlow.ask(ref) { (jsValue: JsValue, replyTo: ActorRef[JsValue]) =>
        ref ! ClientActor.Initialize(replyTo)
        val map = jsValue.as[JsObject].value
        val typeString = map.get("type") match {
          case Some(typeValue) => typeValue.as[JsString].value
          case None => throw new Exception("Badly formatted message")
        }
        typeString match {
          case "create" => ClientActor.CreateRoom
          case "join" => {
            val roomId = map.get("room").get.as[JsString]
            ClientActor.ConnectToRoom(roomId.value)
          }
          case "message" => ClientActor.ChatMessage(jsValue)
          case "leave" => ClientActor.DisconnectFromRoom
        }
      }
    }

    futureFlow.map { flow =>
      Right(flow)
    }.recover { case e: Exception =>
      val jsError = Json.obj("error" -> "Cannot create websocket")
      val result = InternalServerError(jsError)
      Left(result)
    }
  }
}
