package com.github.scalaspring.akka.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.AskSupport
import akka.stream.scaladsl.{Sink, Source}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

import scala.concurrent.duration._

@ContextConfiguration(
  loader = classOf[SpringApplicationContextLoader],
  classes = Array(classOf[AkkaHttpServerAutoConfigurationSpec.Configuration])
)
class AkkaHttpServerAutoConfigurationSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with AskSupport with ScalaFutures with StrictLogging {

  implicit val patience = PatienceConfig((10 seconds))

  @Value("${http.server.port}")
  val httpServerPort: Int = -1


  "Echo service" should "respond" in {
    val name = "name"
    val connectionFlow = Http().outgoingConnection("localhost", httpServerPort)
    val future = Source.single(Get(s"/echo/$name")).via(connectionFlow).runWith(Sink.head)


    whenReady(future) { response =>
      logger.info(s"""received response "$response"""")

      response.status shouldBe OK

      whenReady(Unmarshal(response.entity).to[String])(_ shouldBe name)
    }
  }

}


object AkkaHttpServerAutoConfigurationSpec {

  trait EchoService extends AkkaHttpService with StrictLogging {
    override val route: Route = {
      get {
        path("echo" / Segment) { name =>
          complete(name)
        }
      }
    }
  }

  @Configuration
  @Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
  class Configuration extends EchoService

}
