package sample.random

import scala.util.Random


sealed trait Sex { val sex: String }
case object Male extends Sex { val sex = "M" }
case object Female extends Sex { val sex = "F" }

object Sex {
  def random: Sex = random()
  def random(pF: Double = 0.5): Sex = if (Random.nextDouble > pF) Male else Female
  def iterator(f: => Sex = random()) = new Iterator[Sex] {
    override def hasNext: Boolean = true
    override def next(): Sex = f
  }

  implicit def toString(sex: Sex): String = sex.sex.toString
  implicit def fromString(s: String): Option[Sex] = s.toUpperCase match {
    case Male.sex => Some(Male)
    case Female.sex => Some(Female)
    case _ => None
  }

}

