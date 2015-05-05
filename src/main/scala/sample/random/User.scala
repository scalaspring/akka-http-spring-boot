package sample.random

import sample.util.Util

import scala.util.Random


case class Credentials(username: String, password: String)

object Credentials {
  def apply(name: Name): Credentials = Credentials(username = name.first + name.last, password = (name.first + name.last).reverse + "1")
}


case class User(
               name: Name,
               credentials: Credentials,
               sex: Sex,
               email: String,
               creditCard: CreditCard,
               height: Int,
               weight: Int
)

object User {

  // See: https://signup.weightwatchers.com/util/sig/healthy_weight_ranges_pop.aspx
  val weightRanges = Map[Int, (Int, Int)](
    56 ->(89, 112),
    57 ->(92, 116),
    58 ->(96, 120),
    59 ->(99, 124),
    60 ->(102, 128),
    61 ->(106, 132),
    62 ->(109, 137),
    63 ->(113, 141),
    64 ->(117, 146),
    65 ->(120, 150),
    66 ->(124, 155),
    67 ->(128, 160),
    68 ->(132, 164),
    69 ->(135, 169),
    70 ->(139, 174),
    71 ->(143, 179),
    72 ->(147, 184),
    73 ->(152, 189),
    74 ->(156, 195),
    75 ->(160, 200),
    76 ->(164, 205),
    77 ->(169, 211),
    78 ->(173, 216),
    79 ->(178, 222)
  )

  val domains = Seq(
    "hotmail.com",
    "gmail.com",
    "yahoo.com",
    "outlook.com"
  )

  val (minHeight, maxHeight) = (weightRanges.keySet.min, weightRanges.keySet.max)


  protected def randomHeight: Int = Util.random(minHeight, maxHeight)

  protected def randomWeight(height: Int) = weightRanges(height) match {
    case (min, max) => Util.random(min, max)
  }

  protected def randomEmail(name: Name) = s"${name.first}.${name.last}@${domains(Random.nextInt(domains.size))}".toLowerCase

  def random: User = {
    val sex = Sex.random
    val name = Name.random(sex)
    val height = randomHeight
    new User(
      name = name,
      credentials = Credentials(name),
      sex = sex,
      email = randomEmail(name),
      creditCard = CreditCard.random,
      height = height,
      weight = randomWeight(height)
    )
  }

}