package sample.yahoo

case class Bollinger(lower: Double, middle: Double, upper: Double)

object Bollinger {
  def apply(s: Statistics[_]): Bollinger = new Bollinger(s.mean - 2 * s.stddev, s.mean, s.mean + 2 * s.stddev)

  implicit class BollingerQuote(val q: Quote) {
    // Merge Bollinger points into a quote
    def ++(b: Bollinger, fmt: Double => String = "%.2f".format(_)): Quote =
      q += ("BB Lower" -> fmt(b.lower)) += ("BB Middle" -> fmt(b.middle)) += ("BB Upper" -> fmt(b.upper))
  }
}