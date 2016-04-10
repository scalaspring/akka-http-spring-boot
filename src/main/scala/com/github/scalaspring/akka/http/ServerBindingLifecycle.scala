package com.github.scalaspring.akka.http

import java.util.concurrent.atomic.AtomicReference

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import com.github.scalaspring.akka.ActorSystemLifecycle
import com.typesafe.scalalogging.StrictLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.SmartLifecycle

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object ServerBindingLifecycle {
  
  sealed trait State
  case object Stopped extends State
  case object Starting extends State
  case object Started extends State
  case object Stopping extends State

  def apply(settings: ServerSettings, route: Route) =
    new ServerBindingLifecycle(settings, route)

}

/**
 * Manages the lifecycle of an Akka HTTP `ServerBinding`, ensuring its lifecycle matches that of the containing
 * Spring application context.
 *
 * This is an internal management class and is not intended for direct use. An instance is automatically created by
 * [AkkaHttpServerAutoConfiguration].
 *
 * The Spring lifecycle phase (default 10) can be adjusted by setting the http.server.lifecycle.phase configuration
 * property. Note that the phase MUST be greater than that of the ActorSystemLifecycle bean to ensure the underlying
 * ActorSystem is started before, and is terminated after, the Akka HTTP server.
 *
 * @param settings binding settings
 * @param route route definition
 */
class ServerBindingLifecycle (val settings: ServerSettings, val route: Route)
  extends SmartLifecycle with AkkaStreamsAutowiredImplicits with StrictLogging
{

  import ServerBindingLifecycle._

  // Read-only state property
  private val _state: AtomicReference[State] = new AtomicReference(Stopped)
  def state = _state.get()

  // Read-only binding property
  private var _binding: Option[Future[ServerBinding]] = None
  def binding = _binding

  @Value("${http.server.lifecycle.timeout:30 seconds}")
  protected val timeout: String = "30 seconds"

  @Value("${http.server.lifecycle.phase:10}")
  protected val phase: Int = 10
  override def getPhase: Int = phase

  override def isAutoStartup: Boolean = true

  override def isRunning: Boolean = (_state.get() == Started)

  override def start(): Unit = {

    if (_state.compareAndSet(Stopped, Starting)) {
      logger.info("Starting server")

      _binding = Some(doStart().andThen {
        case Success(binding) => {
          logger.info(s"Started server on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}")
          _state.set(Started)
        }
        case Failure(t) => {
          logger.error("Failed to start server", t)
          _binding = None
          _state.set(Stopped)
        }
      })

      // Wait for the server binding to complete before returning
      Await.result(_binding.get, Duration(timeout))

    } else {
      logger.warn(s"Unexpected server state (state=${state}), ignoring call to start()")
    }
  }

  override def stop(callback: Runnable): Unit = {

    _binding.map { future =>
      future.andThen {

        case Success(binding) => {
          if (_state.compareAndSet(Started, Stopping)) {
            logger.info(s"Stopping server on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}")

            doStop(binding).andThen {
              case Success(_) => logger.info(s"Server on ${binding.localAddress.getHostString}:${binding.localAddress.getPort} stopped")
              case Failure(t) => logger.error(s"Error stopping server on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}", t)
            }.andThen {
              case r => {
                _state.set(Stopped)
                _binding = None
                callback.run()
              }
            }

          } else {
            logger.warn(s"Unexpected server state (state=${state}), ignoring call to stop()")
            callback.run()
          }
        }

        case Failure(t) => {
          logger.info("Server failed to bind, ignoring call to stop()")
          callback.run()
        }
      }

    }.getOrElse {
      logger.info("No server binding, ignoring call to stop()")
      callback.run()
    }

  }

  protected def doStart(): Future[ServerBinding] = {
    Http().bindAndHandle(Route.handlerFlow(route), settings.interface, settings.port)
  }

  protected def doStop(binding: ServerBinding): Future[Unit] = {
    binding.unbind()
  }

  // Intentionally left unimplemented since it should never be called
  override def stop(): Unit = ???

}
