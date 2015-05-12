package sample.yahoo

import java.time.Period

import akka.http.scaladsl.model.{ContentType, HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.scalaspring.akka.http.{AkkaHttpServerAutoConfiguration, AkkaHttpService}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import Flows._
import scala.concurrent.Future


trait BollingerQuoteService extends AkkaHttpService with StrictLogging {

  val period = Period.ofMonths(60)
  val window = 14
  val contentType = ContentType(MediaTypes.`text/plain`)
//  val contentType = ContentType(MediaTypes.`text/csv`)

  @Autowired val quoteService: QuoteService = null


  def getQuotes(symbol: String, period: Period): Future[Source[ByteString, _]] = {
    val quoteFuture: Future[Source[Quote, _]] = quoteService.history(symbol, period)
    val csvFuture = quoteFuture.map(_.via(bollinger(window)).via(csv))

    csvFuture.map(_.map(ByteString(_)))
  }

  override val route: Route = {
    get {
      pathPrefix("quote") {
        path(Segment) { symbol =>
          complete {
            getQuotes(symbol, period).map(HttpEntity.Chunked.fromData(contentType, _))
          }
        }
      }
    }
  }

}


@SpringBootApplication
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends BollingerQuoteService { override val window: Int = 20 }

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
