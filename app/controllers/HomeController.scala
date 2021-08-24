package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import actors.ManagerActor
import akka.util.Timeout
import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.Materializer
import play.api.libs.json.{JsValue, Json}
import play.api.Logger

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(managerActor: ActorRef[ManagerActor.Command],
                               val controllerComponents: ControllerComponents)
                              (implicit materializer: Materializer,
                               executionContext: ExecutionContext,
                               scheduler: Scheduler)
  extends BaseController {

  val logger = play.api.Logger(getClass)

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def createRoom() = Action { implicit request: Request[AnyContent] =>
    managerActor ! ManagerActor.CreateChatRoom
    Ok
  }

  def roomWS(id: String) = WebSocket.acceptOrResult[JsValue, JsValue] { implicit rh: RequestHeader =>
    implicit val timeout = Timeout(1.second)
    val futureFlow = managerActor.ask(replyTo => ManagerActor.FlowQuery(id, replyTo))

    futureFlow.map { flow =>
      Right(flow)
    }.recover {
      case e: Exception =>
        logger.error("Cannot create websocket", e)
        val jsError = Json.obj("error" -> "Cannot create websocket")
        val result = InternalServerError(jsError)
        Left(result)
    }
  }
}
