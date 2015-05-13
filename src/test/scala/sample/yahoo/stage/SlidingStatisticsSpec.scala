package sample.yahoo.stage

import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration
import sample.yahoo.Statistics
import sample.yahoo.StatisticsSpec.Expected

@Configuration
@ContextConfiguration(classes = Array(classOf[SlidingStatisticsSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class SlidingStatisticsSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  val permutations = Table(
    ("window", "input", "expected"),
    ( 5, List(1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89, 144, 233, 377, 610),
      List((2.4, 2.8), (3.8, 7.7), (6.2, 19.7), (10.0, 52.0), (16.2, 135.7), (26.2, 355.7), (42.4, 930.8), (68.6, 2437.3), (111.0, 6380.5), (179.6, 16704.8), (290.6, 43733.3)).map(e => Expected(e._1, e._2)))
  )

  "Sliding statistics stage" should "properly calculate all valid permutations" in {
    forAll(permutations) { (window: Int, input: List[Int], expected: List[Expected]) =>
      val future = Source(input).via(Flow[Int].slidingStatistics(window)).runWith(Sink.fold(List[Statistics[_]]())(_ :+ _))
      whenReady(future) { result =>

        result.zip(expected).foreach { pair =>
          val (stats, expected) = pair

          stats.mean shouldBe expected.mean
          stats.variance shouldBe expected.variance
          stats.stddev shouldBe expected.stddev

        }
      }
    }
  }

}
