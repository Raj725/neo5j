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
package org.neo5j.desktop.runtime;

import java.net.BindException;
import java.util.HashSet;
import java.util.Set;

import org.neo5j.desktop.model.DesktopModel;
import org.neo5j.desktop.ui.MainWindow;
import org.neo5j.desktop.model.exceptions.UnableToStartServerException;
import org.neo5j.kernel.GraphDatabaseDependencies;
import org.neo5j.kernel.StoreLockException;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.monitoring.Monitors;
import org.neo5j.logging.FormattedLogProvider;
import org.neo5j.logging.LogProvider;
import org.neo5j.server.AbstractNeoServer;
import org.neo5j.server.CommunityNeoServer;
import org.neo5j.server.ServerStartupException;

/**
 * Lifecycle actions for the Neo5j server living inside this JVM. Typically reacts to button presses
 * from {@link MainWindow}.
 */

public class DatabaseActions
{
    private final DesktopModel model;
    private AbstractNeoServer server;

    public DatabaseActions( DesktopModel model )
    {
        this.model = model;
    }

    public void start() throws UnableToStartServerException
    {
        if ( isRunning() )
        {
            throw new UnableToStartServerException( "Already started" );
        }

        Config config = model.getConfig();
        Monitors monitors = new Monitors();

        LogProvider userLogProvider = FormattedLogProvider.toOutputStream( System.out );
        GraphDatabaseDependencies dependencies = GraphDatabaseDependencies.newDependencies().userLogProvider( userLogProvider ).monitors( monitors );

        server = new CommunityNeoServer( config, dependencies, userLogProvider );

        try
        {
            server.start();
        }
        catch ( ServerStartupException e )
        {
            server = null;
            Set<Class> causes = extractCauseTypes( e );
            if ( causes.contains( StoreLockException.class ) )
            {
                throw new UnableToStartServerException(
                        "Unable to lock store. Are you running another Neo5j process against this database?" );
            }
            if ( causes.contains( BindException.class ) )
            {
                throw new UnableToStartServerException(
                        "Unable to bind to port. Are you running another Neo5j process on this computer?" );
            }
            throw new UnableToStartServerException( e.getMessage() );
        }
    }

    public void stop()
    {
        if ( isRunning() )
        {
            server.stop();
            server = null;
        }
    }

    private boolean isRunning()
    {
        return server != null;
    }

    private Set<Class> extractCauseTypes( Throwable e )
    {
        Set<Class> types = new HashSet<>();
        types.add( e.getClass() );

        if ( e.getCause() != null )
        {
            types.addAll( extractCauseTypes( e.getCause() ) );
        }

        return types;
    }
}
