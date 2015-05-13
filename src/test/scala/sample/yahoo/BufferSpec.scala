package sample.yahoo

import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration

import scala.concurrent.Future

@Configuration
@ContextConfiguration(classes = Array(classOf[BufferSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class BufferSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  def zipSource(num: Int, diff: Int) = Source() { implicit b =>
    import akka.stream.scaladsl.FlowGraph.Implicits._

    val source0 = b.add(Source(1 to num))
    val source1 = b.add(Source(1 to (num + diff)))
    val zip = b.add(Zip[Int, Int])

    source0 ~> zip.in0
    source1 ~> zip.in1

    (zip.out)
  }

  def dropSinkSource(num: Int, diff: Int) = Source() {  implicit b =>
    import akka.stream.scaladsl.FlowGraph.Implicits._

    val source = b.add(Source(1 to (num + diff)))
    val bcast = b.add(Broadcast[Int](2))
    val drop = b.add(Flow[Int].drop(diff))
    val sink0 = b.add(Sink.ignore)

    source ~> bcast ~> sink0
    bcast ~> drop

    (drop.outlet)
  }

  def zipDropSource(num: Int, diff: Int) = Source() {  implicit b =>
    import akka.stream.scaladsl.FlowGraph.Implicits._

    val source = b.add(Source(1 to (num + diff)))
    val bcast = b.add(Broadcast[Int](2))
//    val buffer = b.add(Flow[Int].buffer(Math.max(diff, 1), OverflowStrategy.backpressure))
    val buffer = b.add(Flow[Int].buffer(Math.max(diff, 1), OverflowStrategy.fail))
    val drop = b.add(Flow[Int].drop(diff))
    val zip = b.add(Zip[Int, Int])

    source ~> bcast ~> buffer ~>  zip.in0
              bcast ~> drop ~> zip.in1

    (zip.out)
  }

  // PASS
  "Zip" should "complete with same length streams" in {
    val future: Future[Int] = zipSource(10, 10).runWith(Sink.fold(0)((s, i) => s + 1))
    whenReady(future)(_ shouldBe 10)
  }

  // PASS
  it should "complete with different length streams" in {
    val future: Future[Int] = zipSource(10, 20).runWith(Sink.fold(0)((s, i) => s + 1))
    whenReady(future)(_ shouldBe 10)
  }

  // PASS
  "Sink with drop" should "complete with different length streams" in {
    val future: Future[Int] = dropSinkSource(10, 10).runWith(Sink.fold(0)((s, i) => s + 1))
    whenReady(future)(_ shouldBe 10)
  }

  // PASS
  "Zip with drop" should "complete with same length streams" in {
    val future: Future[Int] = zipDropSource(10, 0).runWith(Sink.fold(0)((s, i) => s + 1))
    whenReady(future)(_ shouldBe 10)
  }

  // FAIL
  it should "complete with different length streams" in {
    val future: Future[Int] = zipDropSource(10, 10).runWith(Sink.fold(0)((s, i) => s + 1))
    whenReady(future)(_ shouldBe 10)
  }

}

