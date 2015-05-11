package sample.yahoo

import java.time.Period

import akka.http.scaladsl.model.{ContentType, HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.scalaspring.akka.http.{AkkaHttpServerAutoConfiguration, AkkaHttpService}
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import sample.flow._

import scala.collection._
import scala.collection.immutable
import scala.concurrent.Future


trait BollingerQuoteService extends AkkaHttpService with StrictLogging {

  val window: Int = 14

  @Autowired val quoteService: QuoteService = null

  // Merge Bollinger points into a quote
  def mergeBollinger(q: Quote, b: Bollinger, fmt: Double => String = "%.2f".format(_)): Quote = {
    q += ("BB Lower" -> fmt(b.lower)) += ("BB Middle" -> fmt(b.middle)) += ("BB Upper" -> fmt(b.upper))
  }

  // Calculates and adds Bollinger points to a Quote stream
  // Note: Assumes quotes are in descending date order, as provided by Yahoo Finance
  // Note: The number of quotes emitted will be reduced by the window size (dropped from the tail)
  lazy val addBollingerToQuoteFlow = Flow() { implicit b =>

    val extract = b.add(Flow[Quote].map(_("Close").toDouble))
    val statistics = b.add(Flow[Double].slidingStatistics(window))
    val bollinger = b.add(Flow[Statistics[Double]].map(Bollinger(_)).drop(window - 1))
    val merge = b.add(Flow[(Quote, Bollinger)].map(t => mergeBollinger(t._1, t._2)))

    val in = b.add(Broadcast[Quote](2))
    // See: https://github.com/akka/akka/issues/17435
    val buffer = b.add(Flow[Quote].buffer(Math.max(window -1, 1), OverflowStrategy.backpressure))
    val zip = b.add(Zip[Quote, Bollinger])

    in ~> buffer                             ~> zip.in0
    in ~> extract ~> statistics ~> bollinger ~> zip.in1
                                                zip.out ~> merge

    (in.in, merge.outlet)
  }

  // Convert a stream of quotes to CSV, first writing header then rows
  lazy val quoteToCsvFlow = Flow() { implicit b =>
    val columns = b.add(Flow[Quote].take(1).map(_.keys.toSeq).expand[Seq[String], Seq[String]](identity)(s => (s, s)))
    val header = b.add(Flow[(Seq[String], Quote)].take(1).map(_._1.mkString(",")))
    val rows = b.add(Flow[(Seq[String], Quote)].mapConcat { t =>
      val (cols, quote) = t
      immutable.Seq("\n", cols.map(quote(_)).mkString(","))
    })

    val in = b.add(Broadcast[Quote](3))
    val print = b.add(Sink.fold[Int, Quote](0){ (i, q) => logger.info(s"received quote $i: $q"); i + 1 })
    val zip = b.add(Zip[Seq[String], Quote])
    val bcast = b.add(Broadcast[(Seq[String], Quote)](2))
    val concat = b.add(Concat[String])

    in ~> columns ~> zip.in0
    in            ~> zip.in1
                     zip.out ~> bcast ~> header ~> concat
                                bcast ~> rows   ~> concat
    in ~> print

    (in.in, concat.out)
  }

  def formatRow(q: Quote, header: Boolean): String = if (header) q.keys.mkString(",") else "\n" + q.values.mkString(",")

  lazy val quoteToCsvFlow2 = Flow() { implicit b =>


    val rows = b.add(
          Flow[Quote].prefixAndTail(1)
          .mapConcat[Source[String, Unit]]{ pt =>
            val (prefix, tail) = pt
            immutable.Seq(
            Source(prefix).map(formatRow(_, true)),
            Source(prefix).map(formatRow(_, false)),
            tail.map(formatRow(_, false)))
          }.flatten(FlattenStrategy.concat))

    (rows.inlet, rows.outlet)

//    val columns = b.add(Flow[Quote].take(1).map(_.keys.toSeq).expand[Seq[String], Seq[String]](identity)(s => (s, s)))
//    val header = b.add(Flow[(Seq[String], Quote)].take(1).map(_._1.mkString(",")))
//    val rows = b.add(Flow[(Seq[String], Quote)].mapConcat { t =>
//      val (cols, quote) = t
//      immutable.Seq("\n", cols.map(quote(_)).mkString(","))
//    })
//
//    val in = b.add(Broadcast[Quote](3))
//    val zip = b.add(Zip[Seq[String], Quote])
//    val bcast = b.add(Broadcast[(Seq[String], Quote)](2))
//    val concat = b.add(Concat[String])
//
//    in ~> columns ~> zip.in0
//    in            ~> zip.in1
//                     zip.out ~> bcast ~> header ~> concat
//                                bcast ~> rows   ~> concat
//
//    (in.in, concat.out)
  }



  def getQuotes(symbol: String): Future[Source[ByteString, _]] = {
    val quoteFuture: Future[Source[Quote, _]] = quoteService.history(symbol, Period.ofMonths(3))
//    val csvFuture = quoteFuture.map(_.via(quoteToCsvFlow2))
//    val csvFuture = quoteFuture.map(_.via(quoteToCsvFlow))
    val csvFuture = quoteFuture.map(_.via(addBollingerToQuoteFlow).via(quoteToCsvFlow2))

    csvFuture.map(_.map(q => ByteString(q.toString)))
  }

  override val route: Route = {
    get {
      pathPrefix("quote") {
        path(Segment) { symbol =>
          complete {
            getQuotes(symbol).map(HttpEntity.Chunked.fromData(ContentType(MediaTypes.`text/plain`), _))
//            getQuotes(symbol).map(HttpEntity.Chunked.fromData(ContentType(MediaTypes.`text/csv`), _))
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
