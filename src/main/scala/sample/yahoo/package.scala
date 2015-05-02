package sample

import java.time.LocalDate

package object yahoo {

  type Quote = Map[String, String]

  implicit def quoteToLocalDate(quote: Quote): LocalDate = LocalDate.parse(quote("Date"))

  implicit val QuoteOrdering: Ordering[Quote] = new Ordering[Quote] {
    override def compare(x: Quote, y: Quote): Int = quoteToLocalDate(x).compareTo(quoteToLocalDate(y))
  }

}
