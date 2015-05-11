package sample.yahoo

import akka.stream.scaladsl._
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalactic.Tolerance._
import org.scalactic.TripleEqualsSupport.Spread
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration
import sample.flow._
import sample.util.Util
import sample.yahoo.BollingerSpec.Expected

@Configuration
@ContextConfiguration(classes = Array(classOf[BollingerSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class BollingerSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  "Bollinger stage" should "properly calculate bands" in {
    val window = 14
    val input: List[Double] = Util.openCsvResource("/bollinger_test_data.csv").map(_("Close").toDouble).toList
    val expected: List[Expected] = Util.openCsvResource("/bollinger_test_data.csv").toList.dropRight(window - 1).map(r => Expected(r("BB(14) Lower").toDouble, r("SMA(14)").toDouble, r("BB(14) Upper").toDouble))

    val bollingerFlow = Flow[Double].slidingStatistics(window).map(Bollinger(_)).drop(window - 1)

    val future = Source(input).via(bollingerFlow).runWith(Sink.fold(List[Bollinger]())(_ :+ _))
    whenReady(future) { result =>

      result should have size expected.size

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