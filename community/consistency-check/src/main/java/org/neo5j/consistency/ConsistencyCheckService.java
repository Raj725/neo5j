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
package org.neo5j.consistency;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.neo5j.consistency.checking.full.CheckConsistencyConfig;
import org.neo5j.consistency.checking.full.ConsistencyCheckIncompleteException;
import org.neo5j.consistency.checking.full.FullCheck;
import org.neo5j.consistency.report.ConsistencySummaryStatistics;
import org.neo5j.consistency.statistics.AccessStatistics;
import org.neo5j.consistency.statistics.AccessStatsKeepingStoreAccess;
import org.neo5j.consistency.statistics.DefaultCounts;
import org.neo5j.consistency.statistics.Statistics;
import org.neo5j.consistency.statistics.VerboseStatistics;
import org.neo5j.function.Suppliers;
import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.graphdb.factory.GraphDatabaseSettings.LabelIndex;
import org.neo5j.helpers.progress.ProgressMonitorFactory;
import org.neo5j.io.fs.DefaultFileSystemAbstraction;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.io.pagecache.PageCache;
import org.neo5j.io.pagecache.tracing.PageCacheTracer;
import org.neo5j.io.pagecache.tracing.cursor.PageCursorTracerSupplier;
import org.neo5j.kernel.api.direct.DirectStoreAccess;
import org.neo5j.kernel.api.index.SchemaIndexProvider;
import org.neo5j.kernel.api.labelscan.LabelScanStore;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.extension.KernelExtensionFactory;
import org.neo5j.kernel.extension.KernelExtensions;
import org.neo5j.kernel.extension.dependency.HighestSelectionStrategy;
import org.neo5j.kernel.extension.dependency.NamedLabelScanStoreSelectionStrategy;
import org.neo5j.kernel.impl.api.index.IndexStoreView;
import org.neo5j.kernel.impl.api.scan.LabelScanStoreProvider;
import org.neo5j.kernel.impl.locking.LockService;
import org.neo5j.kernel.impl.logging.SimpleLogService;
import org.neo5j.kernel.impl.pagecache.ConfiguringPageCacheFactory;
import org.neo5j.kernel.impl.spi.KernelContext;
import org.neo5j.kernel.impl.spi.SimpleKernelContext;
import org.neo5j.kernel.impl.store.NeoStores;
import org.neo5j.kernel.impl.store.StoreAccess;
import org.neo5j.kernel.impl.store.StoreFactory;
import org.neo5j.kernel.impl.store.id.DefaultIdGeneratorFactory;
import org.neo5j.kernel.impl.transaction.state.storeview.NeoStoreIndexStoreView;
import org.neo5j.kernel.impl.util.Dependencies;
import org.neo5j.kernel.lifecycle.LifeSupport;
import org.neo5j.logging.DuplicatingLog;
import org.neo5j.logging.Log;
import org.neo5j.logging.LogProvider;

import static java.lang.String.format;
import static org.neo5j.helpers.Service.load;
import static org.neo5j.helpers.collection.MapUtil.stringMap;
import static org.neo5j.io.file.Files.createOrOpenAsOuputStream;
import static org.neo5j.kernel.configuration.Settings.TRUE;
import static org.neo5j.kernel.extension.UnsatisfiedDependencyStrategies.ignore;
import static org.neo5j.kernel.impl.factory.DatabaseInfo.UNKNOWN;

public class ConsistencyCheckService
{
    private final Date timestamp;

    public ConsistencyCheckService()
    {
        this( new Date() );
    }

    public ConsistencyCheckService( Date timestamp )
    {
        this.timestamp = timestamp;
    }

    @Deprecated
    public Result runFullConsistencyCheck( File storeDir, Config tuningConfiguration,
            ProgressMonitorFactory progressFactory, LogProvider logProvider, boolean verbose )
            throws ConsistencyCheckIncompleteException, IOException
    {
        return runFullConsistencyCheck( storeDir, tuningConfiguration, progressFactory, logProvider, verbose,
                new CheckConsistencyConfig(tuningConfiguration) );
    }

    public Result runFullConsistencyCheck( File storeDir, Config config, ProgressMonitorFactory progressFactory,
            LogProvider logProvider, boolean verbose, CheckConsistencyConfig checkConsistencyConfig )
            throws ConsistencyCheckIncompleteException, IOException
    {
        FileSystemAbstraction fileSystem = new DefaultFileSystemAbstraction();
        try
        {
            return runFullConsistencyCheck( storeDir, config, progressFactory, logProvider,
                    fileSystem, verbose, checkConsistencyConfig );
        }
        finally
        {
            try
            {
                fileSystem.close();
            }
            catch ( IOException e )
            {
                Log log = logProvider.getLog( getClass() );
                log.error( "Failure during shutdown of file system", e );
            }
        }
    }

    @Deprecated
    public Result runFullConsistencyCheck( File storeDir, Config tuningConfiguration,
            ProgressMonitorFactory progressFactory, LogProvider logProvider, FileSystemAbstraction fileSystem,
            boolean verbose ) throws ConsistencyCheckIncompleteException, IOException
    {
        return runFullConsistencyCheck( storeDir, tuningConfiguration, progressFactory, logProvider, fileSystem,
                verbose, new CheckConsistencyConfig(tuningConfiguration) );
    }

    public Result runFullConsistencyCheck( File storeDir, Config config, ProgressMonitorFactory progressFactory,
            LogProvider logProvider, FileSystemAbstraction fileSystem, boolean verbose,
            CheckConsistencyConfig checkConsistencyConfig ) throws ConsistencyCheckIncompleteException, IOException
    {
        return runFullConsistencyCheck( storeDir, config, progressFactory, logProvider, fileSystem,
                verbose, defaultReportDir( config, storeDir ), checkConsistencyConfig );
    }

    @Deprecated
    public Result runFullConsistencyCheck( File storeDir, Config tuningConfiguration,
            ProgressMonitorFactory progressFactory, LogProvider logProvider, FileSystemAbstraction fileSystem,
            boolean verbose, File reportDir ) throws ConsistencyCheckIncompleteException, IOException
    {
        return runFullConsistencyCheck( storeDir, tuningConfiguration, progressFactory, logProvider, fileSystem,
                verbose, reportDir, new CheckConsistencyConfig(tuningConfiguration ) );
    }

    public Result runFullConsistencyCheck( File storeDir, Config config, ProgressMonitorFactory progressFactory,
            LogProvider logProvider, FileSystemAbstraction fileSystem, boolean verbose, File reportDir,
            CheckConsistencyConfig checkConsistencyConfig ) throws ConsistencyCheckIncompleteException, IOException
    {
        Log log = logProvider.getLog( getClass() );
        ConfiguringPageCacheFactory pageCacheFactory = new ConfiguringPageCacheFactory(
                fileSystem, config, PageCacheTracer.NULL, PageCursorTracerSupplier.NULL,
                logProvider.getLog( PageCache.class ) );
        PageCache pageCache = pageCacheFactory.getOrCreatePageCache();

        try
        {
            return runFullConsistencyCheck( storeDir, config, progressFactory, logProvider, fileSystem,
                    pageCache, verbose, reportDir, checkConsistencyConfig );
        }
        finally
        {
            try
            {
                pageCache.close();
            }
            catch ( Exception e )
            {
                log.error( "Failure during shutdown of the page cache", e );
            }
        }
    }

    @Deprecated
    public Result runFullConsistencyCheck( final File storeDir, Config tuningConfiguration,
            ProgressMonitorFactory progressFactory, final LogProvider logProvider,
            final FileSystemAbstraction fileSystem, final PageCache pageCache, final boolean verbose )
            throws ConsistencyCheckIncompleteException
    {
        return runFullConsistencyCheck( storeDir, tuningConfiguration, progressFactory, logProvider, fileSystem,
                pageCache, verbose, new CheckConsistencyConfig(tuningConfiguration ) );
    }

    public Result runFullConsistencyCheck( final File storeDir, Config config, ProgressMonitorFactory progressFactory,
            final LogProvider logProvider, final FileSystemAbstraction fileSystem, final PageCache pageCache,
            final boolean verbose, CheckConsistencyConfig checkConsistencyConfig )
            throws ConsistencyCheckIncompleteException
    {
        return runFullConsistencyCheck( storeDir,  config, progressFactory, logProvider, fileSystem, pageCache,
                verbose, defaultReportDir( config, storeDir ), checkConsistencyConfig );
    }

    @Deprecated
    public Result runFullConsistencyCheck( final File storeDir, Config tuningConfiguration,
            ProgressMonitorFactory progressFactory, final LogProvider logProvider,
            final FileSystemAbstraction fileSystem, final PageCache pageCache, final boolean verbose, File reportDir )
            throws ConsistencyCheckIncompleteException
    {
        return runFullConsistencyCheck( storeDir, tuningConfiguration, progressFactory, logProvider, fileSystem,
                pageCache, verbose, reportDir, new CheckConsistencyConfig(tuningConfiguration) );
    }

    public Result runFullConsistencyCheck( final File storeDir, Config config, ProgressMonitorFactory progressFactory,
            final LogProvider logProvider, final FileSystemAbstraction fileSystem, final PageCache pageCache,
            final boolean verbose, File reportDir, CheckConsistencyConfig checkConsistencyConfig )
            throws ConsistencyCheckIncompleteException
    {
        Log log = logProvider.getLog( getClass() );
        config = config.with( stringMap(
                GraphDatabaseSettings.read_only.name(), TRUE,
                GraphDatabaseSettings.label_index.name(), LabelIndex.AUTO.name() ) );
        StoreFactory factory = new StoreFactory( storeDir, config,
                new DefaultIdGeneratorFactory( fileSystem ), pageCache, fileSystem, logProvider );

        ConsistencySummaryStatistics summary;
        final File reportFile = chooseReportPath( reportDir );
        Log reportLog = new ConsistencyReportLog( Suppliers.lazySingleton( () ->
        {
            try
            {
                return new PrintWriter( createOrOpenAsOuputStream( fileSystem, reportFile, true ) );
            }
            catch ( IOException e )
            {
                throw new RuntimeException( e );
            }
        } ) );

        // Bootstrap kernel extensions
        LifeSupport life = new LifeSupport();
        try ( NeoStores neoStores = factory.openAllNeoStores() )
        {
            IndexStoreView indexStoreView = new NeoStoreIndexStoreView( LockService.NO_LOCK_SERVICE, neoStores );
            Dependencies dependencies = new Dependencies();
            dependencies.satisfyDependencies( config, fileSystem,
                    new SimpleLogService( logProvider, logProvider ), indexStoreView, pageCache );
            KernelContext kernelContext = new SimpleKernelContext( storeDir, UNKNOWN, dependencies );
            KernelExtensions extensions = life.add( new KernelExtensions(
                    kernelContext, (Iterable) load( KernelExtensionFactory.class ), dependencies, ignore() ) );
            life.start();
            LabelScanStore labelScanStore = life.add( extensions.resolveDependency( LabelScanStoreProvider.class,
                    new NamedLabelScanStoreSelectionStrategy( config ) ).getLabelScanStore() );
            SchemaIndexProvider indexes = life.add( extensions.resolveDependency( SchemaIndexProvider.class,
                    HighestSelectionStrategy.getInstance() ) );

            int numberOfThreads = defaultConsistencyCheckThreadsNumber();
            Statistics statistics;
            StoreAccess storeAccess;
            AccessStatistics stats = new AccessStatistics();
            if ( verbose )
            {
                statistics = new VerboseStatistics( stats, new DefaultCounts( numberOfThreads ), log );
                storeAccess = new AccessStatsKeepingStoreAccess( neoStores, stats );
            }
            else
            {
                statistics = Statistics.NONE;
                storeAccess = new StoreAccess( neoStores );
            }
            storeAccess.initialize();
            DirectStoreAccess stores = new DirectStoreAccess( storeAccess, labelScanStore, indexes );
            FullCheck check = new FullCheck( progressFactory, statistics, numberOfThreads, checkConsistencyConfig );
            summary = check.execute( stores, new DuplicatingLog( log, reportLog ) );
        }
        finally
        {
            life.shutdown();
        }

        if ( !summary.isConsistent() )
        {
            log.warn( "See '%s' for a detailed consistency report.", reportFile.getPath() );
            return Result.failure( reportFile );
        }
        return Result.success( reportFile );
    }

    private File chooseReportPath( File reportDir )
    {
        return new File( reportDir, defaultLogFileName( timestamp ) );
    }

    private File defaultReportDir( Config tuningConfiguration, File storeDir )
    {
        if ( tuningConfiguration.get( GraphDatabaseSettings.neo5j_home ) == null )
        {
            tuningConfiguration = tuningConfiguration.with(
                    stringMap( GraphDatabaseSettings.neo5j_home.name(), storeDir.getAbsolutePath() ) );
        }

        return tuningConfiguration.get( GraphDatabaseSettings.logs_directory );
    }

    private static String defaultLogFileName( Date date )
    {
        return format( "inconsistencies-%s.report", new SimpleDateFormat( "yyyy-MM-dd.HH.mm.ss" ).format( date ) );
    }

    public interface Result
    {
        static Result failure( File reportFile )
        {
            return new Result()
            {
                @Override
                public boolean isSuccessful()
                {
                    return false;
                }

                @Override
                public File reportFile()
                {
                    return reportFile;
                }
            };
        }

        static Result success( File reportFile )
        {
            return new Result()
            {
                @Override
                public boolean isSuccessful()
                {
                    return true;
                }

                @Override
                public File reportFile()
                {
                    return reportFile;
                }
            };
        }

        boolean isSuccessful();

        File reportFile();
    }

    public static int defaultConsistencyCheckThreadsNumber()
    {
        return Math.max( 1, Runtime.getRuntime().availableProcessors() - 1 );
    }
}
