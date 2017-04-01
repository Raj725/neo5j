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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.neo5j.dbms.DatabaseManagementSystemSettings;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.factory.CommunityEditionModule;
import org.neo5j.kernel.impl.factory.DatabaseInfo;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacadeFactory;
import org.neo5j.logging.LogProvider;
import org.neo5j.server.database.Database;
import org.neo5j.server.database.LifecycleManagingDatabase.GraphFactory;
import org.neo5j.server.modules.AuthorizationModule;
import org.neo5j.server.modules.ConsoleModule;
import org.neo5j.server.modules.DBMSModule;
import org.neo5j.server.modules.ManagementApiModule;
import org.neo5j.server.modules.Neo5jBrowserModule;
import org.neo5j.server.modules.RESTApiModule;
import org.neo5j.server.modules.SecurityRulesModule;
import org.neo5j.server.modules.ServerModule;
import org.neo5j.server.modules.ThirdPartyJAXRSModule;
import org.neo5j.server.rest.management.AdvertisableService;
import org.neo5j.server.rest.management.JmxService;
import org.neo5j.server.rest.management.console.ConsoleService;
import org.neo5j.server.web.Jetty9WebServer;
import org.neo5j.server.web.WebServer;

import static org.neo5j.server.database.LifecycleManagingDatabase.lifecycleManagingDatabase;

public class CommunityNeoServer extends AbstractNeoServer
{
    protected static final GraphFactory COMMUNITY_FACTORY = ( config, dependencies ) ->
    {
        File storeDir = config.get( DatabaseManagementSystemSettings.database_path );
        return new GraphDatabaseFacadeFactory( DatabaseInfo.COMMUNITY, CommunityEditionModule::new )
                .newFacade( storeDir, config, dependencies );
    };

    public CommunityNeoServer( Config config, GraphDatabaseFacadeFactory.Dependencies dependencies,
            LogProvider logProvider )
    {
        this( config, lifecycleManagingDatabase( COMMUNITY_FACTORY ), dependencies, logProvider );
    }

    public CommunityNeoServer( Config config, Database.Factory dbFactory, GraphDatabaseFacadeFactory.Dependencies
            dependencies, LogProvider logProvider )
    {
        super( config, dbFactory, dependencies, logProvider );
    }

    @Override
    protected Iterable<ServerModule> createServerModules()
    {
        return Arrays.asList(
                new DBMSModule( webServer, getConfig() ),
                new RESTApiModule( webServer, getConfig(), getDependencyResolver(), logProvider ),
                new ManagementApiModule( webServer, getConfig() ),
                new ThirdPartyJAXRSModule( webServer, getConfig(), logProvider, this ),
                new ConsoleModule( webServer, getConfig() ),
                new Neo5jBrowserModule( webServer ),
                createAuthorizationModule(),
                new SecurityRulesModule( webServer, getConfig(), logProvider ) );
    }

    @Override
    protected WebServer createWebServer()
    {
        return new Jetty9WebServer( logProvider, getConfig() );
    }

    @Override
    public Iterable<AdvertisableService> getServices()
    {
        List<AdvertisableService> toReturn = new ArrayList<>( 3 );
        toReturn.add( new ConsoleService( null, null, logProvider, null ) );
        toReturn.add( new JmxService( null, null ) );

        return toReturn;
    }

    protected AuthorizationModule createAuthorizationModule()
    {
        return new AuthorizationModule( webServer, authManagerSupplier, logProvider, getConfig(), getUriWhitelist() );
    }
}