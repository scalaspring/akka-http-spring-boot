package com.github.scalaspring.akka.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import org.springframework.beans.BeanInstantiationException
import org.springframework.context.annotation.Bean
import org.springframework.util.ClassUtils

/**
 * Base trait used to define services. Defines the service route bean used by [AkkaHttpServerAutoConfiguration].
 *
 * This trait is designed to be stackable, allowing multiple services to be implemented in a single server.
 *
 * Usage: Extend this trait and override the route function to create your service.
 *
 * Example: Note the use of `abstract override` and the concatenation of `super.route` to make the service stackable.
 * {{{
 *
 *  // Define the service route in trait
 *  trait EchoService extends AkkaHttpService {
 *    abstract override def route: Route = {
 *      get {
 *        path("echo" / Segment) { name =>
 *          complete(name)
 *        }
 *      }
 *    } ~ super.route
 *  }
 *
 *  // Implement the trait in your application configuration
 *  @Configuration
 *  @Import(Array(classOf[AkkaHttpServerAutoConfiguration]))
 *  class Configuration extends EchoService
 *
 * }}}
 */
trait AkkaHttpService extends AkkaStreamsAutowiredImplicits {

  def route: Route = reject

  @Bean(name = Array("route"))
  def akkaHttpRoute: Route = {
    if (route == reject) throw new BeanInstantiationException(classOf[Route],
      s"Please supply a route definition by overriding the route function in ${ClassUtils.getUserClass(this).getName}. " +
        s"See the example in the documentation for ${classOf[AkkaHttpService].getName}.")
    else route
  }

}

