package com.github.scalaspring.akka.http

import akka.pattern.AskSupport
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Import
import resource._

import scala.concurrent.duration._

class MissingRouteSpec extends FlatSpec with AkkaStreamsAutowiredImplicits with Matchers with AskSupport with ScalaFutures with StrictLogging {

  implicit val patience = PatienceConfig((10 seconds))    // Allow time for server startup

  "Context startup" should "fail" in {
    a [BeanCreationException] should be thrownBy
      managed(SpringApplication.run(classOf[MissingRouteSpec.Configuration])).map(c => c).opt.get
  }

}

object MissingRouteSpec {

  // Missing route definition
  trait NoRouteService extends AkkaHttpService

  @Configuration
  @Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
  class Configuration extends AkkaHttpServer with NoRouteService

}
