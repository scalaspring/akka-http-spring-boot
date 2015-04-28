package com.github.scalaspring.akka.http

import akka.http.scaladsl.server._
import akka.stream.FlowMaterializer
import com.github.scalaspring.akka.AkkaAutowiredImplicits
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean

trait AkkaHttpService extends AkkaHttpAutowiredImplicits {

  protected val route: Route

  @Bean(name = Array("route"))
  def akkaHttpRoute: Route = route

}

trait AkkaHttpAutowiredImplicits extends AkkaAutowiredImplicits {

  @Autowired implicit val materializer: FlowMaterializer = null

}
