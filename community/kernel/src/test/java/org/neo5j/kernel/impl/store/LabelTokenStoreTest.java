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
package org.neo5j.kernel.impl.store;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import org.neo5j.io.pagecache.PageCache;
import org.neo5j.io.pagecache.PageCursor;
import org.neo5j.io.pagecache.PagedFile;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.store.format.RecordFormatSelector;
import org.neo5j.kernel.impl.store.id.IdGeneratorFactory;
import org.neo5j.kernel.impl.store.record.LabelTokenRecord;
import org.neo5j.kernel.impl.store.record.Record;
import org.neo5j.logging.LogProvider;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.neo5j.kernel.impl.store.record.RecordLoad.FORCE;
import static org.neo5j.kernel.impl.store.record.RecordLoad.NORMAL;

public class LabelTokenStoreTest
{
    private final File file = mock( File.class );
    private final IdGeneratorFactory generatorFactory = mock( IdGeneratorFactory.class );
    private final PageCache cache = mock( PageCache.class );
    private final LogProvider logProvider = mock( LogProvider.class );
    private final DynamicStringStore dynamicStringStore = mock( DynamicStringStore.class );
    private final PageCursor pageCursor = mock( PageCursor.class );
    private final Config config = Config.empty();

    @Test
    public void forceGetRecordSkipInUsecheck() throws IOException
    {
        LabelTokenStore store = new UnusedLabelTokenStore();
        LabelTokenRecord record = store.getRecord( 7, store.newRecord(), FORCE );
        assertFalse( "Record should not be in use", record.inUse() );
    }

    @Test( expected = InvalidRecordException.class )
    public void getRecord() throws IOException
    {
        when( pageCursor.getByte() ).thenReturn( Record.NOT_IN_USE.byteValue() );

        LabelTokenStore store = new UnusedLabelTokenStore();
        store.getRecord( 7, store.newRecord(), NORMAL );
    }

    class UnusedLabelTokenStore extends LabelTokenStore
    {
        UnusedLabelTokenStore() throws IOException
        {
            super( file, config, generatorFactory, cache, logProvider, dynamicStringStore,
                    RecordFormatSelector.defaultFormat() );
            storeFile = mock( PagedFile.class );

            when( storeFile.io( any( Long.class ), any( Integer.class ) ) ).thenReturn( pageCursor );
            when( storeFile.pageSize() ).thenReturn( 1 );
            when( pageCursor.next() ).thenReturn( true );
        }
    }
}