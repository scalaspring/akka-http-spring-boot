package com.github.scalaspring.akka.http

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.scalaspring.akka.http.RouteTestSpec.EchoService
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}


class RouteTestSpec extends FlatSpec with EchoService with ScalatestRouteTest with Matchers with ScalaFutures with StrictLogging {

  "Echo service" should "echo" in {
    Get(s"/single/echo/hello") ~> route ~> check {
      status shouldBe OK
    }
  }

}


object RouteTestSpec {

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
