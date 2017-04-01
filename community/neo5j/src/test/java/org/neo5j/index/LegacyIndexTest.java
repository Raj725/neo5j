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
package org.neo5j.index;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import org.neo5j.graphdb.Label;
import org.neo5j.graphdb.index.IndexManager;
import org.neo5j.index.lucene.unsafe.batchinsert.LuceneBatchInserterIndexProvider;
import org.neo5j.test.rule.TestDirectory;
import org.neo5j.unsafe.batchinsert.BatchInserter;
import org.neo5j.unsafe.batchinsert.BatchInserterIndex;
import org.neo5j.unsafe.batchinsert.BatchInserters;

import static org.neo5j.helpers.collection.MapUtil.stringMap;

public class LegacyIndexTest
{
    private static final long TEST_TIMEOUT = 40_000;

    @Rule
    public TestDirectory directory = TestDirectory.testDirectory();

    @Test( timeout = TEST_TIMEOUT )
    public void legacyIndexPopulationWithBunchOfFields() throws Exception
    {
        BatchInserter batchNode = BatchInserters.inserter( directory.graphDbDir() );
        LuceneBatchInserterIndexProvider provider = new LuceneBatchInserterIndexProvider( batchNode );
        try
        {
            BatchInserterIndex batchIndex = provider.nodeIndex( "node_auto_index",
                    stringMap( IndexManager.PROVIDER, "lucene", "type", "fulltext" ) );

            Map<String,Object> properties = new HashMap<>();
            for ( int i = 0; i < 2000; i++ )
            {
                properties.put( Integer.toString( i ), RandomStringUtils.randomAlphabetic( 200 ) );
            }

            long node = batchNode.createNode( properties, Label.label( "NODE" ) );
            batchIndex.add( node, properties );
        }
        finally
        {
            provider.shutdown();
            batchNode.shutdown();
        }
    }
}
