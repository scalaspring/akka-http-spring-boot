package com.github.scalaspring.akka.http

import org.springframework.boot.context.properties.ConfigurationProperties

import scala.beans.BeanProperty
import ServerSettings._

/**
 * HTTP server settings with defaults.
 *
 * This is a standard Spring Boot configuration properties class that will be populated by Spring using property
 * sources available in the application context.
 * HTTP server settings are read from properties prefixed with 'http.server'
 *
 * Example YAML configuration (application.yaml)
 *
 * {{{
 *   http:
 *     server:
 *       interface: localhost
 *       port: 8080
 * }}}
 *
 */
@ConfigurationProperties(prefix = "http.server")
class ServerSettings(
  @BeanProperty var interface: String = DefaultInterface,
  @BeanProperty var port: Int = DefaultPort) {

  //def this() = this(interface = DefaultInterface, port = DefaultPort)

}

object ServerSettings {
  val DefaultInterface = "localhost"
  val DefaultPort = 8080
}
