package com.github.scalaspring.akka.http

import akka.stream.{ActorFlowMaterializer, FlowMaterializer}
import com.github.scalaspring.akka.{SpringLogging, AkkaAutowiredImplicits, AkkaAutoConfiguration}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.{Bean, Configuration, Import}

@Configuration
@Import(Array(classOf[AkkaAutoConfiguration]))
class AkkaStreamsAutoConfiguration extends AkkaAutowiredImplicits with SpringLogging {

  @Bean @ConditionalOnMissingBean(Array(classOf[FlowMaterializer]))
  def flowMaterializer = ActorFlowMaterializer()

}

trait AkkaHttpAutowiredImplicits extends AkkaAutowiredImplicits {

  @Autowired implicit val flowMaterializer: FlowMaterializer = null

}
