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
package org.neo5j.index.impl.lucene.legacy;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

import org.neo5j.collection.primitive.PrimitiveLongCollections;
import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.graphdb.Node;
import org.neo5j.graphdb.Transaction;
import org.neo5j.graphdb.factory.GraphDatabaseFactoryState;
import org.neo5j.kernel.api.impl.index.storage.DirectoryFactory;
import org.neo5j.kernel.api.impl.schema.LuceneSchemaIndexProvider;
import org.neo5j.kernel.api.index.IndexAccessor;
import org.neo5j.kernel.api.index.SchemaIndexProvider;
import org.neo5j.kernel.api.schema_new.IndexQuery;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptorFactory;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.api.index.sampling.IndexSamplingConfig;
import org.neo5j.kernel.impl.factory.CommunityEditionModule;
import org.neo5j.kernel.impl.factory.DatabaseInfo;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacadeFactory;
import org.neo5j.kernel.impl.factory.OperationalMode;
import org.neo5j.kernel.impl.factory.PlatformModule;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.logging.NullLogService;
import org.neo5j.kernel.impl.util.Neo5jJobScheduler;
import org.neo5j.logging.LogProvider;
import org.neo5j.logging.NullLogProvider;
import org.neo5j.storageengine.api.schema.IndexReader;
import org.neo5j.test.rule.TestDirectory;
import org.neo5j.test.rule.fs.DefaultFileSystemRule;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.neo5j.graphdb.Label.label;

public class NonUniqueIndexTest
{
    @Rule
    public final TestDirectory directory = TestDirectory.testDirectory( getClass() );
    @Rule
    public final DefaultFileSystemRule fileSystemRule = new DefaultFileSystemRule();

    @Test
    public void concurrentIndexPopulationAndInsertsShouldNotProduceDuplicates() throws Exception
    {
        // Given
        GraphDatabaseService db = newEmbeddedGraphDatabaseWithSlowJobScheduler();

        // When
        try ( Transaction tx = db.beginTx() )
        {
            db.schema().indexFor( label( "SomeLabel" ) ).on( "key" ).create();
            tx.success();
        }
        Node node;
        try ( Transaction tx = db.beginTx() )
        {
            node = db.createNode( label( "SomeLabel" ) );
            node.setProperty( "key", "value" );
            tx.success();
        }
        db.shutdown();

        // Then
        assertThat( nodeIdsInIndex( 1, "value" ), equalTo( singletonList( node.getId() ) ) );
    }

    private GraphDatabaseService newEmbeddedGraphDatabaseWithSlowJobScheduler()
    {
        GraphDatabaseFactoryState graphDatabaseFactoryState = new GraphDatabaseFactoryState();
        graphDatabaseFactoryState.setUserLogProvider( NullLogService.getInstance().getUserLogProvider() );
        return new GraphDatabaseFacadeFactory( DatabaseInfo.COMMUNITY, CommunityEditionModule::new )
        {
            @Override
            protected PlatformModule createPlatform( File storeDir, Config config, Dependencies dependencies,
                    GraphDatabaseFacade graphDatabaseFacade )
            {
                return new PlatformModule( storeDir, config, databaseInfo, dependencies, graphDatabaseFacade )
                {
                    @Override
                    protected Neo5jJobScheduler createJobScheduler()
                    {
                        return newSlowJobScheduler();
                    }

                    @Override
                    protected LogService createLogService( LogProvider userLogProvider )
                    {
                        return NullLogService.getInstance();
                    }
                };
            }
        }.newFacade( directory.graphDbDir(), Config.embeddedDefaults(),
                graphDatabaseFactoryState.databaseDependencies() );
    }

    private static Neo5jJobScheduler newSlowJobScheduler()
    {
        return new Neo5jJobScheduler()
        {
            @Override
            public JobHandle schedule( Group group, Runnable job )
            {
                return super.schedule( group, slowRunnable( job ) );
            }

            @Override
            public JobHandle schedule( Group group, Runnable job, Map<String,String> metadata )
            {
                return super.schedule( group, slowRunnable(job), metadata );
            }
        };
    }

    private static Runnable slowRunnable( final Runnable target )
    {
        return () ->
        {
            LockSupport.parkNanos( 100_000_000 );
            target.run();
        };
    }

    private List<Long> nodeIdsInIndex( int indexId, String value ) throws Exception
    {
        Config config = Config.empty();
        SchemaIndexProvider indexProvider = new LuceneSchemaIndexProvider( fileSystemRule.get(),
                DirectoryFactory.PERSISTENT, directory.graphDbDir(), NullLogProvider.getInstance(), Config.empty(), OperationalMode.single );
        IndexSamplingConfig samplingConfig = new IndexSamplingConfig( config );
        try ( IndexAccessor accessor = indexProvider.getOnlineAccessor( indexId,
                NewIndexDescriptorFactory.forLabel( 0, 0 ), samplingConfig );
              IndexReader reader = accessor.newReader() )
        {
            return PrimitiveLongCollections.asList( reader.query( IndexQuery.exact( 1, value ) ) );
        }
    }
}
