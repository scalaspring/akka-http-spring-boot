package sample.yahoo

import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration
import sample.flow.Reverse

@Configuration
@ContextConfiguration(classes = Array(classOf[ReverseSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class ReverseSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  "Reverse" should "reverse elements" in {
    val values = List(1, 2, 3, 4, 5)
    val expected = List(5, 4, 3, 2, 1)

    val future = Source(values).via(Flow[Int].transform(() => Reverse[Int])).runWith(Sink.fold(List[Int]())(_ :+ _))

    whenReady(future)(_ shouldBe expected)
  }

}
