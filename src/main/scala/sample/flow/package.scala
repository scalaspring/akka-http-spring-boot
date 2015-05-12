package sample

import akka.stream.scaladsl.{Flow}

package object flow {

  implicit class Operations[In, Out, Mat](val flow: Flow[In, Out, Mat]) {
    def movingAverage(size: Int)(implicit num: Numeric[Out]) = flow.transform(() => MovingAverage[Out](size)(num))
    def slidingWindow(size: Int) = flow.transform(() => Sliding[Out](size))
    def slidingStatistics(size: Int)(implicit num: Numeric[Out]) = flow.transform(() => Sliding[Out](size)).map(Statistics[Out](_)(num))
  }

}
