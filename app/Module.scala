import javax.inject.{ Inject, Provider, Singleton }

import actors._
import akka.actor.typed.{ ActorRef, Behavior }
import akka.stream.Materializer
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import scala.concurrent.ExecutionContext

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindTypedActor(ManagerActor(), "managerActor")
  }
}
