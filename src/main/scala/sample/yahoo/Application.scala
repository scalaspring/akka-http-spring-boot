package sample.yahoo

import java.time.Period

import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl._
import com.github.scalaspring.akka.http.{AkkaHttpServerAutoConfiguration, AkkaHttpService}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import sample.yahoo.Flows._

import scala.concurrent.Future


trait BollingerQuoteService extends AkkaHttpService with StrictLogging {

  val window = 14

  @Autowired val quoteService: QuoteService = null


  def getQuotes(symbol: String, period: Period): Future[Option[Source[Quote, _]]] =
    quoteService.history(symbol, period).map(_.map(_.via(bollinger(window))))


  override val route: Route = {
    get {
      path("quote" / Segment) { symbol =>
        parameters('months.as[Int] ? 6) { months =>
          complete {
            val period = Period.ofMonths(months)
            getQuotes(symbol, period).map[ToResponseMarshallable] {
              case Some(s) => HttpEntity.Chunked.fromData(ContentTypes.`text/plain`, s.via(csv).via(chunked()))
              case None => NotFound -> s"No data found for the given symbol '$symbol' or period $period"
            }
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
