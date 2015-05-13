package sample.random

import java.time.LocalDate

import sample.random.CreditCard.Brand
import sample.util
import scala.util.Random

case class CreditCard(brand: Brand, number: String, expiration: LocalDate)

object CreditCard {

  sealed trait Brand
  case object Visa extends Brand
  case object MasterCard extends Brand
  case object Amex extends Brand
  case object Discover extends Brand

  object Brand {

    val map: Map[String, Brand] = Seq(Visa, MasterCard, Amex, Discover).foldLeft(Map[String, Brand]()) { (m, v) => (m + (toString(v) -> v)) }

    implicit def toString(brand: Brand): String = brand.asInstanceOf[Product].productPrefix
    implicit def fromString(s: String): Option[Brand] = map.get(s.capitalize)

  }

  val prefixes: Map[Brand, Seq[String]] = Map(
    Visa -> Seq("4"),
    MasterCard -> Seq("51", "52", "53", "54", "55"),
    Amex -> Seq("34", "37"),
    Discover -> Seq("6011")
  )
  val lengths: Map[Brand, Int] = Map(
    Visa -> 16,
    MasterCard -> 16,
    Amex -> 15,
    Discover -> 16
  )
  val brands = prefixes.keySet.toSeq


  def random: CreditCard = random(randomBrand)
  def random(brand: Brand): CreditCard = CreditCard(brand = brand, number = randomNumber(randomPrefix(brand), lengths(brand)), randomExpiration())


  protected def randomBrand: Brand = util.random(brands)

  protected def randomPrefix(brand: Brand): String = util.random(prefixes(brand))

  protected def randomNumber(prefix: String, length: Int): String = {

    val digits: Seq[Int] = prefix.map(_.asDigit) ++ ((0 until (length - prefix.length - 1)).map(_ => Random.nextInt(9)))
    val reversed = digits.reverse
    val sum = reversed.indices.foldLeft(0)((s, i) => {
      val digit = reversed(i)
      i % 2 match {
        case 0 => s + (if (digit > 4) ((digit * 2) - 9) else (digit * 2))
        case 1 => s + digit
      }
    })
    val checkdigit: Int = 10 - (sum % 10)

    digits.foldLeft(new StringBuilder(digits.size + 1))((s, d) => s.append(d)).append(checkdigit).toString
  }

  protected def randomExpiration(days: (Int, Int) = (3 * 30, 36 * 30)): LocalDate = LocalDate.now().plusDays(util.random(days))

}

