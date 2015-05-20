package sample

import akka.http.scaladsl.client.RequestBuilding._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.github.scalaspring.akka.http._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import


// Echo a path segment
trait EchoService extends AkkaHttpService {
  abstract override def route: Route = {
    (get & path("echo" / Segment)) { name =>
      complete(name)
    }
  } ~ super.route
}

// Primitive site proxy (ex: http://localhost:8080/finance.yahoo.com)
trait ProxyService extends AkkaHttpService {

  @Autowired val client: HttpClient = null

  abstract override def route: Route = {
    (get & path("proxy" / Segment)) { site =>
      complete(client.request(Get(s"http://$site/")))
    }
  } ~ super.route
}

@SpringBootApplication
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends AkkaHttpServer with EchoService with ProxyService

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
