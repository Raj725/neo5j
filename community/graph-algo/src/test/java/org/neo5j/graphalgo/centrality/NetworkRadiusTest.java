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
package org.neo5j.graphalgo.centrality;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.neo5j.graphalgo.CostEvaluator;
import org.neo5j.graphalgo.impl.centrality.NetworkRadius;
import org.neo5j.graphalgo.impl.shortestpath.SingleSourceShortestPath;
import org.neo5j.graphalgo.impl.shortestpath.SingleSourceShortestPathDijkstra;
import org.neo5j.graphalgo.impl.util.DoubleComparator;
import org.neo5j.graphdb.Direction;
import org.neo5j.graphdb.Relationship;

import common.Neo5jAlgoTestCase;

public class NetworkRadiusTest extends Neo5jAlgoTestCase
{
    protected SingleSourceShortestPath<Double> getSingleSourceShortestPath()
    {
        return new SingleSourceShortestPathDijkstra<Double>( 0.0, null,
            new CostEvaluator<Double>()
            {
                public Double getCost( Relationship relationship,
                            Direction direction )
                {
                    return 1.0;
                }
            }, new org.neo5j.graphalgo.impl.util.DoubleAdder(),
            new org.neo5j.graphalgo.impl.util.DoubleComparator(),
            Direction.BOTH, MyRelTypes.R1 );
    }

    @Test
    public void testBox()
    {
        graph.makeEdgeChain( "a,b,c,d,a" );
        NetworkRadius<Double> radius = new NetworkRadius<Double>(
            getSingleSourceShortestPath(), 0.0, graph.getAllNodes(),
            new DoubleComparator() );
        assertTrue( radius.getCentrality( null ) == 2.0 );
    }

    @Test
    public void testPlusShape()
    {
        graph.makeEdgeChain( "a,b,c" );
        graph.makeEdgeChain( "d,b,e" );
        NetworkRadius<Double> radius = new NetworkRadius<Double>(
            getSingleSourceShortestPath(), 0.0, graph.getAllNodes(),
            new DoubleComparator() );
        assertTrue( radius.getCentrality( null ) == 1.0 );
    }

    @Test
    public void testChain()
    {
        graph.makeEdgeChain( "a,b,c,d,e" );
        NetworkRadius<Double> radius = new NetworkRadius<Double>(
            getSingleSourceShortestPath(), 0.0, graph.getAllNodes(),
            new DoubleComparator() );
        assertTrue( radius.getCentrality( null ) == 2.0 );
    }
}
