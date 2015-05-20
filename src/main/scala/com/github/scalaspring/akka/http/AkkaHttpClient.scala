package com.github.scalaspring.akka.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.scalaspring.akka.http.AkkaHttpClient.ConnectionFlow
import org.springframework.stereotype.Component

import scala.concurrent.Future


/**
 * Defines a basic HTTP client useful for common request operations.
 */
trait HttpClient {

  /** Send an HTTP request */
  def request(request: HttpRequest): Future[HttpResponse]

}

/**
 * Defines Akka HTTP client components useful for common request operations.
 *
 * @param connectionFlow provides connection flow instances for requests
 */
@Component
class AkkaHttpClient(connectionFlow: Uri => ConnectionFlow) extends HttpClient with AkkaStreamsAutowiredImplicits {

  override def request(request: HttpRequest): Future[HttpResponse] =
    Source.single(request).via(connectionFlow(request.uri)).runWith(Sink.head)

}

object AkkaHttpClient {

  type ConnectionFlow = Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]]

  def apply()(implicit system: ActorSystem): AkkaHttpClient = apply(defaultConnectionFlow _)
  def apply(connectionFlow: Uri => ConnectionFlow): AkkaHttpClient = new AkkaHttpClient(connectionFlow)

  /**
   * Default simple connection flow provider.
   */
  protected def defaultConnectionFlow(uri: Uri)(implicit system: ActorSystem): Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]] =
    Http().outgoingConnection(uri.authority.host.address(), uri.effectivePort)
}