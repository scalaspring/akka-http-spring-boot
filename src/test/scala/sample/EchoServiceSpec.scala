package sample

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.github.scalaspring.akka.http.AkkaStreamsAutoConfiguration
import com.github.scalaspring.scalatest.TestContextManagement
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration

@Configuration
@ContextConfiguration(classes = Array(classOf[EchoServiceSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class EchoServiceSpec extends FlatSpec with TestContextManagement with EchoService with ScalatestRouteTest with Matchers {

  "Echo service" should "echo" in {
    Get(s"/echo/test") ~> route ~> check {
      status shouldBe OK
      responseAs[String] shouldBe "test"
    }
  }

}
