package sample.random

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.scalaspring.akka.http.{AkkaHttpServerAutoConfiguration, AkkaHttpService}
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import


trait RandomService extends AkkaHttpService with JsonProtocols {

  override val route: Route = {
    get {
      pathPrefix("random") {
        path("name") {
          complete(Name.random)
        } ~
        path("creditcard") {
          complete(CreditCard.random)
        } ~
        path("user") {
          complete(User.random)
        }
      }
    }
  }

}


@SpringBootApplication
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends RandomService {}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
