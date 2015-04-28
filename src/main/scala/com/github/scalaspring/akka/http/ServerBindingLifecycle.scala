package com.github.scalaspring.akka.http

import java.util.concurrent.atomic.AtomicReference

import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.StrictLogging
import org.springframework.context.SmartLifecycle

import scala.concurrent.Future
import scala.util.{Failure, Success}


object ServerBindingLifecycle {
  
  sealed trait State
  case object Stopped extends State
  case object Starting extends State
  case object Started extends State
  case object Stopping extends State

  def apply(settings: ServerSettings, route: Route) = new ServerBindingLifecycle(settings, route)

}

class ServerBindingLifecycle (val settings: ServerSettings, val route: Route) extends SmartLifecycle with AkkaHttpAutowiredImplicits with StrictLogging {

  import ServerBindingLifecycle._

  // Read-only state property
  private val _state: AtomicReference[State] = new AtomicReference(Stopped)
  def state = _state.get()

  // Read-only binding property
  private var _binding: Option[Future[ServerBinding]] = None
  def binding = _binding

  override def getPhase: Int = 0
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

    } else {
      logger.warn(s"Unexpected server state (state=${state}), ignoring call to start()")
    }
  }

  override def stop(callback: Runnable): Unit = {

    _binding.map { future =>
      future.andThen {
        case Success(b) => {

          if (_state.compareAndSet(Started, Stopping)) {
            logger.info(s"Stopping server on ${b.localAddress.getHostString}:${b.localAddress.getPort}")

            doStop(b).andThen {
              case Success(_) => logger.info(s"Server on ${b.localAddress.getHostString}:${b.localAddress.getPort} stopped")
              case Failure(t) => logger.error(s"Error stopping server on ${b.localAddress.getHostString}:${b.localAddress.getPort}", t)
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
