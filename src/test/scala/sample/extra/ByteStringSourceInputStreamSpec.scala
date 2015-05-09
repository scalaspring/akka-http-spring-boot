package sample.extra

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaStreamsAutoConfiguration}
import com.github.scalaspring.scalatest.TestContextManagement
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, FlatSpec}
import org.springframework.context.annotation.{Configuration, Import}
import org.springframework.test.context.ContextConfiguration

@Configuration
@ContextConfiguration(classes = Array(classOf[ByteStringSourceInputStreamSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class ByteStringSourceInputStreamSpec extends FlatSpec with TestContextManagement with AkkaHttpAutowiredImplicits with Matchers with ScalaFutures with StrictLogging {

  "ByteString input source" should "read all bytes" in {
    val strings = List("one", "two", "three")
    val values = strings.map(ByteString(_))
    val source = Source(values)
    val stream = ByteStringSourceInputStream(source)

    val expected = List(5, 4, 3, 2, 1)

    var c: Int = 0
    do {
      c = stream.read
      logger.info(s"read ${c.toChar} from stream")
    } while (c != -1)

    true shouldBe true
  }

}
