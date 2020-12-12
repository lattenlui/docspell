package docspell.store.qb.impl

import docspell.store.qb._

import _root_.doobie.implicits._
import _root_.doobie.{Query => _, _}

object ConditionBuilder {
  val or         = fr"OR"
  val and        = fr"AND"
  val comma      = fr","
  val parenOpen  = Fragment.const0("(")
  val parenClose = Fragment.const0(")")

  def build(expr: Condition): Fragment =
    expr match {
      case c @ Condition.CompareVal(col, op, value) =>
        val opFrag  = operator(op)
        val valFrag = buildValue(value)(c.P)
        val colFrag = op match {
          case Operator.LowerLike =>
            lower(col)
          case _ =>
            SelectExprBuilder.column(col)
        }
        colFrag ++ opFrag ++ valFrag

      case c @ Condition.CompareFVal(dbf, op, value) =>
        val opFrag  = operator(op)
        val valFrag = buildValue(value)(c.P)
        val dbfFrag = op match {
          case Operator.LowerLike =>
            lower(dbf)
          case _ =>
            DBFunctionBuilder.build(dbf)
        }
        dbfFrag ++ opFrag ++ valFrag

      case Condition.CompareCol(c1, op, c2) =>
        val (c1Frag, c2Frag) = op match {
          case Operator.LowerLike =>
            (lower(c1), lower(c2))
          case _ =>
            (SelectExprBuilder.column(c1), SelectExprBuilder.column(c2))
        }
        c1Frag ++ operator(op) ++ c2Frag

      case Condition.InSubSelect(col, subsel) =>
        val sub = DoobieQuery(subsel)
        SelectExprBuilder.column(col) ++ sql" IN (" ++ sub ++ parenClose

      case c @ Condition.InValues(col, values, toLower) =>
        val cfrag = if (toLower) lower(col) else SelectExprBuilder.column(col)
        cfrag ++ sql" IN (" ++ values.toList
          .map(a => buildValue(a)(c.P))
          .reduce(_ ++ comma ++ _) ++ parenClose

      case Condition.IsNull(col) =>
        SelectExprBuilder.column(col) ++ fr" is null"

      case Condition.And(c, cs) =>
        val inner = cs.prepended(c).map(build).reduce(_ ++ and ++ _)
        if (cs.isEmpty) inner
        else parenOpen ++ inner ++ parenClose

      case Condition.Or(c, cs) =>
        val inner = cs.prepended(c).map(build).reduce(_ ++ or ++ _)
        if (cs.isEmpty) inner
        else parenOpen ++ inner ++ parenClose

      case Condition.Not(Condition.IsNull(col)) =>
        SelectExprBuilder.column(col) ++ fr" is not null"

      case Condition.Not(c) =>
        fr"NOT" ++ build(c)
    }

  def operator(op: Operator): Fragment =
    op match {
      case Operator.Eq =>
        fr" ="
      case Operator.Neq =>
        fr" <>"
      case Operator.Gt =>
        fr" >"
      case Operator.Lt =>
        fr" <"
      case Operator.Gte =>
        fr" >="
      case Operator.Lte =>
        fr" <="
      case Operator.LowerLike =>
        fr" LIKE"
    }

  def buildValue[A: Put](v: A): Fragment =
    fr"$v"

  def buildOptValue[A: Put](v: Option[A]): Fragment =
    fr"$v"

  def lower(col: Column[_]): Fragment =
    Fragment.const0("LOWER(") ++ SelectExprBuilder.column(col) ++ parenClose

  def lower(dbf: DBFunction): Fragment =
    Fragment.const0("LOWER(") ++ DBFunctionBuilder.build(dbf) ++ parenClose
}