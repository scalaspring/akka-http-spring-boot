package sample.flow

import akka.stream.stage._

import scala.collection.mutable

/**
 * Reverses a stream of incoming elements.
 * The last element pushed by the upstream will be the first pushed downstream.
 * This means that the upstream must finish before elements begin flowing downstream.
 *
 * NOTE: Never use this stage on unbounded streams.
 */
case class Reverse[T]() extends StatefulStage[T, T] {

  private val elements = mutable.Stack[T]()

  override def initial = new StageState[T, T] {
    override def onPush(elem: T, ctx: Context[T]): SyncDirective = {
      elements.push(elem)
      ctx.pull
    }
  }

  override def onUpstreamFinish(ctx: Context[T]): TerminationDirective = terminationEmit(elements.iterator, ctx)

}
