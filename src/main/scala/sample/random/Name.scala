package sample.random

import sample.util


case class Name(first: String, last: String)

object Name {

  lazy val firstNames: Map[Sex, Seq[String]] = util.openCsvResource("/random/user/first_names.csv").partition({
    case r if r("sex") == "M" => true; case _ => false }) match {
    case (m, f) => Map(
      Male -> m.map(_("name").toLowerCase.capitalize).toList,
      Female -> f.map(_("name").toLowerCase.capitalize).toList
    )
  }
  lazy val lastNames = util.openCsvResource("/random/user/last_names.csv").map(_("name").toLowerCase.capitalize).toList


  def random: Name = random(Sex.random)
  def random(sex: Sex) = new Name(first = util.random(firstNames(sex)), last = util.random(lastNames))

}
