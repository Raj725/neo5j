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
package org.neo5j.upgrade;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import upgrade.DatabaseContentVerifier;
import upgrade.ListAccumulatorMigrationProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.neo5j.consistency.checking.full.ConsistencyCheckIncompleteException;
import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.graphdb.Transaction;
import org.neo5j.graphdb.factory.EnterpriseGraphDatabaseFactory;
import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.io.pagecache.PageCache;
import org.neo5j.kernel.api.impl.index.storage.DirectoryFactory;
import org.neo5j.kernel.api.impl.schema.LuceneSchemaIndexProvider;
import org.neo5j.kernel.api.index.SchemaIndexProvider;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.ha.HaSettings;
import org.neo5j.kernel.ha.HighlyAvailableGraphDatabase;
import org.neo5j.kernel.impl.MyRelTypes;
import org.neo5j.kernel.impl.api.scan.LabelScanStoreProvider;
import org.neo5j.kernel.impl.factory.OperationalMode;
import org.neo5j.kernel.impl.ha.ClusterManager;
import org.neo5j.kernel.impl.logging.NullLogService;
import org.neo5j.kernel.impl.store.MetaDataStore;
import org.neo5j.kernel.impl.store.NeoStores;
import org.neo5j.kernel.impl.store.StoreFactory;
import org.neo5j.kernel.impl.store.format.RecordFormats;
import org.neo5j.kernel.impl.store.format.highlimit.HighLimit;
import org.neo5j.kernel.impl.store.format.standard.Standard;
import org.neo5j.kernel.impl.storemigration.StoreUpgrader;
import org.neo5j.kernel.impl.storemigration.StoreVersionCheck;
import org.neo5j.kernel.impl.storemigration.UpgradableDatabase;
import org.neo5j.kernel.impl.storemigration.legacystore.LegacyStoreVersionCheck;
import org.neo5j.kernel.impl.storemigration.participant.SchemaIndexMigrator;
import org.neo5j.kernel.impl.storemigration.participant.StoreMigrator;
import org.neo5j.kernel.lifecycle.LifeSupport;
import org.neo5j.logging.LogProvider;
import org.neo5j.logging.NullLogProvider;
import org.neo5j.test.rule.NeoStoreDataSourceRule;
import org.neo5j.test.rule.PageCacheRule;
import org.neo5j.test.rule.TestDirectory;
import org.neo5j.test.rule.fs.DefaultFileSystemRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo5j.consistency.store.StoreAssertions.assertConsistentStore;
import static org.neo5j.helpers.collection.MapUtil.stringMap;
import static org.neo5j.kernel.impl.ha.ClusterManager.allSeesAllAsAvailable;
import static org.neo5j.kernel.impl.storemigration.MigrationTestUtils.find20FormatStoreDirectory;
import static org.neo5j.upgrade.StoreMigratorTestUtil.buildClusterWithMasterDirIn;

@RunWith( Parameterized.class )
public class StoreMigratorFrom20IT
{
    private final TestDirectory storeDir = TestDirectory.testDirectory();
    private final PageCacheRule pageCacheRule = new PageCacheRule();
    private final DefaultFileSystemRule fileSystemRule = new DefaultFileSystemRule();

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule( storeDir )
                                          .around( fileSystemRule ).around( pageCacheRule );

    private final ListAccumulatorMigrationProgressMonitor monitor = new ListAccumulatorMigrationProgressMonitor();
    private FileSystemAbstraction fs;
    private PageCache pageCache;
    private final LifeSupport life = new LifeSupport();
    private UpgradableDatabase upgradableDatabase;
    private SchemaIndexProvider schemaIndexProvider;
    private LabelScanStoreProvider labelScanStoreProvider;

    @Parameter
    public String recordFormatName;
    @Parameter( 1 )
    public RecordFormats recordFormat;

    @Parameters( name = "{0}" )
    public static List<Object[]> recordFormats()
    {
        return Arrays.asList(
                new Object[]{Standard.LATEST_NAME, Standard.LATEST_RECORD_FORMATS},
                new Object[]{HighLimit.NAME, HighLimit.RECORD_FORMATS} );
    }

    @Before
    public void setUp()
    {
        fs = fileSystemRule.get();
        pageCache = pageCacheRule.getPageCache( fs );

        File storeDirectory = storeDir.directory();
        schemaIndexProvider = new LuceneSchemaIndexProvider( fs, DirectoryFactory.PERSISTENT, storeDirectory,
                NullLogProvider.getInstance(), Config.empty(), OperationalMode.single );
        labelScanStoreProvider = NeoStoreDataSourceRule.nativeLabelScanStoreProvider( storeDirectory, fs, pageCache );

        upgradableDatabase = new UpgradableDatabase( fs, new StoreVersionCheck( pageCache ),
                new LegacyStoreVersionCheck( fs ), recordFormat );
    }

    @After
    public void tearDown()
    {
        life.shutdown();
    }

    @Test
    public void shouldMigrate() throws IOException, ConsistencyCheckIncompleteException
    {
        // WHEN
        StoreMigrator storeMigrator = new StoreMigrator( fs, pageCache, getConfig(), NullLogService.getInstance(),
                schemaIndexProvider );
        SchemaIndexMigrator indexMigrator = new SchemaIndexMigrator( fs, schemaIndexProvider, labelScanStoreProvider );
        upgrader( indexMigrator, storeMigrator ).migrateIfNeeded( find20FormatStoreDirectory( storeDir.directory() ) );

        // THEN
        assertEquals( 2, monitor.progresses().size() );
        assertTrue( monitor.isStarted() );
        assertTrue( monitor.isFinished() );

        GraphDatabaseService database = new EnterpriseGraphDatabaseFactory()
                .newEmbeddedDatabaseBuilder( storeDir.absolutePath() )
                .newGraphDatabase();
        try
        {
            verifyDatabaseContents( database );
        }
        finally
        {
            // CLEANUP
            database.shutdown();
        }

        LogProvider logProvider = NullLogProvider.getInstance();
        StoreFactory storeFactory = new StoreFactory( storeDir.directory(), pageCache, fs, logProvider );
        try ( NeoStores neoStores = storeFactory.openAllNeoStores( true ) )
        {
            verifyNeoStore( neoStores );
        }
        assertConsistentStore( storeDir.directory() );
    }

    @Test
    public void shouldMigrateCluster() throws Throwable
    {
        // Given
        File legacyStoreDir = find20FormatStoreDirectory( storeDir.directory( "legacy-indexes" ) );

        // When
        StoreMigrator storeMigrator = new StoreMigrator( fs, pageCache, getConfig(), NullLogService.getInstance(),
                schemaIndexProvider );
        SchemaIndexMigrator indexMigrator = new SchemaIndexMigrator( fs, schemaIndexProvider, labelScanStoreProvider );
        upgrader( indexMigrator, storeMigrator ).migrateIfNeeded( legacyStoreDir );
        ClusterManager.ManagedCluster cluster =
                buildClusterWithMasterDirIn( fs, legacyStoreDir, life, getParams() );
        cluster.await( allSeesAllAsAvailable() );
        cluster.sync();

        // Then
        HighlyAvailableGraphDatabase slave1 = cluster.getAnySlave();
        verifySlaveContents( slave1 );
        verifySlaveContents( cluster.getAnySlave( slave1 ) );
        verifyDatabaseContents( cluster.getMaster() );
    }

    private static void verifyDatabaseContents( GraphDatabaseService database )
    {
        DatabaseContentVerifier verifier = new DatabaseContentVerifier( database, 2 );
        verifyNumberOfNodesAndRelationships( verifier );
        createNewNode( database );
        createNewRelationship( database );
        verifier.verifyLegacyIndex();
        verifier.verifyIndex();
        verifier.verifyJohnnyLabels();
    }

    private static void createNewNode( GraphDatabaseService database )
    {
        try ( Transaction tx = database.beginTx() )
        {
            database.createNode();
            tx.success();
        }
    }

    private static void createNewRelationship( GraphDatabaseService database )
    {
        try ( Transaction tx = database.beginTx() )
        {
            database.createNode().createRelationshipTo( database.createNode(), MyRelTypes.TEST );
            tx.success();
        }
    }

    private static void verifySlaveContents( HighlyAvailableGraphDatabase haDb )
    {
        DatabaseContentVerifier verifier = new DatabaseContentVerifier( haDb, 2 );
        verifyNumberOfNodesAndRelationships( verifier );
    }

    private static void verifyNumberOfNodesAndRelationships( DatabaseContentVerifier verifier )
    {
        verifier.verifyNodes( 502 );
        verifier.verifyRelationships( 500 );
    }

    public void verifyNeoStore( NeoStores neoStores )
    {
        MetaDataStore metaDataStore = neoStores.getMetaDataStore();
        assertEquals( 1317392957120L, metaDataStore.getCreationTime() );
        assertEquals( -472309512128245482L, metaDataStore.getRandomNumber() );
        assertEquals( 5L, metaDataStore.getCurrentLogVersion() );
        assertEquals( recordFormat.storeVersion(),
                MetaDataStore.versionLongToString( metaDataStore.getStoreVersion() ) );
        assertEquals( 1042L, metaDataStore.getLastCommittedTransactionId() );
    }

    private StoreUpgrader upgrader( SchemaIndexMigrator indexMigrator, StoreMigrator storeMigrator )
    {
        Config config = getConfig().augment( stringMap( GraphDatabaseSettings.allow_store_upgrade.name(), "true" ) );
        StoreUpgrader upgrader = new StoreUpgrader( upgradableDatabase, monitor, config, fs, pageCache,
                NullLogProvider.getInstance() );
        upgrader.addParticipant( indexMigrator );
        upgrader.addParticipant( storeMigrator );
        return upgrader;
    }

    private Map<String,String> getParams()
    {
        return stringMap(
                GraphDatabaseSettings.record_format.name(), recordFormatName,
                HaSettings.read_timeout.name(), "2m" );
    }

    private Config getConfig()
    {
        return Config.embeddedDefaults( getParams() );
    }
}
