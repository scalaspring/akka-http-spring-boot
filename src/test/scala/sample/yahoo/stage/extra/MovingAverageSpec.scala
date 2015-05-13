package sample.yahoo.stage.extra

import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration

@Configuration
@ContextConfiguration(classes = Array(classOf[MovingAverageSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class MovingAverageSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  val permutations = Table(
    ("window", "input", "expected"),
    (
      10,
      List(1.0, 2.0, 3.0, 4.0, 5.0),
      List(1.0, 1.5, 2.0, 2.5, 3.0)
    ),
    (
      3,
      List(1.0, 2.0, 3.0, 4.0, 5.0),
      List(1.0, 1.5, 2.0, 3.0, 4.0)
    )
  )


  "Moving average stage" should "properly calculate all valid permutations" in {
    forAll(permutations) { (window: Int, input: List[Double], expected: List[Double]) =>
      val future = Source(input).via(Flow[Double].transform(() => MovingAverage(window))).runWith(Sink.fold(List[Double]())(_ :+ _))
      whenReady(future)(_ shouldBe expected)
    }
  }

  it should "fail for non-positive sized window" in {
    an [IllegalArgumentException] should be thrownBy MovingAverage[Int](0)
    an [IllegalArgumentException] should be thrownBy MovingAverage[Int](-1)
  }
}
