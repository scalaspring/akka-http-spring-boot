### Akka HTTP Spring Boot Integration (akka-http-spring-boot)

Integrates Scala Akka HTTP and Spring Boot for rapid, robust service development with minimal configuration.
Pre-configured server components require little more than a route to get a service up and running.

#### Key Benefits
1. Simple creation of microservices
   * Create your service route and implement it via a trivial server class to get going
   * Easily send outbound requests to aggregate data from other services
2. Highly testable, easily composable services
   * Full support for testing service routes and injecting an HTTP client to mock outbound requests during testing
   * Services are implemented via stackable Scala traits to promote bite-sized, reusable services that are easily composed in your server class. Ditch the monster service route.
3. Pre-configured server that's managed for you
   * No need to manage the server lifecycle. An Akka HTTP server will be started when your application starts and terminated when your application is stopped.
   * Provide your own configuration via standard Spring Boot configuration (application.yaml or other property source) to override defaults, if needed
4. Full Spring dependency injection support
   * Autowire any dependency into your services and leverage the full Spring ecosystem
   * Use existing Spring components to enable gradual migration or reuse of suitable existing enterprise components
   * Avoid the downsides of using Scala implicits or abstract types to implement dependency injection. While both implicits and abstract types are excellent language features, they can also lead to tight coupling and less maintainable code.

#### Getting Started

##### build.sbt

````scala
libraryDependencies ++= "com.github.scalaspring" %% "akka-http-spring-boot" % "0.2.1"
````

##### Creating a service

Extend `AkkaHttpService` and override the `route` method to define your service

````scala
// Echo a path segment
trait EchoService extends AkkaHttpService {
  abstract override def route: Route = {
    (get & path("echo" / Segment)) { name =>
      complete(name)
    }
  } ~ super.route
}
````

###### Notes on the code

* Be sure to use `abstract override` and to concatenate `super.route` when defining your route to make your services composable. See `MultiServiceSpec` in the test source for an example of a server implementing multiple, stackable services.

##### Create the server configuration and run it

Extend `AkkaHttpServer` and import `AkkaHttpServerAutoConfiguration` to implement your service(s)

````scala
@Configuration
@Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
class Application extends AkkaHttpServer with EchoService

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
````

#### Testing

Create a ScalaTest-based test that extends the service trait to test your service route

````scala
@Configuration
@ContextConfiguration(classes = Array(classOf[EchoServiceSpec]))
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class EchoServiceSpec extends FlatSpec with TestContextManagement with EchoService with ScalatestRouteTest with Matchers {

  "Echo service" should "echo" in {
    Get(s"/echo/test") ~> route ~> check {
      status shouldBe OK
      responseAs[String] shouldBe "test"
    }
  }

}
````

#### Outbound Requests

Inject the Akka HTTP-based client into your service and call its `request` method to make outbound requests.
The HttpClient bean is automatically created as part of the `AkkaHttpServerAutoConfiguration` configuration.

````scala
// Primitive site proxy (ex: http://localhost:8080/finance.yahoo.com)
trait ProxyService extends AkkaHttpService {

  @Autowired val client: HttpClient = null

  abstract override def route: Route = {
    (get & path("proxy" / Segment)) { site =>
      complete(client.request(Get(s"http://$site/")))
    }
  } ~ super.route
}
````

##### Notes on the code

* The autowired `HttpClient` will be injected by Spring (standard injection) upon startup.

#### Putting it all together - stackable services

````scala
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
````