package com.github.scalaspring.akka.http

import akka.stream.{ActorMaterializer, Materializer}
import com.github.scalaspring.akka.{AkkaAutoConfiguration, AkkaAutowiredImplicits, SpringLogging}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.{Bean, Configuration, Import}

/**
 * Configures Spring to materialize Akka Streams flows via Akka.
 */
@Configuration
@Import(Array(classOf[AkkaAutoConfiguration]))
class AkkaStreamsAutoConfiguration extends AkkaAutowiredImplicits with SpringLogging {

  @Bean @ConditionalOnMissingBean(Array(classOf[Materializer]))
  def materializer = ActorMaterializer()

}

/**
 * Defines autowired implicits needed to materialize Akka Streams flows.
 */
trait AkkaStreamsAutowiredImplicits extends AkkaAutowiredImplicits {

  @Autowired implicit val materializer: Materializer = null

}
