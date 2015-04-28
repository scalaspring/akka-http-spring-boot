package sample.yahoo

import java.io.IOException
import java.time.LocalDate

import com.github.scalaspring.akka.http.AkkaStreamsAutoConfiguration
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.context.annotation.{ComponentScan, Configuration, Import}
import org.springframework.test.context.ContextConfiguration

import scala.concurrent.duration._

@Configuration
@ComponentScan
@ContextConfiguration(
  loader = classOf[SpringApplicationContextLoader],
  classes = Array(classOf[QuoteServiceSpec])
)
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class QuoteServiceSpec extends FlatSpec with TestContextManagement with Matchers with EitherValues with ScalaFutures with StrictLogging {

  // Yahoo takes a few seconds to respond
  implicit val patience = PatienceConfig((10 seconds))

  @Autowired val quotes: QuoteService = null


  "Quote service" should "return data" in {
    val getFuture = quotes.historical("YHOO", LocalDate.now().minusWeeks(8))

    whenReady(getFuture) { response =>
      response.right.value.isEmpty shouldBe false
      logger.info(s"data:\n${response.right.value.mkString("\n")}")
    }
  }

  "Quote service" should "fail on bad symbol" in {
    val getFuture = quotes.historical("BLAH", LocalDate.now().minusWeeks(8))

    whenReady(getFuture) { response =>
      response.left.value.isEmpty shouldBe false
      logger.info(s"error: ${response.left.value}")
    }
  }

}
