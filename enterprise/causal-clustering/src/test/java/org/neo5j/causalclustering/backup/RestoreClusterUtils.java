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
package org.neo5j.causalclustering.backup;

import java.io.File;

import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.graphdb.Label;
import org.neo5j.graphdb.Node;
import org.neo5j.graphdb.RelationshipType;
import org.neo5j.graphdb.Transaction;
import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.test.TestGraphDatabaseFactory;

public class RestoreClusterUtils
{
    public static File createClassicNeo5jStore( File base, FileSystemAbstraction fileSystem, int nodesToCreate, String recordFormat )
    {
        File existingDbDir = new File( base, "existing" );
        GraphDatabaseService db = new TestGraphDatabaseFactory()
                .setFileSystem( fileSystem )
                .newEmbeddedDatabaseBuilder( existingDbDir )
                .setConfig( GraphDatabaseSettings.record_format, recordFormat )
                .newGraphDatabase();

        for ( int i = 0; i < (nodesToCreate / 2); i++ )
        {
            try ( Transaction tx = db.beginTx() )
            {
                Node node1 = db.createNode( Label.label( "Label-" + i ) );
                Node node2 = db.createNode( Label.label( "Label-" + i ) );
                node1.createRelationshipTo( node2, RelationshipType.withName( "REL-" + i ) );
                tx.success();
            }
        }

        db.shutdown();

        return existingDbDir;
    }
}
