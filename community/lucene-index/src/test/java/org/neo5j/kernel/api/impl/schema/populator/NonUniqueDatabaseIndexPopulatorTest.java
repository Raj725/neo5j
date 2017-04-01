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
package org.neo5j.kernel.api.impl.schema.populator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.neo5j.collection.primitive.PrimitiveLongCollections;
import org.neo5j.collection.primitive.PrimitiveLongIterator;
import org.neo5j.io.IOUtils;
import org.neo5j.kernel.api.impl.index.storage.DirectoryFactory;
import org.neo5j.kernel.api.impl.index.storage.PartitionedIndexStorage;
import org.neo5j.kernel.api.impl.schema.LuceneSchemaIndexBuilder;
import org.neo5j.kernel.api.impl.schema.SchemaIndex;
import org.neo5j.kernel.api.index.IndexEntryUpdate;
import org.neo5j.kernel.api.schema_new.IndexQuery;
import org.neo5j.kernel.api.schema_new.LabelSchemaDescriptor;
import org.neo5j.kernel.api.schema_new.SchemaDescriptorFactory;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptorFactory;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.api.index.sampling.IndexSamplingConfig;
import org.neo5j.storageengine.api.schema.IndexReader;
import org.neo5j.storageengine.api.schema.IndexSample;
import org.neo5j.test.rule.TestDirectory;
import org.neo5j.test.rule.fs.DefaultFileSystemRule;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NonUniqueDatabaseIndexPopulatorTest
{
    @Rule
    public final TestDirectory testDir = TestDirectory.testDirectory();
    @Rule
    public final DefaultFileSystemRule fileSystemRule = new DefaultFileSystemRule();

    private final DirectoryFactory dirFactory = new DirectoryFactory.InMemoryDirectoryFactory();

    private SchemaIndex index;
    private NonUniqueLuceneIndexPopulator populator;
    private LabelSchemaDescriptor labelSchemaDescriptor = SchemaDescriptorFactory.forLabel( 0, 0 );

    @Before
    public void setUp() throws Exception
    {
        File folder = testDir.directory( "folder" );
        PartitionedIndexStorage indexStorage = new PartitionedIndexStorage( dirFactory, fileSystemRule.get(), folder,
                "testIndex", false );

        index = LuceneSchemaIndexBuilder.create( NewIndexDescriptorFactory.forSchema( labelSchemaDescriptor ) )
                .withIndexStorage( indexStorage )
                .build();
    }

    @After
    public void tearDown() throws Exception
    {
        if ( populator != null )
        {
            populator.close( false );
        }
        IOUtils.closeAll( index, dirFactory );
    }

    @Test
    public void sampleEmptyIndex() throws IOException
    {
        populator = newPopulator();

        IndexSample sample = populator.sampleResult();

        assertEquals( new IndexSample(), sample );
    }

    @Test
    public void sampleIncludedUpdates() throws Exception
    {
        populator = newPopulator();

        List<IndexEntryUpdate> updates = Arrays.asList(
                IndexEntryUpdate.add( 1, labelSchemaDescriptor, "aaa" ),
                IndexEntryUpdate.add( 2, labelSchemaDescriptor, "bbb" ),
                IndexEntryUpdate.add( 3, labelSchemaDescriptor, "ccc" ) );

        updates.forEach( populator::includeSample );

        IndexSample sample = populator.sampleResult();

        assertEquals( new IndexSample( 3, 3, 3 ), sample );
    }

    @Test
    public void sampleIncludedUpdatesWithDuplicates() throws Exception
    {
        populator = newPopulator();

        List<IndexEntryUpdate> updates = Arrays.asList(
                IndexEntryUpdate.add( 1, labelSchemaDescriptor, "foo" ),
                IndexEntryUpdate.add( 2, labelSchemaDescriptor, "bar" ),
                IndexEntryUpdate.add( 3, labelSchemaDescriptor, "foo" ) );

        updates.forEach( populator::includeSample );

        IndexSample sample = populator.sampleResult();

        assertEquals( new IndexSample( 3, 2, 3 ), sample );
    }

    @Test
    public void addUpdates() throws Exception
    {
        populator = newPopulator();

        List<IndexEntryUpdate<?>> updates = Arrays.asList(
                IndexEntryUpdate.add( 1, labelSchemaDescriptor, "foo" ),
                IndexEntryUpdate.add( 2, labelSchemaDescriptor, "bar" ),
                IndexEntryUpdate.add( 42, labelSchemaDescriptor, "bar" ) );

        populator.add( updates );

        index.maybeRefreshBlocking();
        try ( IndexReader reader = index.getIndexReader() )
        {
            int propertyKeyId = labelSchemaDescriptor.getPropertyId();
            PrimitiveLongIterator allEntities = reader.query( IndexQuery.exists( propertyKeyId ) );
            assertArrayEquals( new long[]{1, 2, 42}, PrimitiveLongCollections.asArray( allEntities ) );
        }
    }

    private NonUniqueLuceneIndexPopulator newPopulator() throws IOException
    {
        IndexSamplingConfig samplingConfig = new IndexSamplingConfig( Config.empty() );
        NonUniqueLuceneIndexPopulator populator = new NonUniqueLuceneIndexPopulator( index, samplingConfig );
        populator.create();
        populator.configureSampling( true );
        return populator;
    }
}
