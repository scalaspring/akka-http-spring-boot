package sample.flow

case class Bollinger(lower: Double, middle: Double, upper: Double)

object Bollinger {
  def apply(s: Statistics[_]): Bollinger = new Bollinger(s.mean - 2 * s.stddev, s.mean, s.mean + 2 * s.stddev)
}