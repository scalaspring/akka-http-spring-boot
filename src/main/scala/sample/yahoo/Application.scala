package sample.yahoo

import java.time.Period

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.{MediaTypes, ContentTypes, ContentType, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import com.github.scalaspring.akka.http.{AkkaHttpServerAutoConfiguration, AkkaHttpService}
import com.github.tototoshi.csv.CSVWriter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

import scala.concurrent.Future
import scala.util.{Success, Failure}


trait BollingerQuoteService extends AkkaHttpService {

  @Autowired val quoteService: QuoteService = null

  def getQuotes(symbol: String): Future[Source[ByteString, Unit]] = {
    val dataFuture: Future[Source[Quote, Unit]] = quoteService.history(symbol, Period.ofMonths(1))

//    dataFuture.flatMap { source =>
//      source
//    }
    //Flow[ByteString].

    dataFuture.map(_.map(q => ByteString(q.toString)))
  }

  override val route: Route = {
    get {
      pathPrefix("quote") {
        path(Segment) { symbol =>
          complete {
            getQuotes(symbol).map(HttpEntity.Chunked.fromData(ContentType(MediaTypes.`text/csv`), _))
          }
        }
      }
    }
  }

}


@SpringBootApplication
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends BollingerQuoteService {}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
