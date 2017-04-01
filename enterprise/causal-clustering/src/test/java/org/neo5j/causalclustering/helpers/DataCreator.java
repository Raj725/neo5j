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
package org.neo5j.causalclustering.helpers;

import java.util.function.Supplier;

import org.neo5j.causalclustering.discovery.Cluster;
import org.neo5j.causalclustering.discovery.CoreClusterMember;
import org.neo5j.graphdb.Label;
import org.neo5j.graphdb.Node;
import org.neo5j.helpers.collection.Pair;

public class DataCreator
{
    public static CoreClusterMember createLabelledNodesWithProperty( Cluster cluster, int numberOfNodes,
            Label label, Supplier<Pair<String,Object>> supplier ) throws Exception
    {
        CoreClusterMember last = null;
        for ( int i = 0; i < numberOfNodes; i++ )
        {
            last = cluster.coreTx( ( db, tx ) ->
            {
                Node node = db.createNode( label );
                node.setProperty( supplier.get().first(), supplier.get().other() );
                tx.success();
            } );
        }
        return last;
    }

    public static CoreClusterMember createEmptyNodes( Cluster cluster, int numberOfNodes ) throws Exception
    {
        CoreClusterMember last = null;
        for ( int i = 0; i < numberOfNodes; i++ )
        {
            last = cluster.coreTx( ( db, tx ) ->
            {
                db.createNode();
                tx.success();
            } );
        }
        return last;
    }
}