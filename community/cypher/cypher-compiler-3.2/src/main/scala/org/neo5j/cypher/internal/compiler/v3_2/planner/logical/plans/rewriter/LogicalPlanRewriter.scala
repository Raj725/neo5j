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
package org.neo5j.cypher.internal.compiler.v3_2.planner.logical.plans.rewriter

import org.neo5j.cypher.internal.compiler.v3_2.phases.{CompilationState, CompilerContext}
import org.neo5j.cypher.internal.frontend.v3_2.Rewriter
import org.neo5j.cypher.internal.frontend.v3_2.helpers.fixedPoint
import org.neo5j.cypher.internal.frontend.v3_2.helpers.rewriting.RewriterStepSequencer
import org.neo5j.cypher.internal.frontend.v3_2.phases.CompilationPhaseTracer.CompilationPhase
import org.neo5j.cypher.internal.frontend.v3_2.phases.CompilationPhaseTracer.CompilationPhase.LOGICAL_PLANNING
import org.neo5j.cypher.internal.frontend.v3_2.phases.{Condition, Phase}

/*
 * Rewriters that live here are required to adhere to the contract of
 * receiving a valid plan and producing a valid plan. It should be possible
 * to disable any and all of these rewriters, and still produce correct behavior.
 */
case class PlanRewriter(rewriterSequencer: String => RewriterStepSequencer) extends LogicalPlanRewriter {
  override def description: String = "optimize logical plans using heuristic rewriting"

  override def postConditions: Set[Condition] = Set.empty

  override def instance(context: CompilerContext) = fixedPoint(rewriterSequencer("LogicalPlanRewriter")(
    fuseSelections,
    unnestApply,
    cleanUpEager,
    simplifyEquality,
    unnestOptional,
    predicateRemovalThroughJoins,
    removeIdenticalPlans,
    pruningVarExpander,
    useTop
  ).rewriter)
}

trait LogicalPlanRewriter extends Phase[CompilerContext, CompilationState, CompilationState] {
  override def phase: CompilationPhase = LOGICAL_PLANNING

  def instance(context: CompilerContext): Rewriter

  override def process(from: CompilationState, context: CompilerContext): CompilationState = {
    val rewritten = from.logicalPlan.endoRewrite(instance(context))
    from.copy(maybeLogicalPlan = Some(rewritten))
  }
}
