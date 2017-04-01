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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.neo5j.causalclustering.core.consensus.LeaderLocator;
import org.neo5j.causalclustering.core.consensus.NoLeaderFoundException;
import org.neo5j.causalclustering.discovery.ClientConnectorAddresses;
import org.neo5j.causalclustering.discovery.CoreServerInfo;
import org.neo5j.causalclustering.discovery.CoreTopology;
import org.neo5j.causalclustering.discovery.ReadReplicaInfo;
import org.neo5j.causalclustering.discovery.TopologyService;
import org.neo5j.causalclustering.identity.MemberId;
import org.neo5j.collection.RawIterator;
import org.neo5j.kernel.api.exceptions.ProcedureException;
import org.neo5j.kernel.api.proc.CallableProcedure;
import org.neo5j.kernel.api.proc.Context;
import org.neo5j.kernel.api.proc.Neo5jTypes;
import org.neo5j.kernel.api.proc.QualifiedName;
import org.neo5j.logging.Log;
import org.neo5j.logging.LogProvider;

import static java.util.Comparator.comparing;
import static org.neo5j.helpers.collection.Iterables.asList;
import static org.neo5j.helpers.collection.Iterators.asRawIterator;
import static org.neo5j.helpers.collection.Iterators.map;
import static org.neo5j.kernel.api.proc.ProcedureSignature.procedureSignature;

/**
 * Overview procedure with added support for server groups.
 */
public class ClusterOverviewProcedure extends CallableProcedure.BasicProcedure
{
    private static final String[] PROCEDURE_NAMESPACE = {"dbms", "cluster"};
    public static final String PROCEDURE_NAME = "overview";
    private final TopologyService topologyService;
    private final LeaderLocator leaderLocator;
    private final Log log;

    public ClusterOverviewProcedure( TopologyService topologyService,
            LeaderLocator leaderLocator, LogProvider logProvider )
    {
        super( procedureSignature( new QualifiedName( PROCEDURE_NAMESPACE, PROCEDURE_NAME ) )
                .out( "id", Neo5jTypes.NTString )
                .out( "addresses", Neo5jTypes.NTList( Neo5jTypes.NTString ) )
                .out( "role", Neo5jTypes.NTString )
                .out( "groups", Neo5jTypes.NTList( Neo5jTypes.NTString ) )
                .description( "Overview of all currently accessible cluster members and their roles." )
                .build() );
        this.topologyService = topologyService;
        this.leaderLocator = leaderLocator;
        this.log = logProvider.getLog( getClass() );
    }

    @Override
    public RawIterator<Object[],ProcedureException> apply( Context ctx, Object[] input ) throws ProcedureException
    {
        List<ReadWriteEndPoint> endpoints = new ArrayList<>();
        CoreTopology coreTopology = topologyService.coreServers();
        Set<MemberId> coreMembers = coreTopology.members().keySet();
        MemberId leader = null;

        try
        {
            leader = leaderLocator.getLeader();
        }
        catch ( NoLeaderFoundException e )
        {
            log.debug( "No write server found. This can happen during a leader switch." );
        }

        for ( MemberId memberId : coreMembers )
        {
            Optional<CoreServerInfo> coreServerInfo = coreTopology.find( memberId );
            if ( coreServerInfo.isPresent() )
            {
                Role role = memberId.equals( leader ) ? Role.LEADER : Role.FOLLOWER;
                endpoints.add( new ReadWriteEndPoint( coreServerInfo.get().connectors(), role, memberId.getUuid(), asList( coreServerInfo.get().groups() ) ) );
            }
            else
            {
                log.debug( "No Address found for " + memberId );
            }
        }

        for ( Map.Entry<MemberId,ReadReplicaInfo> readReplica : topologyService.readReplicas().members().entrySet() )
        {
            ReadReplicaInfo readReplicaInfo = readReplica.getValue();
            endpoints.add( new ReadWriteEndPoint( readReplicaInfo.connectors(), Role.READ_REPLICA, readReplica.getKey().getUuid(), asList( readReplicaInfo.groups() ) ) );
        }

        endpoints.sort( comparing( o -> o.addresses().toString() ) );

        return map( ( endpoint ) -> new Object[]
                        {
                                endpoint.memberId().toString(),
                                endpoint.addresses().uriList().stream().map( URI::toString ).toArray(),
                                endpoint.role().name(),
                                endpoint.groups()
                        },
                asRawIterator( endpoints.iterator() ) );
    }

    static class ReadWriteEndPoint
    {
        private final ClientConnectorAddresses clientConnectorAddresses;
        private final Role role;
        private final UUID memberId;
        private final List<String> groups;

        public ClientConnectorAddresses addresses()
        {
            return clientConnectorAddresses;
        }

        public Role role()
        {
            return role;
        }

        UUID memberId()
        {
            return memberId;
        }

        List<String> groups()
        {
            return groups;
        }

        ReadWriteEndPoint( ClientConnectorAddresses clientConnectorAddresses, Role role, UUID memberId, List<String> groups )
        {
            this.clientConnectorAddresses = clientConnectorAddresses;
            this.role = role;
            this.memberId = memberId;
            this.groups = groups;
        }
    }
}
