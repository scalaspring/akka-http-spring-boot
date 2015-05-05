package sample.flow

import akka.stream.stage.{Context, PushStage, SyncDirective}

case class Bollinger() extends PushStage[Statistics[_], BollingerPoints] {
  override def onPush(elem: Statistics[_], ctx: Context[BollingerPoints]): SyncDirective = ctx.push(BollingerPoints(elem))
}

case class BollingerPoints(lower: Double, middle: Double, upper: Double)

object BollingerPoints {
  def apply(s: Statistics[_]): BollingerPoints = BollingerPoints(s.mean - 2 * s.stddev, s.mean, s.mean + 2 * s.stddev)
}