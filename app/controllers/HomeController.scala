package controllers

import javax.inject._
import play.api.mvc._
import actors.ManagerActor
import akka.util.Timeout
import akka.actor.typed.{ActorRef, Scheduler}
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.Materializer
import play.api.libs.json.{JsValue, Json}
import play.api.Configuration
import util.{ AliasGenerator, ColorGenerator }

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(managerActor: ActorRef[ManagerActor.Command],
                               val controllerComponents: ControllerComponents)
                              (implicit materializer: Materializer,
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

  def createRoom(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    implicit val timeout: Timeout = Timeout(1.second)
    val futureId = managerActor.ask(replyTo => ManagerActor.CreateChatRoom(replyTo))
    println("Received create room post")

    futureId.map { id =>
      Ok(Json.obj("id" -> id))
    }.recover {
      case _: Exception =>
        Ok(Json.obj("error" -> "Couldn't create room"))
    }
  }

  def roomWS(id: String, clientName: String): WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] { implicit rh: RequestHeader =>
    implicit val timeout: Timeout = Timeout(1.second)
    val futureFlow = managerActor.ask(replyTo => ManagerActor.JoinRoom(id, clientName, replyTo))

    futureFlow.map { flow =>
      val watchedFlow = flow.watchTermination() { (_, termination) =>
        termination foreach (_ => managerActor ! ManagerActor.Leave(id, clientName))
      }
      Right(watchedFlow)
    }.recover {
      case e: Exception =>
        logger.error("Failed to join the room.", e)
        val jsError = Json.obj("error" -> "Failed to join the room.")
        val result = InternalServerError(jsError)
        Left(result)
    }
  }
}
