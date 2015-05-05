package sample.flow


case class Statistics[T](population: Boolean, M: Double, S: Double, k: Int)(implicit num: Numeric[T]) {

  def apply(values: Iterable[T]): Statistics[T] = values.foldLeft(this)((s, x) => s(x))

  // See: http://stackoverflow.com/questions/895929/how-do-i-determine-the-standard-deviation-stddev-of-a-set-of-values
  def apply(value: T): Statistics[T] = {
    val x = num.toDouble(value)
    val newM = M + (x - M) / k
    val newS = S + (x - newM) * (x - M)
    val newK = k + 1

    copy(M = newM, S = newS, k = newK)
  }


  def mean: Double = M
  def variance: Double = if (k < 3) 0 else if (population) (S / (k-1)) else (S / (k-2))
  def stddev: Double = Math.sqrt(variance)

}

object Statistics {
  def apply[T](values: Iterable[T] = Nil, population: Boolean = false)(implicit num: Numeric[T]) = (new Statistics[T](population, 0, 0, 1)(num))(values)
}