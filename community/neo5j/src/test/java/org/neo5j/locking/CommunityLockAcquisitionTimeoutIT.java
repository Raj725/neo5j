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
package org.neo5j.locking;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.graphdb.Label;
import org.neo5j.graphdb.Node;
import org.neo5j.graphdb.ResourceIterator;
import org.neo5j.graphdb.Transaction;
import org.neo5j.graphdb.factory.GraphDatabaseBuilder;
import org.neo5j.graphdb.factory.GraphDatabaseFactoryState;
import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.kernel.GraphDatabaseDependencies;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.factory.CommunityEditionModule;
import org.neo5j.kernel.impl.factory.DatabaseInfo;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacadeFactory;
import org.neo5j.kernel.impl.factory.PlatformModule;
import org.neo5j.kernel.impl.locking.LockAcquisitionTimeoutException;
import org.neo5j.kernel.impl.locking.community.CommunityLockClient;
import org.neo5j.test.OtherThreadExecutor;
import org.neo5j.test.TestGraphDatabaseFactory;
import org.neo5j.test.mockito.matcher.RootCauseMatcher;
import org.neo5j.test.rule.TestDirectory;
import org.neo5j.time.Clocks;
import org.neo5j.time.FakeClock;
import org.neo5j.time.SystemNanoClock;

import static org.junit.Assert.fail;

public class CommunityLockAcquisitionTimeoutIT
{

    @ClassRule
    public static final TestDirectory directory = TestDirectory.testDirectory();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private final OtherThreadExecutor<Void> secondTransactionExecutor = new OtherThreadExecutor<Void>(
            "transactionExecutor", null );
    private final OtherThreadExecutor<Void> clockExecutor = new OtherThreadExecutor<Void>( "clockExecutor", null );

    private static final int TEST_TIMEOUT = 5000;
    private static final String TEST_PROPERTY_NAME = "a";
    private static final Label marker = Label.label( "marker" );
    private static final FakeClock fakeClock = Clocks.fakeClock();

    private static GraphDatabaseService database;

    @BeforeClass
    public static void setUp()
    {
        CustomClockFacadeFactory facadeFactory = new CustomClockFacadeFactory();
        database = new CustomClockTestGraphDatabaseFactory( facadeFactory )
                .newEmbeddedDatabaseBuilder( directory.graphDbDir() )
                .setConfig( GraphDatabaseSettings.lock_acquisition_timeout, "2s" ).newGraphDatabase();

        createTestNode( marker );
    }

    @AfterClass
    public static void tearDownClass()
    {
        database.shutdown();
    }

    @After
    public void tearDown()
    {
        secondTransactionExecutor.close();
        clockExecutor.close();
    }

    @Test( timeout = 5000 )
    public void timeoutOnAcquiringExclusiveLock() throws Exception
    {
        expectedException.expect( new RootCauseMatcher<>( LockAcquisitionTimeoutException.class,
                "The transaction has been terminated. " +
                        "Retry your operation in a new transaction, and you should see a successful result. " +
                        "Unable to acquire lock within configured timeout. " +
                        "Unable to acquire lock for resource: NODE with id: 0 within 2000 millis." ) );

        try ( Transaction ignored = database.beginTx() )
        {
            ResourceIterator<Node> nodes = database.findNodes( marker );
            Node node = nodes.next();
            node.setProperty( TEST_PROPERTY_NAME, "b" );

            Future<Void> propertySetFuture = secondTransactionExecutor.executeDontWait( state ->
            {
                try ( Transaction transaction1 = database.beginTx() )
                {
                    node.setProperty( TEST_PROPERTY_NAME, "b" );
                    transaction1.success();
                }
                return null;
            } );

            secondTransactionExecutor.waitUntilWaiting( exclusiveLockWaitingPredicate() );
            clockExecutor.execute( (OtherThreadExecutor.WorkerCommand<Void,Void>) state ->
            {
                fakeClock.forward( 3, TimeUnit.SECONDS );
                return null;
            } );
            propertySetFuture.get();

            fail( "Should throw termination exception." );
        }
    }

    @Test( timeout = TEST_TIMEOUT )
    public void timeoutOnAcquiringSharedLock() throws Exception
    {
        expectedException.expect( new RootCauseMatcher<>( LockAcquisitionTimeoutException.class,
                "The transaction has been terminated. " +
                        "Retry your operation in a new transaction, and you should see a successful result. " +
                        "Unable to acquire lock within configured timeout. " +
                        "Unable to acquire lock for resource: SCHEMA with id: 0 within 2000 millis." ) );

        try ( Transaction ignored = database.beginTx() )
        {
            database.schema().indexFor( marker ).on( TEST_PROPERTY_NAME ).create();

            Future<Void> propertySetFuture = secondTransactionExecutor.executeDontWait( state ->
            {
                try ( Transaction nestedTransaction = database.beginTx() )
                {
                    ResourceIterator<Node> nodes = database.findNodes( marker );
                    Node node = nodes.next();
                    node.addLabel( Label.label( "anotherLabel" ) );
                    nestedTransaction.success();
                }
                return null;
            } );

            secondTransactionExecutor.waitUntilWaiting( sharedLockWaitingPredicate() );
            clockExecutor.execute( (OtherThreadExecutor.WorkerCommand<Void,Void>) state ->
            {
                fakeClock.forward( 3, TimeUnit.SECONDS );
                return null;
            } );
            propertySetFuture.get();

            fail( "Should throw termination exception." );
        }
    }

    protected Predicate<OtherThreadExecutor.WaitDetails> exclusiveLockWaitingPredicate()
    {
        return waitDetails -> waitDetails.isAt( CommunityLockClient.class, "acquireExclusive" );
    }

    protected Predicate<OtherThreadExecutor.WaitDetails> sharedLockWaitingPredicate()
    {
        return waitDetails -> waitDetails.isAt( CommunityLockClient.class, "acquireShared" );
    }

    private static void createTestNode( Label marker )
    {
        try ( Transaction transaction = database.beginTx() )
        {
            database.createNode( marker );
            transaction.success();
        }
    }

    private static class CustomClockTestGraphDatabaseFactory extends TestGraphDatabaseFactory
    {
        private GraphDatabaseFacadeFactory customFacadeFactory;

        CustomClockTestGraphDatabaseFactory( GraphDatabaseFacadeFactory customFacadeFactory )
        {
            this.customFacadeFactory = customFacadeFactory;
        }

        @Override
        protected GraphDatabaseBuilder.DatabaseCreator createDatabaseCreator( File storeDir,
                GraphDatabaseFactoryState state )
        {
            return new GraphDatabaseBuilder.DatabaseCreator()
            {
                @Override
                public GraphDatabaseService newDatabase( Map<String,String> config )
                {
                    return newDatabase( Config.embeddedDefaults( config ) );
                }

                @Override
                public GraphDatabaseService newDatabase( Config config )
                {
                    return customFacadeFactory.newFacade( storeDir, config,
                            GraphDatabaseDependencies.newDependencies( state.databaseDependencies() ) );
                }
            };
        }
    }

    private static class CustomClockFacadeFactory extends GraphDatabaseFacadeFactory
    {

        CustomClockFacadeFactory()
        {
            super( DatabaseInfo.COMMUNITY, CommunityEditionModule::new );
        }

        @Override
        protected PlatformModule createPlatform( File storeDir, Config config, Dependencies dependencies,
                GraphDatabaseFacade graphDatabaseFacade )
        {
            return new PlatformModule( storeDir, config, databaseInfo, dependencies, graphDatabaseFacade )
            {
                @Override
                protected SystemNanoClock createClock()
                {
                    return fakeClock;
                }
            };
        }

    }
}
