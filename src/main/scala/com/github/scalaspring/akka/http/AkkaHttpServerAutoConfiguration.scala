package com.github.scalaspring.akka.http

import akka.http.scaladsl.server.Route
import com.github.scalaspring.akka.SpringLogging
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.{Import, Bean, Configuration}

import scala.reflect.runtime.universe.typeTag

/**
 * Defines an Akka HTTP server that serves all Akka HTTP routes found in the application context.
 *
 * Usage: Import this configuration into your application context.
 *
 * Example:
 *
 * {{{
 *   @Configuration
 *   @Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
 *   class Configuration extends MyService
 * }}}
 */
@Configuration
@EnableConfigurationProperties
@Import(Array(classOf[AkkaStreamsAutoConfiguration]))
class AkkaHttpServerAutoConfiguration extends AkkaStreamsAutowiredImplicits with SpringLogging {

  // Note: This is intentionally not required, even though a route is required, so that we can provide a useful message
  // below if the user doesn't define a route
  @Autowired(required=false)
  private val route: Route = null

  /**
   * Defines a server settings bean with values read from configuration.
   * Set the `http.server.interface` and `http.server.port` configuration properties to set server configuration.
   * Note: This an automatic bean definition that can be overridden by supplying your own bean definition.
   */
  @Bean @ConditionalOnMissingBean(Array(classOf[ServerSettings]))
  def serverSettings = new ServerSettings()

  /**
   * Defines the server binding lifecycle bean that creates the server for the route defined in the application context.
   * Note: This an automatic bean definition that can be overridden by supplying your own bean definition.
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

  /**
   * Defines an HTTP client that can be used for outgoing HTTP connections.
   * Note: This an automatic bean definition that can be overridden by supplying your own bean definition.
   */
  @Bean @ConditionalOnMissingBean(Array(classOf[HttpClient]))
  def httpClient: HttpClient = AkkaHttpClient()

}
