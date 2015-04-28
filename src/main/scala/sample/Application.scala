package sample

import akka.http.marshallers.sprayjson.SprayJsonSupport._
import akka.http.server.Directives._
import akka.http.server.Route
import com.github.scalaspring.akka.http.{AkkaHttpAutowiredImplicits, AkkaHttpAutoConfiguration}
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.{Bean, Import}


trait Service extends AkkaHttpAutowiredImplicits with JsonProtocols {

  @Bean
  def route: Route = {
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
@Import(Array(classOf[AkkaHttpAutoConfiguration]))
class Application extends Service {}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
