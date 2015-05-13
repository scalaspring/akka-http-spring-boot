package sample.yahoo

import akka.stream.scaladsl.Flow

package object stage {

  implicit class Operations[In, Out, Mat](val flow: Flow[In, Out, Mat]) {
    def sliding(size: Int) = flow.transform(() => Sliding[Out](size))
    def slidingStatistics(size: Int)(implicit num: Numeric[Out]) = flow.transform(() => Sliding[Out](size)).map(Statistics[Out](_)(num))
  }

}
