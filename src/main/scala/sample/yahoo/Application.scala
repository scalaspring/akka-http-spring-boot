package sample.yahoo

import java.time.Period

import akka.http.scaladsl.model.{ContentType, HttpEntity, MediaTypes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.FlowShape
import akka.stream.scaladsl.FlowGraph.Implicits._
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.scalaspring.akka.http.{AkkaHttpServerAutoConfiguration, AkkaHttpService}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import sample.flow._

import scala.concurrent.Future


trait BollingerQuoteService extends AkkaHttpService {

  val window: Int = 14

  @Autowired val quoteService: QuoteService = null

  def mergeBollinger(p: (Quote, Bollinger), fmt: Double => String = "%.2f".format(_)): Quote = {
    val (q, b) = p
    (q + ("BB Lower" -> fmt(b.lower)) + ("BB Middle" -> fmt(b.middle)) + ("BB Upper" -> fmt(b.upper)))
  }

  lazy val bollingerGraph = FlowGraph.partial() { implicit b =>

    val extract = b.add(Flow[Quote].map(_("Close").toDouble))
    val statistics = b.add(Flow[Double].slidingStatistics(window))
    // TODO: Add drop back once we figure out the issue with zipping different length streams
    val bollinger = b.add(Flow[Statistics[Double]].map(Bollinger(_))/*.drop(window - 1)*/)
    val merge = b.add(Flow[(Quote, Bollinger)].map(mergeBollinger(_)))

    val broadcast = b.add(Broadcast[Quote](2))
    val zip = b.add(Zip[Quote, Bollinger])

    broadcast ~>                                       zip.in0
    broadcast ~> extract ~> statistics ~> bollinger ~> zip.in1
                                                       zip.out ~> merge

    FlowShape(broadcast.in, merge.outlet)
  }

  // https://groups.google.com/forum/#!searchin/akka-user/akka-streams$20outlet/akka-user/Sivt8uXuRH8/U48pnOuaisMJ
  lazy val bollingerFlow = Flow() { implicit b =>
    val g = b.add(bollingerGraph)
    (g.inlet, g.outlet)
  }

  def getQuotes(symbol: String): Future[Source[ByteString, Unit]] = {
    val quoteFuture: Future[Source[Quote, Unit]] = quoteService.history(symbol, Period.ofMonths(1))
    val bollingerFuture = quoteFuture.map(_.via(bollingerFlow))

    bollingerFuture.map(_.map(q => ByteString(q.toString)))
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
