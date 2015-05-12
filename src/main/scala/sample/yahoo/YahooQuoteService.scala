package sample.yahoo

import java.io.{IOException, StringReader}
import java.net.URL
import java.time.{LocalDate, Period}

import akka.event.Logging.LogLevel
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.OperationAttributes.LogLevels
import akka.stream.{OperationAttributes, OverflowStrategy}
import akka.stream.impl.fusing.Take
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.scalaspring.akka.http.AkkaHttpClient
import com.github.tototoshi.csv.CSVReader
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import sample.flow.ParseRecord

import scala.collection.mutable
import scala.concurrent.Future
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.event.Logging._

trait QuoteService {
  /**
   * Returns daily historical quotes for a specified trailing period, e.g. the past month.
   */
  def history(symbol: String, period: Period): Future[Source[Quote, _]]

  /**
   * Returns daily historical quotes for a specified date range.
   */
  def history(symbol: String, begin: LocalDate, end: LocalDate): Future[Source[Quote, _]]
}

@Component
class YahooQuoteService extends AkkaHttpClient with QuoteService with StrictLogging {

  @Value("${services.yahoo.finance.url:http://real-chart.finance.yahoo.com/table.csv}")
  val url: URL = null

  protected lazy val connectionFlow = Http().outgoingConnection(url.getHost)

  protected def request(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(connectionFlow).runWith(Sink.head)

  protected def params(symbol: String, begin: LocalDate, end: LocalDate) =
    Map[String, String](
      "s" -> symbol, "g" -> "d",
      "a" -> (begin.getMonthValue - 1).toString, "b" -> begin.getDayOfMonth.toString, "c" -> begin.getYear.toString,
      "d" -> (end.getMonthValue - 1).toString, "e" -> end.getDayOfMonth.toString, "f" -> end.getYear.toString
    )

  // Converts a ByteString stream into a Quote stream
  protected lazy val parseResponseFlow = Flow() { implicit b =>
    val parse = b.add(Flow[ByteString].transform[String](() => ParseRecord()).map(_.split(',')))
    val zipHeader = b.add(Flow[Array[String]].prefixAndTail(1).map(pt => pt._2.map((pt._1.head, _))).flatten(FlattenStrategy.concat))
    val convert = b.add(Flow[(Array[String], Array[String])].map(t => t._1.zip(t._2).foldLeft(Quote())((q, t) => q += t)))

    parse ~> zipHeader ~> convert

    (parse.inlet, convert.outlet)
  }

  override def history(symbol: String, period: Period): Future[Source[Quote, _]] = history(symbol, LocalDate.now.minus(period), LocalDate.now)

  override def history(symbol: String, begin: LocalDate, end: LocalDate): Future[Source[Quote, _]] = {
    require(end.isAfter(begin) || end.isEqual(begin), "invalid date range - end date must be on or after begin date")

    val uri = Uri(url.getPath).withQuery(params(symbol, begin, end))

    logger.info(s"Sending request for $uri")

    request(RequestBuilding.Get(uri)).flatMap { response =>
      logger.info(s"Received response with status ${response.status} from $uri")
      response.status match {
        case OK => Future.successful(response.entity.dataBytes.via(parseResponseFlow))
        case NotFound => Future.failed(new IllegalArgumentException(s"Bad symbol or invalid date range (symbol: $symbol, begin: $begin, end: $end, uri: $uri"))
        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
          val error = s"Request to $uri failed with status code ${response.status}"
          logger.error(error)
          Future.failed(new IOException(error))
        }
      }
    }
  }

}
