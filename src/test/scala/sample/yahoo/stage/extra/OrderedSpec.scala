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
@ContextConfiguration(classes = Array(classOf[OrderedSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class OrderedSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  val permutations = Table(
    ("ascending", "strict", "values", "expected"),
    (
      true, true,
      List(1, 2, 3, 4, 5),
      List(Right(1), Right(2), Right(3), Right(4), Right(5))
    ),
    (
      true, true,
      List(1, 2, 3, 2, 3, 4, 5),
      List(Right(1), Right(2), Right(3), Left(2), Left(3), Right(4), Right(5))
    ),
    (
      true, false,
      List(1, 2, 3, 3, 2, 3, 4, 5),
      List(Right(1), Right(2), Right(3), Right(3), Left(2), Right(3), Right(4), Right(5))
    ),
    (
      false, true,
      List(5, 4, 3, 2, 1),
      List(Right(5), Right(4), Right(3), Right(2), Right(1))
    ),
    (
      false, true,
      List(5, 4, 3, 4, 3, 2, 1),
      List(Right(5), Right(4), Right(3), Left(4), Left(3), Right(2), Right(1))
    ),
    (
      false, false,
      List(5, 4, 3, 3, 4, 3, 2, 1),
      List(Right(5), Right(4), Right(3), Right(3), Left(4), Right(3), Right(2), Right(1))
    )
  )

  "Ordered" should "process all permutations" in {
    forAll(permutations) { (ascending: Boolean, strict: Boolean, values: List[Int], expected: List[Either[Int, Int]]) =>
      val future = Source(values).via(Flow[Int].transform(() => Ordered[Int](ascending, strict))).runWith(Sink.fold(List[Either[Int, Int]]())(_ :+ _))
      whenReady(future)(_ shouldBe expected)
    }
  }

}
