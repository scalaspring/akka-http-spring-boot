package sample.yahoo

import akka.stream.stage.{Context, PushStage, SyncDirective}

/**
 * Implements a sliding window on a stream of elements.
 */
class SlidingWindow[T](size: Int) extends PushStage[T, Seq[T]] {

  require(size > 0, "window size must be positive")

  var seq = Seq[T]()

  override def onPush(elem: T, ctx: Context[Seq[T]]): SyncDirective = {
    if (seq.size == size) seq = seq.tail
    seq = seq :+ elem
    ctx.push(seq)
  }
}
