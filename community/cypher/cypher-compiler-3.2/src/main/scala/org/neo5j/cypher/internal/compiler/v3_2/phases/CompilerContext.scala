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
package org.neo5j.cypher.internal.compiler.v3_2.phases

import java.time.Clock

import org.neo5j.cypher.internal.compiler.v3_2._
import org.neo5j.cypher.internal.compiler.v3_2.executionplan.{PlanFingerprint, PlanFingerprintReference}
import org.neo5j.cypher.internal.compiler.v3_2.helpers.RuntimeTypeConverter
import org.neo5j.cypher.internal.compiler.v3_2.planner.logical.{Metrics, QueryGraphSolver}
import org.neo5j.cypher.internal.compiler.v3_2.spi.PlanContext
import org.neo5j.cypher.internal.frontend.v3_2.phases.{BaseContext, CompilationPhaseTracer, InternalNotificationLogger, Monitors}
import org.neo5j.cypher.internal.frontend.v3_2.{CypherException, InputPosition}

class CompilerContext(val exceptionCreator: (String, InputPosition) => CypherException,
                      val tracer: CompilationPhaseTracer,
                      val notificationLogger: InternalNotificationLogger,
                      val planContext: PlanContext,
                      val typeConverter: RuntimeTypeConverter,
                      val createFingerprintReference: Option[PlanFingerprint] => PlanFingerprintReference,
                      val monitors: Monitors,
                      val metrics: Metrics,
                      val queryGraphSolver: QueryGraphSolver,
                      val config: CypherCompilerConfiguration,
                      val updateStrategy: UpdateStrategy,
                      val debugOptions: Set[String],
                      val clock: Clock) extends BaseContext