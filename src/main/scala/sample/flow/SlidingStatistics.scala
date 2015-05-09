package sample.flow

import akka.stream.stage.{Context, PushStage, SyncDirective}

//case class SlidingStatistics[T](implicit num: Numeric[T]) extends PushStage[Iterable[T], Statistics[T]] {
//  override def onPush(window: Iterable[T], ctx: Context[Statistics[T]]): SyncDirective =
//    ctx.push(Statistics[T](window)(num))
//}
