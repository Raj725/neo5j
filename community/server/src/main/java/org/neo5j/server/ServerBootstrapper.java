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
package org.neo5j.server;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import sun.misc.Signal;

import org.neo5j.graphdb.TransactionFailureException;
import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.helpers.collection.Pair;
import org.neo5j.kernel.GraphDatabaseDependencies;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.configuration.ConfigurationValidator;
import org.neo5j.kernel.configuration.HttpConnector.Encryption;
import org.neo5j.kernel.info.JvmChecker;
import org.neo5j.kernel.info.JvmMetadataRepository;
import org.neo5j.logging.FormattedLogProvider;
import org.neo5j.logging.Log;
import org.neo5j.logging.LogProvider;
import org.neo5j.server.configuration.ConfigLoader;
import org.neo5j.server.logging.JULBridge;
import org.neo5j.server.logging.JettyLogBridge;

import static java.lang.String.format;
import static org.neo5j.commandline.Util.neo5jVersion;

public abstract class ServerBootstrapper implements Bootstrapper
{
    public static final int OK = 0;
    public static final int WEB_SERVER_STARTUP_ERROR_CODE = 1;
    public static final int GRAPH_DATABASE_STARTUP_ERROR_CODE = 2;
    private static final String SIGTERM = "TERM";
    private static final String SIGINT = "INT";

    private volatile NeoServer server;
    private Thread shutdownHook;
    private GraphDatabaseDependencies dependencies = GraphDatabaseDependencies.newDependencies();
    // in case we have errors loading/validating the configuration log to stdout
    private Log log = FormattedLogProvider.toOutputStream( System.out ).getLog( getClass() );
    private String serverAddress = "unknown address";

    public static int start( Bootstrapper boot, String... argv )
    {
        ServerCommandLineArgs args = ServerCommandLineArgs.parse( argv );

        if ( args.version() )
        {
            System.out.println( "neo5j " + neo5jVersion() );
            return 0;
        }

        if ( args.homeDir() == null )
        {
            throw new ServerStartupException( "Argument --home-dir is required and was not provided." );
        }

        return boot.start( args.homeDir(), args.configFile(), args.configOverrides() );
    }

    @Override
    @SafeVarargs
    public final int start( File homeDir, Optional<File> configFile, Pair<String, String>... configOverrides )
    {
        addShutdownHook();
        installSignalHandler();
        try
        {
            Config config = createConfig( homeDir, configFile, configOverrides );

            LogProvider userLogProvider = setupLogging( config );
            dependencies = dependencies.userLogProvider( userLogProvider );
            log = userLogProvider.getLog( getClass() );
            config.setLogger( log );

            serverAddress =  config.httpConnectors().stream()
                    .filter( c -> Encryption.NONE.equals( c.encryptionLevel() ) )
                    .findFirst()
                    .map( ( connector ) -> config.get( connector.listen_address ).toString() )
                    .orElse( serverAddress );

            checkCompatibility();

            server = createNeoServer( config, dependencies, userLogProvider );
            server.start();

            return OK;
        }
        catch ( ServerStartupException e )
        {
            e.describeTo( log );
            return WEB_SERVER_STARTUP_ERROR_CODE;
        }
        catch ( TransactionFailureException tfe )
        {
            String locationMsg = (server == null) ? "" :
                    " Another process may be using database location " + server.getDatabase().getLocation();
            log.error( format( "Failed to start Neo5j on %s.", serverAddress ) + locationMsg, tfe );
            return GRAPH_DATABASE_STARTUP_ERROR_CODE;
        }
        catch ( Exception e )
        {
            log.error( format( "Failed to start Neo5j on %s.", serverAddress ), e );
            return WEB_SERVER_STARTUP_ERROR_CODE;
        }
    }

    public int stop()
    {
        String location = "unknown location";
        try
        {
            if ( server != null )
            {
                server.stop();
            }

            removeShutdownHook();

            return 0;
        }
        catch ( Exception e )
        {
            log.error( "Failed to cleanly shutdown Neo Server on port [%s], database [%s]. Reason [%s] ",
                    serverAddress, location, e.getMessage(), e );
            return 1;
        }
    }

    public boolean isRunning()
    {
        return server != null && server.getDatabase() != null && server.getDatabase().isRunning();
    }

    public NeoServer getServer()
    {
        return server;
    }

    protected abstract NeoServer createNeoServer( Config config, GraphDatabaseDependencies dependencies,
                                                  LogProvider userLogProvider );

    @Nonnull
    protected abstract Collection<ConfigurationValidator> configurationValidators();

    private static LogProvider setupLogging( Config config )
    {
        LogProvider userLogProvider = FormattedLogProvider.withoutRenderingContext()
                            .withDefaultLogLevel( config.get( GraphDatabaseSettings.store_internal_log_level ) )
                            .toOutputStream( System.out );
        JULBridge.resetJUL();
        Logger.getLogger( "" ).setLevel( Level.WARNING );
        JULBridge.forwardTo( userLogProvider );
        JettyLogBridge.setLogProvider( userLogProvider );
        return userLogProvider;
    }

    private Config createConfig( File homeDir, Optional<File> file, Pair<String, String>[] configOverrides )
    {
        return ConfigLoader.loadServerConfig( Optional.of( homeDir ), file, configOverrides,
                configurationValidators() );
    }

    // Exit gracefully if possible
    private void installSignalHandler()
    {
        try
        {
            // SIGTERM is invoked when system service is stopped
            Signal.handle( new Signal( SIGTERM ), ( signal ) -> System.exit( 0 ) );
        }
        catch ( Throwable e )
        {
            log.warn( "Unable to install signal handler. Exit code may not be 0 on graceful shutdown.", e );
        }
        try
        {
            // SIGINT is invoked when user hits ctrl-c  when running `neo5j console`
            Signal.handle( new Signal( SIGINT ), ( signal ) -> System.exit( 0 ) );
        }
        catch ( Throwable e )
        {
            // Happens on IBM JDK with IllegalArgumentException: Signal already used by VM: INT
            log.warn( "Unable to install signal handler. Exit code may not be 0 on graceful shutdown.", e );
        }
    }

    private void addShutdownHook()
    {
        shutdownHook = new Thread()
        {
            @Override
            public void run()
            {
                log.info( "Neo5j Server shutdown initiated by request" );
                if ( server != null )
                {
                    server.stop();
                }
            }
        };
        Runtime.getRuntime().addShutdownHook( shutdownHook );
    }

    private void removeShutdownHook()
    {
        if ( shutdownHook != null )
        {
            if ( !Runtime.getRuntime().removeShutdownHook( shutdownHook ) )
            {
                log.warn( "Unable to remove shutdown hook" );
            }
        }
    }

    private void checkCompatibility()
    {
        new JvmChecker( log, new JvmMetadataRepository() ).checkJvmCompatibilityAndIssueWarning();
    }
}
