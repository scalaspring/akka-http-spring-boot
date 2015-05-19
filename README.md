# Akka HTTP Spring Boot Integration Library

Integrates Scala Akka HTTP and Spring Boot for rapid, robust service development with minimal configuration.
Pre-configured server components require little more than a route to get a service up and running.

## Key Benefits
1. Simple creation of microservices
   * Create your service route and implement it via a trivial application class to get going
   * Easily send outbound requests to aggregate data from other services
2. Highly testable, easily composable services
   * Full support for testing service routes and injecting an HTTP client to mock outbound requests during testing
   * Services are implemented via stackable Scala traits, letting you ditch the monster service route and instead design bite-sized services that are easily composable in your application class
3. Pre-configured server that's managed for you
   * No need to manage the server lifecycle. An Akka HTTP server will be started when your application starts and terminated when your application is stopped.
   * Provide your own configuration via standard Spring Boot configuration (application.yaml) to override defaults, if needed
4. Full Spring dependency injection support
   * Autowire any dependency into your services and leverage the full Spring ecosystem
   * Use existing Spring components to enable gradual migration or reuse of suitable existing enterprise components
   * Avoid the anti-patterns of using Scala implicits or abstract types to implement dependency injection. Scala implicits are excellent, but can be abused, IMHO, to pass dependencies around an application resulting in tight coupling and less maintainable code.

## Getting Started

### build.sbt

````scala
libraryDependencies ++= "com.github.scalaspring" %% "akka-http-spring-boot" % "0.2.0"
````

### Create a Spring Configuration

Create a configuration class that extends the ActorSystemConfiguration trait and imports the AkkaAutoConfiguration

````scala
@Configuration
@ComponentScan
@Import(Array(classOf[AkkaAutoConfiguration]))
class Configuration extends ActorSystemConfiguration {

  // Note: the EchoActor class is part of Akka test kit
  @Bean
  def echoActor = actorOf[EchoActor]

}
````

### Testing Your Configuration

Create a ScalaTest-based test that uses the configuration

````scala
@ContextConfiguration(
  loader = classOf[SpringApplicationContextLoader],
  classes = Array(classOf[AkkaAutoConfigurationSpec.Configuration])
)
class AkkaAutoConfigurationSpec extends FlatSpec with TestContextManagement with Matchers with AskSupport with ScalaFutures with StrictLogging {

  implicit val timeout: Timeout = (1 seconds)

  @Autowired val echoActor: ActorRef = null

  "Echo actor" should "receive and echo message" in {
    val message = "test message"
    val future = echoActor ? message

    whenReady(future) { result =>
      logger.info(s"""received result "$result"""")
      result should equal(message)
    }
  }

}
````