package sample.yahoo

import java.io.{IOException, StringReader}
import java.net.URL
import java.time.{LocalDate, Period}

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Sink, Source}
import com.github.scalaspring.akka.http.AkkaHttpClient
import com.github.tototoshi.csv.CSVReader
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import scala.concurrent.Future

trait QuoteService {
  def history(symbol: String, period: Period): Future[Source[Quote, Unit]]
  def history(symbol: String, begin: LocalDate, end: LocalDate): Future[Source[Quote, Unit]]
}

@Component
class YahooQuoteService extends AkkaHttpClient with QuoteService with StrictLogging {

  @Value("${services.yahoo.finance.url:http://real-chart.finance.yahoo.com/table.csv}")
  val url: URL = null

  protected lazy val connectionFlow = Http().outgoingConnection(url.getHost)

  protected def request(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(connectionFlow).runWith(Sink.head)

  protected def query(symbol: String, begin: LocalDate, end: LocalDate) =
    Map[String, String](
      "s" -> symbol, "g" -> "d",
      "a" -> (begin.getMonthValue - 1).toString, "b" -> begin.getDayOfMonth.toString, "c" -> begin.getYear.toString,
      "d" -> (end.getMonthValue - 1).toString, "e" -> end.getDayOfMonth.toString, "f" -> end.getYear.toString
    )

  override def history(symbol: String, period: Period): Future[Source[Quote, Unit]] = history(symbol, LocalDate.now.minus(period), LocalDate.now)

  override def history(symbol: String, begin: LocalDate, end: LocalDate): Future[Source[Quote, Unit]] = {
    val uri = Uri(url.getPath).withQuery(query(symbol, begin, end))

    logger.info(s"Sending request for $uri")

    request(RequestBuilding.Get(uri)).flatMap(response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[String].map(s => Source(() => CSVReader.open(new StringReader(s)).iteratorWithHeaders))
        case NotFound => Future.failed(new IllegalArgumentException(s"Bad symbol or invalid date range (symbol: $symbol, begin: $begin, end: $end, uri: $uri"))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request to $uri failed with status code ${response.status}"
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    )
  }

}
