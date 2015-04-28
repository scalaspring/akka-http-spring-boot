package sample

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.scalaspring.akka.http.{AkkaHttpAutoConfiguration, AkkaHttpAutowiredImplicits}
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
