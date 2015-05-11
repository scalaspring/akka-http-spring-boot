package sample

import java.time.LocalDate

import scala.collection.mutable

package object yahoo {

  type Quote = mutable.Map[String, String]

  implicit def quoteToLocalDate(quote: Quote): LocalDate = LocalDate.parse(quote("Date"))

  implicit val QuoteOrdering: Ordering[Quote] = new Ordering[Quote] {
    override def compare(x: Quote, y: Quote): Int = quoteToLocalDate(x).compareTo(quoteToLocalDate(y))
  }

}
