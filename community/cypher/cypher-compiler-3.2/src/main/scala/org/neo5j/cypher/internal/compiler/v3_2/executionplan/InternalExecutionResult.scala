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
package org.neo5j.cypher.internal.compiler.v3_2.executionplan

import java.io.PrintWriter
import java.util

import org.neo5j.cypher.internal.compiler.v3_2.planDescription.InternalPlanDescription
import org.neo5j.cypher.internal.compiler.v3_2.spi.InternalResultVisitor
import org.neo5j.cypher.internal.compiler.v3_2.{ExecutionMode, InternalQueryStatistics}
import org.neo5j.cypher.internal.frontend.v3_2.notification.InternalNotification
import org.neo5j.graphdb.ResourceIterator

trait InternalExecutionResult extends Iterator[Map[String, Any]] {

  def columns: List[String]
  def columnAs[T](column: String): Iterator[T]

  def javaColumns: java.util.List[String]
  def javaColumnAs[T](column: String): ResourceIterator[T]
  def javaIterator: ResourceIterator[java.util.Map[String, Any]]

  def dumpToString(writer: PrintWriter)
  def dumpToString(): String

  def queryStatistics(): InternalQueryStatistics

  def planDescriptionRequested: Boolean
  def executionPlanDescription(): InternalPlanDescription

  def executionType: InternalQueryType
  def executionMode: ExecutionMode

  def notifications: Iterable[InternalNotification]

  @throws(classOf[Exception])
  def accept[EX <: Exception](visitor: InternalResultVisitor[EX])

  def close()
}

