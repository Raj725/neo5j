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
package org.neo5j.cypher.internal.compiler.v3_2.pipes

import org.neo5j.cypher.internal.compiler.v3_2.ExecutionContext
import org.neo5j.cypher.internal.compiler.v3_2.planDescription.Id

case class LetSemiApplyPipe(source: Pipe, inner: Pipe, letVarName: String, negated: Boolean)
                           (val id: Id = new Id)
                           (implicit pipeMonitor: PipeMonitor) extends PipeWithSource(source, pipeMonitor) {
  def internalCreateResults(input: Iterator[ExecutionContext], state: QueryState): Iterator[ExecutionContext] = {
    input.map {
      (outerContext) =>
        val innerState = state.withInitialContext(outerContext)
        val innerResults = inner.createResults(innerState)
        val holds = if (negated) innerResults.isEmpty else innerResults.nonEmpty
        outerContext += (letVarName -> holds)
    }
  }

  private def name = if (negated) "LetAntiSemiApply" else "LetSemiApply"
}
