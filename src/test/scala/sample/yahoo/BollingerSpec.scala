package sample.yahoo

import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.TripleEqualsSupport.Spread
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration
import sample.flow._
import org.scalactic.Tolerance._
import sample.util.Util
import sample.yahoo.BollingerSpec.Expected

@Configuration
@ContextConfiguration(classes = Array(classOf[BollingerSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class BollingerSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  "Bollinger stage" should "properly calculate bands" in {
    val window = 14
    val input: List[Double] = Util.openCsvResource("/bollinger_test_data.csv").map(_("Close").toDouble).toList.dropRight(window - 1)
    val expected: List[Expected] = Util.openCsvResource("/bollinger_test_data.csv").toList.dropRight(window - 1).map(r => Expected(r("BB(14) Lower").toDouble, r("SMA(14)").toDouble, r("BB(14) Upper").toDouble))

    val future = Source(input).via(Flow[Double].slidingWindow(window).statistics.bollinger.drop(window - 1)).runWith(Sink.fold(List[BollingerPoints]())(_ :+ _))
    whenReady(future) { result =>

      result.zip(expected).foreach { pair =>
        val (bollinger, expected) = pair

        bollinger.lower shouldBe expected.lower
        bollinger.middle shouldBe expected.middle
        bollinger.upper shouldBe expected.upper

      }
    }
  }

}

object BollingerSpec {
  case class Expected(lower: Spread[Double], middle: Spread[Double], upper: Spread[Double])
  object Expected {
    def apply(lower: Double, middle: Double, upper: Double, tolerance: Double = 0.001) =
      new Expected(lower +- tolerance, middle +- tolerance, upper +- tolerance)
  }
}