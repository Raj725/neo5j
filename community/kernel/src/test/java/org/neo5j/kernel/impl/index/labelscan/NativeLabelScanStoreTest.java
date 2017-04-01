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
package org.neo5j.kernel.impl.index.labelscan;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Rule;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.io.pagecache.PageCache;
import org.neo5j.kernel.api.impl.labelscan.LabelScanStoreTest;
import org.neo5j.kernel.api.labelscan.LabelScanStore;
import org.neo5j.kernel.api.labelscan.NodeLabelUpdate;
import org.neo5j.test.rule.PageCacheRule;

public class NativeLabelScanStoreTest extends LabelScanStoreTest
{
    @Rule
    public PageCacheRule pageCacheRule = new PageCacheRule();

    @Override
    protected LabelScanStore createLabelScanStore( FileSystemAbstraction fileSystemAbstraction, File rootFolder,
            List<NodeLabelUpdate> existingData, boolean usePersistentStore, boolean readOnly,
            LabelScanStore.Monitor monitor )
    {
        PageCache pageCache = pageCacheRule.getPageCache( fileSystemAbstraction );
        NativeLabelScanStore nativeLabelScanStore = new NativeLabelScanStore( pageCache, rootFolder,
                asStream( existingData ), readOnly, monitor );
        return nativeLabelScanStore;
    }

    @Override
    protected Matcher<Iterable<? super String>> hasBareMinimumFileList()
    {
        return Matchers.hasItem( Matchers.equalTo( NativeLabelScanStore.FILE_NAME ) );
    }

    @Override
    protected void corruptIndex( FileSystemAbstraction fileSystem, File rootFolder ) throws IOException
    {
        File lssFile = new File( rootFolder, NativeLabelScanStore.FILE_NAME );
        scrambleFile( lssFile );
    }
}
