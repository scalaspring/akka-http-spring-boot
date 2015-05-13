package sample.yahoo

import com.typesafe.scalalogging.StrictLogging
import org.scalactic.Tolerance._
import org.scalactic.TripleEqualsSupport.Spread
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.{FlatSpec, Matchers}
import sample.yahoo.StatisticsSpec.Expected


object StatisticsSpec {
  case class Expected(mean: Spread[Double], variance: Spread[Double], tolerance: Double) {
    val stddev = Math.sqrt(variance.pivot) +- tolerance
  }
  object Expected {
    def apply(mean: Double, variance: Double, tolerance: Double = 0.001) = new Expected(mean +- tolerance, variance +- tolerance, tolerance)
  }
}

class StatisticsSpec extends FlatSpec with Matchers with StrictLogging {

  val permutations = Table(
    ("input", "expected"),
    (
      List(1.0, 2.0, 3.0, 4.0, 5.0),
      Expected(3, 2.5)
    ),
    (
      List(-10.0, -5.0, 0.0, 10.0, 20.0),
      Expected(3, 145)
    )
  )


  "Sliding statistics stage" should "properly calculate all valid permutations" in {
    forAll(permutations) { (input: List[Double], expected: Expected) =>
      val stats = Statistics(input)

      stats.mean shouldBe expected.mean
      stats.variance shouldBe expected.variance
      stats.stddev shouldBe expected.stddev
    }
  }

}
