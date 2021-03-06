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
package org.neo5j.cypher.internal.compiler.v3_2.planner.logical.steps

import org.neo5j.cypher.internal.compiler.v3_2.planner.logical._
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.plans.LogicalPlan
import org.neo5j.cypher.internal.frontend.v3_2.ast.{Expression, Ors}
import org.neo5j.cypher.internal.frontend.v3_2.helpers.SeqCombiner.combine
import org.neo5j.cypher.internal.ir.v3_2.QueryGraph

case class OrLeafPlanner(inner: Seq[LeafPlanFromExpressions]) extends LeafPlanner {

  override def apply(qg: QueryGraph)(implicit context: LogicalPlanningContext): Seq[LogicalPlan] = {
    qg.selections.flatPredicates.flatMap {
      case orPredicate@Ors(exprs) =>

        // This is a Seq of possible solutions per expression
        val plansPerExpression: Seq[Seq[LeafPlansForVariable]] = exprs.toSeq.map { e =>
          inner.flatMap(_.producePlanFor(Set(e), qg))
        }

        if (plansPerExpression.exists(_.isEmpty) || {
          val id = plansPerExpression.head.head.id

          plansPerExpression.exists(leafs => leafs.exists(_.id != id))
        }) {
          // If we either didn't find a good plan for one of the expressions,
          // or we are mixing plans for different variables, we bail out early.
          Seq.empty
        } else {
          val combination: Seq[Seq[LeafPlansForVariable]] = combine(plansPerExpression)
          val step2: Seq[Seq[LogicalPlan]] = combination.map(_.flatMap(_.plans))

          val producer = context.logicalPlanProducer
          step2.flatMap {
            case plans if plans.isEmpty =>
              None
            case plans =>
              // We need to collect the predicates to be able to update solved correctly. After finishing to build the
              // OR plan, we will report solving the OR predicate, but also other predicates solved.
              val predicates = collection.mutable.HashSet[Expression]()
              val singlePlan = plans.reduce[LogicalPlan] {
                case (p1, p2) =>
                  predicates ++= p1.solved.tailOrSelf.queryGraph.selections.flatPredicates
                  predicates ++= p2.solved.tailOrSelf.queryGraph.selections.flatPredicates
                  producer.planUnion(p1, p2)
              }
              val orPlan = context.logicalPlanProducer.planDistinct(singlePlan)

              Some(context.logicalPlanProducer.updateSolvedForOr(orPlan, orPredicate, predicates.toSet))
          }
        }

      case _ => Seq.empty
    }
  }

}
