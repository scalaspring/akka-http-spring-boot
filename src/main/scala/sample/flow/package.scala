package sample

import akka.stream.scaladsl.{Flow}

package object flow {

  implicit class Operations[In, Out, Mat](val flow: Flow[In, Out, Mat]) {
    def movingAverage(size: Int)(implicit num: Numeric[Out]) = flow.transform(() => MovingAverage[Out](size)(num))
    def slidingWindow(size: Int) = flow.transform(() => SlidingWindow[Out](size))
    def slidingStatistics(size: Int)(implicit num: Numeric[Out]) = flow.transform(() => SlidingWindow[Out](size)).map(Statistics[Out](_)(num))
  }

//  implicit class WindowOperations[In, Out, Mat](val flow: Flow[In, Seq[Out], Mat]) {
//    def statistics(implicit num: Numeric[Out]) = flow.map(Statistics[Out](_)(num))
//  }

//  implicit class StatisticalOperations[In, N <% Numeric[N], Out <: Statistics[N], Mat](val flow: Flow[In, Out, Mat]) {
//    def bollinger = flow.map(Bollinger(_))
//  }

}
