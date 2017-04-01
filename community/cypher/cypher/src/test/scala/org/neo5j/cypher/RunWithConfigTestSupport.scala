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
package org.neo5j.cypher

import java.util

import org.neo5j.cypher.javacompat.internal.GraphDatabaseCypherService
import org.neo5j.graphdb.config.Setting
import org.neo5j.test.TestGraphDatabaseFactory

import scala.collection.JavaConverters._

trait RunWithConfigTestSupport {
  def runWithConfig(m: (Setting[_], String)*)(run: GraphDatabaseCypherService => Unit) = {
    val config: util.Map[Setting[_], String] = m.toMap.asJava
    val graph = new TestGraphDatabaseFactory().newImpermanentDatabase(config)
    try {
      run(new GraphDatabaseCypherService(graph))
    } finally {
      graph.shutdown()
    }
  }
}