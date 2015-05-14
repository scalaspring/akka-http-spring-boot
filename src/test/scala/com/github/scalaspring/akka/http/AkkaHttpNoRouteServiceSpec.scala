package com.github.scalaspring.akka.http

import akka.pattern.AskSupport
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Ignore, FlatSpec, Matchers}
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration

import scala.concurrent.duration._

@ContextConfiguration(
  loader = classOf[SpringApplicationContextLoader],
  classes = Array(classOf[AkkaHttpNoRouteServiceSpec.Configuration])
)
@Ignore
class AkkaHttpNoRouteServiceSpec extends FlatSpec with TestContextManagement with AkkaHttpClient with Matchers with AskSupport with ScalaFutures with StrictLogging {

  implicit val patience = PatienceConfig((10 seconds))    // Allow time for server startup

  "Context startup" should "fail" in {
    fail(s"The application context should have failed to start due to no route defined in ${classOf[AkkaHttpNoRouteServiceSpec.NoRouteService].getSimpleName}")
  }

}

object AkkaHttpNoRouteServiceSpec {

  trait NoRouteService extends AkkaHttpService

  @Configuration
  @Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
  class Configuration extends NoRouteService

}
