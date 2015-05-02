package sample.yahoo

import akka.stream.stage.{SyncDirective, Context, PushStage}

class SlidingStatistics[T <% Numeric[T]](implicit num: Numeric[T]) extends PushStage[Iterable[T], Statistics[T]] {

  override def onPush(window: Iterable[T], ctx: Context[Statistics[T]]): SyncDirective =
    ctx.push(window.foldLeft(Statistics[T]())((s, e) => s.update(e)))

}
