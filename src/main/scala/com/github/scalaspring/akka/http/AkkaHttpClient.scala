package com.github.scalaspring.akka.http

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

/**
 * Defines Akka HTTP client components useful for common request operations.
 */
trait AkkaHttpClient extends AkkaStreamsAutowiredImplicits {

  protected def request(request: HttpRequest): Future[HttpResponse] =
    Source.single(request).via(connectionFlow(request.uri)).runWith(Sink.head)

  /**
   * Provides connection flow instances for requests. Override if connection customization is required.
   */
  protected def connectionFlow(uri: Uri): Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]] =
    Http().outgoingConnection(uri.authority.host.address(), uri.effectivePort)

}
