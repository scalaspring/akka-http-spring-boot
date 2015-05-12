package sample.flow

import akka.stream.stage._

/**
 * Implements a sliding window on a stream of elements.
 * Follows the same semantics as IterableLike.sliding().
 */
case class Sliding[T](size: Int) extends StatefulStage[T, Iterator[T]] {

  require(size > 0, "window size must be positive")

  private var seq = Seq[T]()

  override def initial = new StageState[T, Iterator[T]] {
    override def onPush(elem: T, ctx: Context[Iterator[T]]): SyncDirective = {
      if (seq.size == size) seq = seq.tail
      seq = seq :+ elem
      if (seq.size == size) ctx.push(seq.iterator) else ctx.pull()
    }
  }

  override def onUpstreamFinish(ctx: Context[Iterator[T]]): TerminationDirective = {
    // Push the incomplete window if stream didn't contain enough elements
    if (seq.size < size) terminationEmit(Seq(seq.iterator).iterator, ctx)
    else super.onUpstreamFinish(ctx)
  }
}
