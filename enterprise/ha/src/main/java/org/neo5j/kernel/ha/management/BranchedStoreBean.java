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
package org.neo5j.kernel.ha.management;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.management.NotCompliantMBeanException;

import org.neo5j.helpers.Service;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.io.pagecache.PageCache;
import org.neo5j.jmx.impl.ManagementBeanProvider;
import org.neo5j.jmx.impl.ManagementData;
import org.neo5j.jmx.impl.Neo5jMBean;
import org.neo5j.kernel.ha.HighlyAvailableGraphDatabase;
import org.neo5j.kernel.impl.store.MetaDataStore;
import org.neo5j.kernel.impl.store.MetaDataStore.Position;
import org.neo5j.management.BranchedStore;
import org.neo5j.management.BranchedStoreInfo;

import static org.neo5j.com.storecopy.StoreUtil.getBranchedDataRootDirectory;


@Service.Implementation(ManagementBeanProvider.class)
public final class BranchedStoreBean extends ManagementBeanProvider
{
    public BranchedStoreBean()
    {
        super( BranchedStore.class );
    }

    @Override
    protected Neo5jMBean createMXBean( ManagementData management )
            throws NotCompliantMBeanException
    {
        if ( !isHA( management ) )
        {
            return null;
        }
        return new BranchedStoreImpl( management, true );
    }

    @Override
    protected Neo5jMBean createMBean( ManagementData management )
            throws NotCompliantMBeanException
    {
        if ( !isHA( management ) )
        {
            return null;
        }
        return new BranchedStoreImpl( management );
    }

    private static boolean isHA( ManagementData management )
    {
        return management.getKernelData().graphDatabase() instanceof HighlyAvailableGraphDatabase;
    }

    private static class BranchedStoreImpl extends Neo5jMBean implements BranchedStore
    {
        private final FileSystemAbstraction fileSystem;
        private final File storePath;
        private final PageCache pageCache;

        protected BranchedStoreImpl( final ManagementData management ) throws NotCompliantMBeanException
        {
            super( management );
            fileSystem = getFilesystem( management );
            storePath = getStorePath( management );
            pageCache = getPageCache( management );
        }

        protected BranchedStoreImpl( final ManagementData management, boolean isMXBean )
        {
            super( management, isMXBean );
            fileSystem = getFilesystem( management );
            storePath = getStorePath( management );
            pageCache = getPageCache( management );
        }

        @Override
        public BranchedStoreInfo[] getBranchedStores()
        {
            if ( storePath == null )
            {
                return new BranchedStoreInfo[0];
            }

            List<BranchedStoreInfo> toReturn = new LinkedList<>();
            for ( File branchDirectory : fileSystem.listFiles( getBranchedDataRootDirectory( storePath ) ) )
            {
                if ( !branchDirectory.isDirectory() )
                {
                    continue;
                }
                toReturn.add( parseBranchedStore( branchDirectory ) );
            }
            return toReturn.toArray( new BranchedStoreInfo[toReturn.size()] );
        }

        private BranchedStoreInfo parseBranchedStore( File branchDirectory )
        {
            try
            {
                final File neoStoreFile = new File( branchDirectory, MetaDataStore.DEFAULT_NAME );
                long txId = MetaDataStore.getRecord( pageCache, neoStoreFile, Position.LAST_TRANSACTION_ID );
                long timestamp = Long.parseLong( branchDirectory.getName() );
                return new BranchedStoreInfo( branchDirectory.getName(), txId, timestamp );
            }
            catch ( IOException e )
            {
                throw new IllegalStateException( "Cannot read branched neostore", e );
            }
        }

        private PageCache getPageCache( ManagementData management)
        {
            return management.getKernelData().getPageCache();
        }

        private FileSystemAbstraction getFilesystem( ManagementData management )
        {
            return management.getKernelData().getFilesystemAbstraction();
        }

        private File getStorePath( ManagementData management )
        {
            return management.getKernelData().getStoreDir();
        }
    }
}
