package com.github.scalaspring.akka.http

import akka.http.scaladsl.server._
import org.springframework.context.annotation.Bean

trait AkkaHttpService extends AkkaHttpAutowiredImplicits {

  protected val route: Route

  @Bean(name = Array("route"))
  def akkaHttpRoute: Route = route

}

