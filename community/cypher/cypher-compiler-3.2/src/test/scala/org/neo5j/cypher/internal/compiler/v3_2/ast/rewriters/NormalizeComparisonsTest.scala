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

import org.neo5j.cypher.internal.frontend.v3_2.DummyPosition
import org.neo5j.cypher.internal.frontend.v3_2.ast.rewriters.normalizeComparisons
import org.neo5j.cypher.internal.frontend.v3_2.ast.{Equals, Expression, InvalidNotEquals, NotEquals, Variable, _}
import org.neo5j.cypher.internal.frontend.v3_2.test_helpers.CypherFunSuite

class NormalizeComparisonsTest extends CypherFunSuite {
  val pos = DummyPosition(0)
  val expression: Expression = Variable("foo")(pos)
  val comparisons = List(
    Equals(expression, expression)(pos),
    NotEquals(expression, expression)(pos),
    LessThan(expression, expression)(pos),
    LessThanOrEqual(expression, expression)(pos),
    GreaterThan(expression, expression)(pos),
    GreaterThanOrEqual(expression, expression)(pos),
    InvalidNotEquals(expression, expression)(pos)
  )

  comparisons.foreach { operator =>
    test(operator.toString) {
      val rewritten = operator.endoRewrite(normalizeComparisons)

      rewritten.lhs shouldNot be theSameInstanceAs rewritten.rhs
    }
  }
}
