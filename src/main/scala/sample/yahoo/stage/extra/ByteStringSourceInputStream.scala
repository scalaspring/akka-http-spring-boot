package sample.yahoo.stage.extra

import java.io.InputStream
import java.nio.ByteBuffer

import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.stage._
import akka.util.ByteString

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Promise}

/**
 * Wraps a `Source[ByteString]` as an `InputStream` using an asynchronous stage.
 */
class ByteStringSourceInputStream(source: Source[ByteString, _])(implicit ec: ExecutionContext, materializer: FlowMaterializer) extends InputStream {

  private var buffer = Promise[Option[ByteBuffer]]
  private var signal: Promise[Unit] = Promise[Unit]

  source.transform(() => new AdapterStage).runWith(Sink.ignore)


  override def read(): Int = {
    // Block until a buffer is available to read
    Await.result(buffer.future, Duration.Inf).map { b =>
      val r = b.get.toInt

      // Signal for more data when the current buffer is exhausted
      if (!b.hasRemaining) {
        buffer = Promise[Option[ByteBuffer]]
        signal.success()
      }

      r
    }.getOrElse(-1)
  }


  private class AdapterStage extends AsyncStage[ByteString, ByteString, ByteString] {

    private var callback: AsyncCallback[ByteString] = _

    override def initAsyncInput(ctx: AsyncContext[ByteString, ByteString]): Unit = {
      callback = ctx.getAsyncCallback()
    }

    override def onAsyncInput(event: ByteString, ctx: AsyncContext[ByteString, ByteString]): Directive = {
      ctx.pushAndPull(event)
    }

    override def onPush(elem: ByteString, ctx: AsyncContext[ByteString, ByteString]): UpstreamDirective = {
      require(!elem.isEmpty, "elements must be non-empty")

      signal = Promise[Unit]
      signal.future.onComplete(t => callback.invoke(elem))
      buffer.success(Some(elem.asByteBuffer))
      ctx.holdUpstream()
    }

    override def onPull(ctx: AsyncContext[ByteString, ByteString]): DownstreamDirective = ctx.holdDownstream()

    override def onUpstreamFinish(ctx: AsyncContext[ByteString, ByteString]): TerminationDirective = {
      signal.future.onComplete(t => buffer.success(None))
      super.onUpstreamFinish(ctx)
    }
  }
}

object ByteStringSourceInputStream {
  def apply(source: Source[ByteString, _])(implicit ec: ExecutionContext, materializer: FlowMaterializer) = new ByteStringSourceInputStream(source)
}
