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
package org.neo5j.causalclustering.discovery.procedures;

import org.neo5j.collection.RawIterator;
import org.neo5j.kernel.api.exceptions.ProcedureException;
import org.neo5j.kernel.api.proc.CallableProcedure;
import org.neo5j.kernel.api.proc.Context;
import org.neo5j.kernel.api.proc.Neo5jTypes;
import org.neo5j.kernel.api.proc.QualifiedName;

import static org.neo5j.kernel.api.proc.ProcedureSignature.procedureSignature;

abstract class RoleProcedure extends CallableProcedure.BasicProcedure
{
    private static final String PROCEDURE_NAME = "role";
    private static final String[] PROCEDURE_NAMESPACE = {"dbms", "cluster"};
    private static final String OUTPUT_NAME = "role";

    RoleProcedure()
    {
        super( procedureSignature( new QualifiedName( PROCEDURE_NAMESPACE, PROCEDURE_NAME ) )
                .out( OUTPUT_NAME, Neo5jTypes.NTString )
                .description( "The role of a specific instance in the cluster." )
                .build() );
    }

    @Override
    public RawIterator<Object[],ProcedureException> apply( Context ctx, Object[] input ) throws ProcedureException
    {
        return RawIterator.<Object[],ProcedureException>of( new Object[]{role().name()} );
    }

    abstract Role role();
}
