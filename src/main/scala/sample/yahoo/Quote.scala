package sample.yahoo

import java.time.LocalDate

import scala.collection.mutable

object Quote {

  def apply(fields: Map[String, String] = Map.empty): Quote = new mutable.LinkedHashMap() ++ fields

  implicit def quoteToLocalDate(quote: Quote): LocalDate = LocalDate.parse(quote("Date"))

  implicit val QuoteOrdering: Ordering[Quote] = new Ordering[Quote] {
    override def compare(x: Quote, y: Quote): Int = quoteToLocalDate(x).compareTo(quoteToLocalDate(y))
  }

}
