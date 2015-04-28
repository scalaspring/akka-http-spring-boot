package com.github.scalaspring.akka.http

import org.springframework.boot.context.properties.ConfigurationProperties

import scala.beans.BeanProperty

@ConfigurationProperties(prefix = "http.server")
class ServerSettings {

  @BeanProperty var interface: String = "localhost"
  @BeanProperty var port: Int = 8080

}
