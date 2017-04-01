/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.internal.cypher.acceptance

import org.neo5j.collection.RawIterator
import org.neo5j.cypher._
import org.neo5j.kernel.api.exceptions.ProcedureException
import org.neo5j.kernel.api.proc.CallableProcedure.BasicProcedure
import org.neo5j.kernel.api.proc.CallableUserAggregationFunction.{Aggregator, BasicUserAggregationFunction}
import org.neo5j.kernel.api.proc.CallableUserFunction.BasicUserFunction
import org.neo5j.kernel.api.proc.ProcedureSignature.procedureSignature
import org.neo5j.kernel.api.proc.UserFunctionSignature.functionSignature
import org.neo5j.kernel.api.proc.{Context, Neo5jTypes, ProcedureSignature}

abstract class ProcedureCallAcceptanceTest extends ExecutionEngineFunSuite {

  protected def registerDummyInOutProcedure(types: Neo5jTypes.AnyType*) =
    registerProcedure("my.first.proc") { builder =>

      for (i <- types.indices) {
        builder
          .in(s"in$i", types(i))
          .out(s"out$i", types(i))
      }

      new BasicProcedure(builder.build) {
        override def apply(ctx: Context, input: Array[AnyRef]): RawIterator[Array[AnyRef], ProcedureException] =
          RawIterator.of[Array[AnyRef], ProcedureException](input)
      }
  }

  protected def registerProcedureReturningSingleValue(value: AnyRef) =
    registerProcedure("my.first.value") { builder =>
      val builder = procedureSignature(Array("my", "first"), "value")
      builder.out("out", Neo5jTypes.NTAny)

      new BasicProcedure(builder.build) {
        override def apply(ctx: Context, input: Array[AnyRef]): RawIterator[Array[AnyRef], ProcedureException] =
          RawIterator.of[Array[AnyRef], ProcedureException](Array(value))
      }
    }

  protected def registerUserFunction(value: AnyRef, typ: Neo5jTypes.AnyType = Neo5jTypes.NTAny) =
    registerUserDefinedFunction("my.first.value") { builder =>
      val builder = functionSignature(Array("my", "first"), "value")
      builder.out(typ)

      new BasicUserFunction(builder.build) {
        override def apply(ctx: Context, input: Array[AnyRef]): AnyRef = value
      }
    }

  protected def registerUserAggregationFunction(value: AnyRef, typ: Neo5jTypes.AnyType = Neo5jTypes.NTAny) =
    registerUserDefinedAggregationFunction("my.first.value") { builder =>
      val builder = functionSignature(Array("my", "first"), "value")
      builder.out(typ)

      new BasicUserAggregationFunction(builder.build) {

        override def create(ctx: Context): Aggregator = new Aggregator {

          override def result() = value

          override def update(input: Array[AnyRef]) = {}
        }
      }
    }


  protected def registerVoidProcedure() =
    registerProcedure("dbms.do_nothing") { builder =>
      builder.out(ProcedureSignature.VOID)

      new BasicProcedure(builder.build) {
        override def apply(ctx: Context, input: Array[AnyRef]): RawIterator[Array[AnyRef], ProcedureException] =
          RawIterator.empty()
      }
    }

  protected def registerProcedureReturningNoRowsOrColumns() =
    registerProcedure("dbms.return_nothing") { builder =>
      new BasicProcedure(builder.build) {
        override def apply(ctx: Context, input: Array[AnyRef]): RawIterator[Array[AnyRef], ProcedureException] =
          RawIterator.empty()
      }
    }
}
