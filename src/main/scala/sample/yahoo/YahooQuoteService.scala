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

  val logLevels = OperationAttributes.logLevels(onElement = DebugLevel)

  protected lazy val parseResponseFlow = Flow() { implicit b =>
    var i = 0
    def extractRecord(record: Array[String]): String = {
      i = i + 1
      s"$i: ${record(0)}"
    }
    var j = 0
    def extractMerged(merged: (collection.Map[String, String])): String = {
      j = j + 1
      s"$j: ${merged.values.head}"
    }
    val parse = b.add(Flow[ByteString].transform[String](() => ParseRecord()).map(_.split(',')).log("parse", extractRecord).withAttributes(logLevels))
    val zipHeader = b.add(Flow[Array[String]].prefixAndTail(1).map(pt => pt._2.map((pt._1.head, _))).flatten(FlattenStrategy.concat))
    val convert = b.add(Flow[(Array[String], Array[String])].map(t => t._1.zip(t._2).foldLeft(mutable.LinkedHashMap[String, String]())((m, p) => m += p).asInstanceOf[Quote]).log("convert", extractMerged).withAttributes(logLevels))

//    val bcast = b.add(Broadcast[Array[String]](2))
//    val header = b.add(Flow[Array[String]].take(1).expand[Array[String], Array[String]](identity)(s => (s, s))/*.buffer(1, OverflowStrategy.backpressure)*/)
//    val rows = b.add(Flow[Array[String]].drop(1))
//    val zip = b.add(Zip[Array[String], Array[String]])
//    val print = b.add(Sink.fold[Int, Array[String]](0){ (i, r) => logger.info(s"parsed record $i: ${r.mkString(",")}"); i + 1 })
//    val merge = b.add(Flow[(Array[String], Array[String])].map(t => t._1.zip(t._2).foldLeft(mutable.LinkedHashMap[String, String]())((m, p) => m += p ).asInstanceOf[Quote]).log("merge", extractMerged).withAttributes(logLevels))

//    parse ~> bcast ~> header ~> zip.in0
//               bcast ~> rows   ~> zip.in1
//                                  zip.out ~> merge
  //             bcast ~> print

    parse ~> zipHeader ~> convert

    (parse.inlet, convert.outlet)
  }

  protected lazy val connectionFlow = Http().outgoingConnection(url.getHost)

  protected def request(request: HttpRequest): Future[HttpResponse] = Source.single(request).via(connectionFlow).runWith(Sink.head)

  protected def params(symbol: String, begin: LocalDate, end: LocalDate) =
    Map[String, String](
      "s" -> symbol, "g" -> "d",
      "a" -> (begin.getMonthValue - 1).toString, "b" -> begin.getDayOfMonth.toString, "c" -> begin.getYear.toString,
      "d" -> (end.getMonthValue - 1).toString, "e" -> end.getDayOfMonth.toString, "f" -> end.getYear.toString
    )

  override def history(symbol: String, period: Period): Future[Source[Quote, _]] = history(symbol, LocalDate.now.minus(period), LocalDate.now)

  override def history(symbol: String, begin: LocalDate, end: LocalDate): Future[Source[Quote, _]] = {
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
