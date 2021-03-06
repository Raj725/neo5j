/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.cypher.internal.compiler.v3_2.ast.rewriters

import org.neo5j.cypher.internal.frontend.v3_2.{bottomUp, Rewriter}
import org.neo5j.cypher.internal.frontend.v3_2.ast._
import org.neo5j.cypher.internal.frontend.v3_2.ast.Return

case object reattachAliasedExpressions extends Rewriter {
  override def apply(in: AnyRef): AnyRef = findingRewriter.apply(in)

  private val findingRewriter: Rewriter = bottomUp(Rewriter.lift {
    case clause: Return =>
      val innerRewriter = expressionRewriter(clause.returnItems.items)
      clause.copy(
        orderBy = clause.orderBy.endoRewrite(innerRewriter)
      )(clause.position)

    case clause: With =>
      val innerRewriter = expressionRewriter(clause.returnItems.items)
      clause.copy(
        orderBy = clause.orderBy.endoRewrite(innerRewriter)
      )(clause.position)
  })

  private def expressionRewriter(items: Seq[ReturnItem]): Rewriter = {
    val aliasedExpressions: Map[String, Expression] = items.map { returnItem =>
      (returnItem.name, returnItem.expression)
    }.toMap

    bottomUp(Rewriter.lift {
      case id@Variable(name) if aliasedExpressions.contains(name) => aliasedExpressions(name)
    })
  }
}
