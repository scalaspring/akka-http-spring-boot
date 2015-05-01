package sample.yahoo

import akka.stream.stage.{Context, PushStage, SyncDirective}

import scala.collection.mutable

/**
 * Calculates the moving average over a stream of numeric elements.
 *
 * @param n moving average window size
 * @tparam T numeric element type (Int, Long, Double, etc.)
 */
// See: https://twitter.github.io/scala_school/advanced-types.html
class MovingAverage[T](n: Int)(implicit num: Numeric[T]) extends PushStage[T, Double] {

  require(n > 0, "moving average window size must be positive")

  val queue = mutable.Queue[T]()
  var sum = num.zero

  override def onPush(elem: T, ctx: Context[Double]): SyncDirective = {
    if (queue.size == n) sum = num.minus(sum, queue.dequeue())
    queue += elem
    sum = num.plus(sum, elem)
    ctx.push(num.toDouble(sum) / queue.size)
  }
}

object MovingAverage {
  def apply[T](n: Int)(implicit num: Numeric[T]) = new MovingAverage[T](n)(num)
}
