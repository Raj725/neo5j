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
package org.neo5j.cypher.internal.compatibility.v3_1

import java.time.Clock

import org.neo5j.cypher.internal.compiler.v3_1.{CypherCompilerConfiguration, CypherCompilerFactory}
import org.neo5j.kernel.GraphDatabaseQueryService
import org.neo5j.kernel.api.KernelAPI
import org.neo5j.kernel.monitoring.{Monitors => KernelMonitors}

case class RuleCompatibility(graph: GraphDatabaseQueryService,
                             config: CypherCompilerConfiguration,
                             clock: Clock,
                             kernelMonitors: KernelMonitors,
                             kernelAPI: KernelAPI) extends Compatibility {
  protected val compiler = {
    val monitors = WrappedMonitors(kernelMonitors)
    CypherCompilerFactory.ruleBasedCompiler(graph, config, clock, monitors, rewriterSequencer, typeConversions)
  }

  override val queryCacheSize: Int = config.queryCacheSize
}
