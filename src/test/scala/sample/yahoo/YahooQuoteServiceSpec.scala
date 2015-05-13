package sample.yahoo

import java.time.{LocalDate, Month, Period}

import akka.stream.scaladsl.Source
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{EitherValues, FlatSpec, Matchers}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.context.annotation.{ComponentScan, Configuration, Import}
import org.springframework.test.context.ContextConfiguration

import scala.concurrent.Future
import scala.concurrent.duration._

@Configuration
@ComponentScan
@ContextConfiguration(
  loader = classOf[SpringApplicationContextLoader],
  classes = Array(classOf[YahooQuoteServiceSpec])
)
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class YahooQuoteServiceSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with EitherValues with ScalaFutures with StrictLogging {

  // Yahoo takes more than a second to respond
  implicit val patience = PatienceConfig((10 seconds))

  @Autowired val quoteService: QuoteService = null


  "Quote service" should "return data" in {
    val getFuture: Future[Option[Source[Quote, _]]] = quoteService.history("YHOO", Period.ofWeeks(8))
    val future: Future[Option[Seq[Quote]]] = getFuture.flatMap(
      _.map(_.runFold(Seq[Quote]())((s, m) => s :+ m)         // Get the sequence of quotes from the Source
        .map(Some(_))).getOrElse(Future.successful(None)))    // Map to a Future[Option[Seq[Quote]]]

    whenReady(future) { quotes =>
      quotes shouldBe defined
      //logger.info(s"data:\n${quotes.map(_.mkString("\n"))}")
    }
  }

  it should "return None for bad symbol" in {
    val future = quoteService.history("BLAH", Period.ofWeeks(8))
    whenReady(future)(_ shouldBe empty)
  }

  it should "return None for bad date range" in {
    // Note: Facebook went public in 2012
    val future = quoteService.history("FB", LocalDate.of(2010, Month.JANUARY, 1), LocalDate.of(2011, Month.JANUARY, 1))
    whenReady(future)(_ shouldBe empty)
  }

}


