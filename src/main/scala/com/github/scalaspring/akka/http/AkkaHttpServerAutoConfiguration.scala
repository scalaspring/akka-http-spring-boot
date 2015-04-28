package com.github.scalaspring.akka.http

import akka.http.scaladsl.server.Route
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.{Bean, Configuration}

import scala.reflect.runtime.universe._

@Configuration
@EnableConfigurationProperties
class AkkaHttpServerAutoConfiguration extends AkkaStreamsAutoConfiguration {

  // This is intentionally not required, even though a route is required, so that we can provide a useful message
  // below if the user doesn't define a route
  @Autowired(required=false)
  private val route: Route = null

  /**
   * Creates a server settings that reads from configuration, if none supplied.
   */
  @Bean @ConditionalOnMissingBean(Array(classOf[ServerSettings]))
  def serverSettings = new ServerSettings()

  /**
   * Creates the server binding for the route defined in the application context.
   */
  @Bean @ConditionalOnMissingBean(Array(classOf[ServerBindingLifecycle]))
  def serverBindingLifecycle(settings: ServerSettings): ServerBindingLifecycle = {
    if (route == null) {
      val msg = s"Error starting server: No route defined. Please define a bean named 'route' of type ${typeTag[Route].tpe}. Akka HTTP applications require a route definition."
      log.error(msg)
      throw new BeanCreationException(msg)
    }
    else ServerBindingLifecycle(settings, route)
  }

}
