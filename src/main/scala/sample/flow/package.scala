package sample

import akka.stream.scaladsl.{Flow}

package object flow {

  implicit class Operations[In, Out, Mat](val flow: Flow[In, Out, Mat]) {
    def movingAverage(size: Int)(implicit num: Numeric[Out]) = flow.transform(() => MovingAverage[Out](size)(num))
    def slidingWindow(size: Int) = flow.transform(() => SlidingWindow[Out](size))
  }

  implicit class WindowOperations[In, Out, Mat](val flow: Flow[In, Seq[Out], Mat]) {
    def statistics(implicit num: Numeric[Out]) = flow.transform(() => SlidingStatistics[Out])
  }

  implicit class StatisticalOperations[In, Out <: Statistics[In], Mat](val flow: Flow[In, Out, Mat]) {
    def bollinger = flow.transform(() => Bollinger())
  }

}
