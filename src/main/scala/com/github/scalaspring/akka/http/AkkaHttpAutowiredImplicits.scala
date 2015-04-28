package com.github.scalaspring.akka.http

import akka.stream.FlowMaterializer
import com.github.scalaspring.akka.AkkaAutowiredImplicits
import org.springframework.beans.factory.annotation.Autowired

trait AkkaHttpAutowiredImplicits extends AkkaAutowiredImplicits {

  @Autowired implicit val materializer: FlowMaterializer = null

}
