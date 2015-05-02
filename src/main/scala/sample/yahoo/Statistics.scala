package sample.yahoo

trait Statistics[T] {
  val population: Boolean

  def mean: Double
  def variance: Double
  def stdDev: Double = Math.sqrt(variance)

  def apply(value: T): Statistics[T]
  def apply(values: Iterable[T]): Statistics[T]
}

object Statistics {

  def apply[T](values: Iterable[T], population: Boolean = false)(implicit num: Numeric[T]): Statistics[T] = new StatisticsImpl[T](population)(num)(values)

  private case class StatisticsImpl[T](population: Boolean = false, M: Double = 0, S: Double = 0, k: Int = 1)(implicit num: Numeric[T]) extends Statistics[T] {

    override def mean: Double = M
    override def variance: Double = if (population) (S / (k-1)) else (S / (k-2))

    // See: http://stackoverflow.com/questions/895929/how-do-i-determine-the-standard-deviation-stddev-of-a-set-of-values
    override def apply(value: T): Statistics[T] = {
      val x = num.toDouble(value)
      val newM = M + (x - M) / k
      val newS = (x - M) * (x - newM)
      val newK = k + 1

      copy(M = newM, S = newS, k = newK)
    }

    override def apply(values: Iterable[T]): Statistics[T] = values.foldLeft(this.asInstanceOf[Statistics[T]])((s, x) => s(x))
  }
}

