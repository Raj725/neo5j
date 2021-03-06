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
package org.neo5j.cypher.internal.frontend.v3_2.phases

import org.neo5j.cypher.internal.frontend.v3_2.ast.rewriters.replaceAliasedFunctionInvocations.aliases
import org.neo5j.cypher.internal.frontend.v3_2.ast.{FunctionInvocation, FunctionName, Statement}
import org.neo5j.cypher.internal.frontend.v3_2.notification.{DeprecatedFunctionNotification, InternalNotification}
import org.neo5j.cypher.internal.frontend.v3_2.phases.CompilationPhaseTracer.CompilationPhase.DEPRECATION_WARNINGS

object SyntaxDeprecationWarnings extends VisitorPhase[BaseContext, BaseState] {
  override def visit(state: BaseState, context: BaseContext): Unit = {
    val warnings = findDeprecations(state.statement())

    warnings.foreach(context.notificationLogger.log)
  }

  private def findDeprecations(statement: Statement): Set[InternalNotification] =
    statement.treeFold(Set.empty[InternalNotification]) {
      case f@FunctionInvocation(_, FunctionName(name), _, _) if aliases.get(name).nonEmpty =>
        (seq) => (seq + DeprecatedFunctionNotification(f.position, name, aliases(name)), None)
    }

  override def phase = DEPRECATION_WARNINGS

  override def description = "find deprecated Cypher constructs and generate warnings for them"
}
