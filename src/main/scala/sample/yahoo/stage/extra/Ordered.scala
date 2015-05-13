package sample.yahoo.stage.extra

import akka.stream.stage.{Context, PushStage, SyncDirective}


/**
 * Enforces ordering on a stream, supporting ascending/descending and strict/non-strict options.
 *
 * This push stage accepts elements of type `T` and emits elements of type `Either[T, T]`.
 * Emitted elements will be of type `Right` if properly ordered or of type `Left` otherwise.
 * The stream of Right elements is guaranteed to have proper ordering semantics.
 *
 * For example, given ascending strict ordering, this stage will emit the following sequence of elements for the given stream.
 * Note that the second C element is rejected since ordering only considers valid elements, not invalid intermediate elements.
 *
 * In:
 * {{{ A, B, C, B, C, D }}}
 *
 * Out:
 * {{{ Right(A) Right(B), Right(C), Left(B), Left(C), Right(D) }}}
 *
 * @param ascending
 * @param strict
 * @tparam T
 */
class Ordered[T](ascending: Boolean = true, strict: Boolean = true)(implicit ordering: Ordering[T]) extends PushStage[T, Either[T, T]] {

  var last: Option[T] = None

  def isOrdered(last: T, cur: T) =
    if (ascending)
      if (strict) ordering.lt(last, cur)
      else ordering.lteq(last, cur)
    else
    if (strict) ordering.gt(last, cur)
    else ordering.gteq(last, cur)

  override def onPush(elem: T, ctx: Context[Either[T, T]]): SyncDirective = {
    val result = last.fold[Either[T, T]](Right(elem))(last => Either.cond(isOrdered(last, elem), elem, elem))
    if (result.isRight) last = Some(elem)
    ctx.push(result)
  }

}

object Ordered {
  def apply[T](ascending: Boolean = true, strict: Boolean = true)(implicit ordering: Ordering[T]) = new Ordered[T](ascending, strict)(ordering)
}