package com.github.scalaspring.akka.http

import java.net.ServerSocket

import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.context.annotation.{Bean, Import}
import org.springframework.test.context.ContextConfiguration
import resource._

import scala.concurrent.duration._

@ContextConfiguration(
  loader = classOf[SpringApplicationContextLoader],
  classes = Array(classOf[SingleServiceSpec.Configuration])
)
class SingleServiceSpec extends FlatSpec with TestContextManagement with AkkaStreamsAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  implicit val patience = PatienceConfig(10.seconds)    // Allow time for server startup

  @Autowired val settings: ServerSettings = null
  @Autowired val client: HttpClient = null

  "Echo service" should "echo" in {
    val name = "name"
    val future = client.request(Get(s"http://${settings.interface}:${settings.port}/single/echo/$name"))

    whenReady(future) { response =>
      //logger.info(s"""received response "$response"""")
      response.status shouldBe OK
      whenReady(Unmarshal(response.entity).to[String])(_ shouldBe name)
    }
  }

}


object SingleServiceSpec {

  @Configuration
  @Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
  class Configuration extends AkkaHttpServer with EchoService {
    @Bean
    def serverSettings = new ServerSettings(port = managed(new ServerSocket(0)).map(_.getLocalPort).opt.get)
  }

  trait EchoService extends AkkaHttpService {
    abstract override def route: Route = {
      get {
        path("single"/ "echo" / Segment) { name =>
          complete(name)
        }
      }
    } ~ super.route
  }

}
