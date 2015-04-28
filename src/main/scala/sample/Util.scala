package sample

import com.github.tototoshi.csv.CSVReader

import scala.util.Random

object Util {

  def random(range: (Int, Int)): Int = (range._1 + Random.nextInt(range._2 - range._1))

  def random[T](seq: Seq[T]): T = seq(Random.nextInt(seq.size))

  def openCsvResource(resource: String) = CSVReader.open(scala.io.Source.fromURL(getClass.getResource(resource)).bufferedReader()).iteratorWithHeaders

}
