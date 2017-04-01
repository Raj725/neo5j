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
package org.neo5j.causalclustering.load_balancing.procedure;

import java.util.Map;

import org.neo5j.causalclustering.load_balancing.LoadBalancingProcessor;
import org.neo5j.collection.RawIterator;
import org.neo5j.kernel.api.exceptions.ProcedureException;
import org.neo5j.kernel.api.proc.CallableProcedure;
import org.neo5j.kernel.api.proc.Context;
import org.neo5j.kernel.api.proc.Neo5jTypes;
import org.neo5j.kernel.api.proc.ProcedureSignature;

import static org.neo5j.causalclustering.load_balancing.procedure.ParameterNames.CONTEXT;
import static org.neo5j.causalclustering.load_balancing.procedure.ParameterNames.SERVERS;
import static org.neo5j.causalclustering.load_balancing.procedure.ParameterNames.TTL;
import static org.neo5j.causalclustering.load_balancing.procedure.ProcedureNames.GET_SERVERS_V2;
import static org.neo5j.kernel.api.proc.ProcedureSignature.procedureSignature;

/**
 * Returns endpoints and their capabilities.
 *
 * GetServersV2 extends upon V1 by allowing a client context consisting of
 * key-value pairs to be supplied to and used by the concrete load
 * balancing strategies.
 */
public class GetServersProcedureForMultiDC implements CallableProcedure
{
    private final String DESCRIPTION = "Returns cluster endpoints and their capabilities.";

    private final ProcedureSignature procedureSignature =
            procedureSignature( GET_SERVERS_V2.fullyQualifiedProcedureName() )
                    .in( CONTEXT.parameterName(), Neo5jTypes.NTMap )
                    .out( TTL.parameterName(), Neo5jTypes.NTInteger )
                    .out( SERVERS.parameterName(), Neo5jTypes.NTMap )
                    .description( DESCRIPTION )
                    .build();

    private final LoadBalancingProcessor loadBalancingProcessor;

    public GetServersProcedureForMultiDC( LoadBalancingProcessor loadBalancingProcessor )
    {
        this.loadBalancingProcessor = loadBalancingProcessor;
    }

    @Override
    public ProcedureSignature signature()
    {
        return procedureSignature;
    }

    @Override
    public RawIterator<Object[],ProcedureException> apply( Context ctx, Object[] input ) throws ProcedureException
    {
        @SuppressWarnings( "unchecked" )
        Map<String,String> clientContext = (Map<String,String>) input[0];

        LoadBalancingProcessor.Result result = loadBalancingProcessor.run( clientContext );

        return RawIterator.<Object[],ProcedureException>of( ResultFormatV1.build( result ) );
    }
}
