package sample

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.github.scalaspring.akka.http.{AkkaHttpServer, AkkaHttpServerAutoConfiguration, AkkaHttpService}
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import


trait EchoService extends AkkaHttpService {
  abstract override def route: Route = {
    get {
      path("echo" / Segment) { name =>
        complete(name)
      }
    }
  } ~ super.route
}

@SpringBootApplication
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends AkkaHttpServer with EchoService

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
