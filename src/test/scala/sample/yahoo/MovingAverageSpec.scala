package sample.yahoo

import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration

@Configuration
@ContextConfiguration(classes = Array(classOf[MovingAverageSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class MovingAverageSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  "Moving average" should "calculate over short stream" in {
    val values = List(1, 2, 3, 4, 5)
    val expected = List(1.0, 1.5, 2.0, 2.5, 3.0)

    val future = Source(values).via(Flow[Int].transform(() => MovingAverage[Int](10))).runWith(Sink.fold(List[Double]())(_ :+ _))

    whenReady(future)(_ shouldBe expected)
  }

  it should "calculate over long stream" in {
    val values = List(1.0, 2.0, 3.0, 4.0, 5.0)
    val expected = List(1.0, 1.5, 2.0, 3.0, 4.0)

    val future = Source(values).via(Flow[Double].transform(() => MovingAverage[Double](3))).runWith(Sink.fold(List[Double]())(_ :+ _))

    whenReady(future)(_ shouldBe expected)
  }

  it should "fail for non-positive sized window" in {
    an [IllegalArgumentException] should be thrownBy MovingAverage[Int](0)
    an [IllegalArgumentException] should be thrownBy MovingAverage[Int](-1)
  }
}
