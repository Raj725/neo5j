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
package org.neo5j.kernel.impl.traversal;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import org.neo5j.graphdb.Node;
import org.neo5j.graphdb.Path;
import org.neo5j.graphdb.RelationshipType;
import org.neo5j.graphdb.Transaction;
import org.neo5j.graphdb.traversal.Evaluators;
import org.neo5j.graphdb.traversal.TraversalDescription;
import org.neo5j.helpers.collection.IterableWrapper;

public class TestTraversalWithIterable extends TraversalTestBase
{
    @Test
    public void traverseWithIterableForStartNodes() throws Exception
    {
        /*
         * (a)-->(b)-->(c)
         * (d)-->(e)-->(f)
         *
         */

        createGraph( "a TO b", "b TO c", "d TO e", "e TO f" );

        try (Transaction tx = beginTx())
        {
            TraversalDescription basicTraverser = getGraphDb().traversalDescription().evaluator( Evaluators.atDepth(2) );

            Collection<Node> startNodes = new ArrayList<>(  );
            startNodes.add( getNodeWithName( "a" ) );
            startNodes.add( getNodeWithName( "d" ) );

            Iterable<Node> iterableStartNodes = startNodes;

            expectPaths( basicTraverser.traverse( iterableStartNodes ), "a,b,c", "d,e,f");
            tx.success();
        }
    }

    @Test
    public void useTraverserInsideTraverser() throws Exception
    {
        /*
         * (a)-->(b)-->(c)
         *  |
         * \/
         * (d)-->(e)-->(f)
         *
         */

        createGraph( "a FIRST d", "a TO b", "b TO c", "d TO e", "e TO f" );

        try (Transaction tx = beginTx())
        {
            TraversalDescription firstTraverser = getGraphDb().traversalDescription()
                    .relationships( RelationshipType.withName( "FIRST" ) )
                    .evaluator( Evaluators.toDepth( 1 ) );
            final Iterable<Path> firstResult = firstTraverser.traverse( getNodeWithName( "a" ) );

            Iterable<Node> startNodesForNestedTraversal = new IterableWrapper<Node,Path>( firstResult )
            {
                @Override
                protected Node underlyingObjectToObject( Path path )
                {
                    return path.endNode();
                }
            };

            TraversalDescription nestedTraversal = getGraphDb().traversalDescription().evaluator( Evaluators.atDepth( 2 ) );
            expectPaths( nestedTraversal.traverse( startNodesForNestedTraversal ), "a,b,c", "d,e,f");
            tx.success();
        }
    }

}
