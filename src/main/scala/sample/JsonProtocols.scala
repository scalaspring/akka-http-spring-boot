package sample

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import sample.CreditCard.Brand
import spray.json._

import Sex._

trait JsonProtocols extends DefaultJsonProtocol {

  implicit object SexJsonFormat extends JsonFormat[Sex] {
    override def write(sex: Sex): JsValue = JsString(sex)
    override def read(value: JsValue): Sex = Sex.fromString(value.convertTo[String]).getOrElse { throw new DeserializationException(s"Invalid value: $value is not a valid code") }
  }

  implicit object BrandJsonFormat extends JsonFormat[Brand] {
    override def write(brand: Brand): JsValue = JsString(brand.asInstanceOf[Product].productPrefix)
    override def read(value: JsValue): Brand = Brand.map.get(value.convertTo[String]).getOrElse { throw new DeserializationException(s"Invalid brand: $value") }
  }

  implicit object LocalDateJsonFormat extends JsonFormat[LocalDate] {
    override def write(date: LocalDate): JsValue = JsString(DateTimeFormatter.ISO_LOCAL_DATE.format(date))
    override def read(value: JsValue): LocalDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(value.convertTo[String]))
  }

  implicit val nameFormat = jsonFormat2(Name.apply)
  implicit val credentialsFormat = jsonFormat2(Credentials.apply)
  implicit val creditCardFormat = jsonFormat3(CreditCard.apply)
  implicit val userFormat = jsonFormat7(User.apply)
  
}
