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
package org.neo5j.kernel.impl.factory;

import java.io.File;
import java.util.function.Predicate;

import org.neo5j.graphdb.DependencyResolver;
import org.neo5j.helpers.Service;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.io.fs.watcher.RestartableFileSystemWatcher;
import org.neo5j.io.pagecache.IOLimiter;
import org.neo5j.kernel.NeoStoreDataSource;
import org.neo5j.kernel.api.bolt.BoltConnectionTracker;
import org.neo5j.kernel.api.exceptions.KernelException;
import org.neo5j.kernel.api.security.SecurityModule;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.api.CommitProcessFactory;
import org.neo5j.kernel.impl.api.SchemaWriteGuard;
import org.neo5j.kernel.impl.constraints.ConstraintSemantics;
import org.neo5j.kernel.impl.core.LabelTokenHolder;
import org.neo5j.kernel.impl.core.PropertyKeyTokenHolder;
import org.neo5j.kernel.impl.core.RelationshipTypeTokenHolder;
import org.neo5j.kernel.impl.coreapi.CoreAPIAvailabilityGuard;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacadeFactory.Configuration;
import org.neo5j.kernel.impl.locking.Locks;
import org.neo5j.kernel.impl.locking.StatementLocksFactory;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.proc.Procedures;
import org.neo5j.kernel.impl.store.id.IdGeneratorFactory;
import org.neo5j.kernel.impl.store.id.IdReuseEligibility;
import org.neo5j.kernel.impl.store.id.configuration.IdTypeConfigurationProvider;
import org.neo5j.kernel.impl.transaction.TransactionHeaderInformationFactory;
import org.neo5j.kernel.impl.util.DependencySatisfier;
import org.neo5j.kernel.impl.util.JobScheduler;
import org.neo5j.kernel.impl.util.watcher.DefaultFileDeletionEventListener;
import org.neo5j.kernel.impl.util.watcher.DefaultFileSystemWatcherService;
import org.neo5j.kernel.impl.util.watcher.FileSystemWatcherService;
import org.neo5j.kernel.info.DiagnosticsManager;
import org.neo5j.kernel.internal.KernelDiagnostics;
import org.neo5j.kernel.lifecycle.LifeSupport;
import org.neo5j.logging.Log;
import org.neo5j.udc.UsageData;
import org.neo5j.udc.UsageDataKeys;

import static java.util.Collections.singletonMap;

/**
 * Edition module for {@link org.neo5j.kernel.impl.factory.GraphDatabaseFacadeFactory}. Implementations of this class
 * need to create all the services that would be specific for a particular edition of the database.
 */
public abstract class EditionModule
{
    void registerProcedures( Procedures procedures ) throws KernelException
    {
        procedures.registerProcedure( org.neo5j.kernel.builtinprocs.BuiltInProcedures.class );
        procedures.registerProcedure( org.neo5j.kernel.builtinprocs.TokenProcedures.class );
        procedures.registerProcedure( org.neo5j.kernel.builtinprocs.BuiltInDbmsProcedures.class );

        registerEditionSpecificProcedures( procedures );
    }

    protected abstract void registerEditionSpecificProcedures( Procedures procedures ) throws KernelException;

    public IdGeneratorFactory idGeneratorFactory;
    public IdTypeConfigurationProvider idTypeConfigurationProvider;

    public LabelTokenHolder labelTokenHolder;

    public PropertyKeyTokenHolder propertyKeyTokenHolder;

    public Locks lockManager;

    public StatementLocksFactory statementLocksFactory;

    public CommitProcessFactory commitProcessFactory;

    public long transactionStartTimeout;

    public RelationshipTypeTokenHolder relationshipTypeTokenHolder;

    public TransactionHeaderInformationFactory headerInformationFactory;

    public SchemaWriteGuard schemaWriteGuard;

    public ConstraintSemantics constraintSemantics;

    public CoreAPIAvailabilityGuard coreAPIAvailabilityGuard;

    public AccessCapability accessCapability;

    public IOLimiter ioLimiter;

    public IdReuseEligibility eligibleForIdReuse;

    public FileSystemWatcherService watcherService;

    protected FileSystemWatcherService createFileSystemWatcherService( FileSystemAbstraction fileSystem, File storeDir,
            LogService logging, JobScheduler jobScheduler, Predicate<String> fileNameFilter )
    {
        try
        {
            RestartableFileSystemWatcher watcher = new RestartableFileSystemWatcher( fileSystem.fileWatcher() );
            watcher.addFileWatchEventListener( new DefaultFileDeletionEventListener( logging, fileNameFilter ) );
            watcher.watch( storeDir );
            // register to watch store dir parent folder to see when store dir removed
            watcher.watch( storeDir.getParentFile() );
            return new DefaultFileSystemWatcherService( jobScheduler, watcher );
        }
        catch ( Exception e )
        {
            Log log = logging.getInternalLog( getClass() );
            log.warn( "Can not create file watcher for current file system. File monitoring capabilities for store " +
                    "files will be disabled.", e );
            return FileSystemWatcherService.EMPTY_WATCHER;
        }
    }

    protected void doAfterRecoveryAndStartup( DatabaseInfo databaseInfo, DependencyResolver dependencyResolver )
    {
        DiagnosticsManager diagnosticsManager = dependencyResolver.resolveDependency( DiagnosticsManager.class );
        NeoStoreDataSource neoStoreDataSource = dependencyResolver.resolveDependency( NeoStoreDataSource.class );

        diagnosticsManager.prependProvider( new KernelDiagnostics.Versions(
                databaseInfo, neoStoreDataSource.getStoreId() ) );
        neoStoreDataSource.registerDiagnosticsWith( diagnosticsManager );
        diagnosticsManager.appendProvider( new KernelDiagnostics.StoreFiles( neoStoreDataSource.getStoreDir() ) );
    }

    protected void publishEditionInfo( UsageData sysInfo, DatabaseInfo databaseInfo, Config config )
    {
        sysInfo.set( UsageDataKeys.edition, databaseInfo.edition );
        sysInfo.set( UsageDataKeys.operationalMode, databaseInfo.operationalMode );
        config.augment( singletonMap( Configuration.editionName.name(), databaseInfo.edition.toString() ) );
    }

    public abstract void setupSecurityModule( PlatformModule platformModule, Procedures procedures );

    protected static void setupSecurityModule( PlatformModule platformModule, Log log, Procedures procedures,
            String key )
    {
        for ( SecurityModule candidate : Service.load( SecurityModule.class ) )
        {
            if ( candidate.matches( key ) )
            {
                try
                {
                    candidate.setup( new SecurityModule.Dependencies()
                    {
                        @Override
                        public LogService logService()
                        {
                            return platformModule.logging;
                        }

                        @Override
                        public Config config()
                        {
                            return platformModule.config;
                        }

                        @Override
                        public Procedures procedures()
                        {
                            return procedures;
                        }

                        @Override
                        public JobScheduler scheduler()
                        {
                            return platformModule.jobScheduler;
                        }

                        @Override
                        public FileSystemAbstraction fileSystem()
                        {
                            return platformModule.fileSystem;
                        }

                        @Override
                        public LifeSupport lifeSupport()
                        {
                            return platformModule.life;
                        }

                        @Override
                        public DependencySatisfier dependencySatisfier()
                        {
                            return platformModule.dependencies;
                        }
                    } );
                    return;
                }
                catch ( Exception e )
                {
                    String errorMessage = "Failed to load security module.";
                    log.error( errorMessage );
                    throw new RuntimeException( errorMessage, e );
                }
            }
        }
        String errorMessage = "Failed to load security module with key '" + key + "'.";
        log.error( errorMessage );
        throw new IllegalArgumentException( errorMessage );
    }

    protected BoltConnectionTracker createSessionTracker()
    {
        return BoltConnectionTracker.NOOP;
    }
}
