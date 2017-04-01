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
package org.neo5j.kernel.api.impl.labelscan;

import java.util.function.Supplier;

import org.neo5j.graphdb.factory.GraphDatabaseSettings.LabelIndex;
import org.neo5j.helpers.Service;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.kernel.NeoStoreDataSource;
import org.neo5j.kernel.api.impl.index.storage.DirectoryFactory;
import org.neo5j.kernel.api.labelscan.LabelScanStore.Monitor;
import org.neo5j.kernel.api.labelscan.LoggingMonitor;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.extension.KernelExtensionFactory;
import org.neo5j.kernel.impl.api.index.IndexStoreView;
import org.neo5j.kernel.impl.api.scan.FullLabelStream;
import org.neo5j.kernel.impl.api.scan.LabelScanStoreProvider;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacadeFactory;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.spi.KernelContext;
import org.neo5j.kernel.lifecycle.Lifecycle;
import org.neo5j.logging.LogProvider;

import static org.neo5j.kernel.api.impl.index.LuceneKernelExtensions.directoryFactory;

@Service.Implementation(KernelExtensionFactory.class)
public class LuceneLabelScanStoreExtension extends KernelExtensionFactory<LuceneLabelScanStoreExtension.Dependencies>
{
    private static final String NAME = LabelIndex.LUCENE.name();

    private final Monitor monitor;

    public interface Dependencies
    {
        Config getConfig();

        /**
         * @return a {@link Supplier} of {@link IndexStoreView}, sort of like a delayed dependency lookup.
         * This is because we need the {@link IndexStoreView} dependency, although at the stage where we
         * grab dependencies, in {@link Lifecycle#init() init} that is, the {@link NeoStoreDataSource} hasn't been
         * {@link Lifecycle#start() started} yet and so haven't provided it.
         */
        Supplier<IndexStoreView> indexStoreView();

        LogService getLogService();

        FileSystemAbstraction fileSystem();
    }

    public LuceneLabelScanStoreExtension()
    {
        this( Monitor.EMPTY );
    }

    LuceneLabelScanStoreExtension( Monitor monitor )
    {
        super( "lucene-scan-store" );
        this.monitor = monitor;
    }

    @Override
    public LabelScanStoreProvider newInstance( KernelContext context, Dependencies dependencies ) throws Throwable
    {
        Config config = dependencies.getConfig();
        boolean ephemeral = config.get( GraphDatabaseFacadeFactory.Configuration.ephemeral );
        FileSystemAbstraction fileSystem = dependencies.fileSystem();
        DirectoryFactory directoryFactory = directoryFactory( ephemeral, fileSystem );

        LuceneLabelScanIndexBuilder indexBuilder = getIndexBuilder( context, directoryFactory, fileSystem, config );
        LogProvider logger = dependencies.getLogService().getInternalLogProvider();
        Monitor loggingMonitor = new LoggingMonitor( logger.getLog( LuceneLabelScanStore.class ), monitor );
        LuceneLabelScanStore scanStore = new LuceneLabelScanStore( indexBuilder,
                new FullLabelStream( dependencies.indexStoreView() ), loggingMonitor );

        return new LabelScanStoreProvider( NAME, scanStore );
    }

    private LuceneLabelScanIndexBuilder getIndexBuilder( KernelContext context, DirectoryFactory directoryFactory,
            FileSystemAbstraction fileSystem, Config config )
    {
        return LuceneLabelScanIndexBuilder.create()
                .withDirectoryFactory( directoryFactory )
                .withFileSystem( fileSystem )
                .withIndexRootFolder( LabelScanStoreProvider.getStoreDirectory( context.storeDir() ) )
                .withConfig( config );
    }
}
