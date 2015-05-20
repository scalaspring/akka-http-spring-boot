package com.github.scalaspring.akka.http

import org.springframework.boot.context.properties.ConfigurationProperties

import scala.beans.BeanProperty

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
  @BeanProperty var interface: String = "localhost",
  @BeanProperty var port: Int = 8080) {

  def this() = this(interface = "localhost", port = 8080)

//  @BeanProperty var interface: String = "localhost"
//  @BeanProperty var port: Int = 8080

}
