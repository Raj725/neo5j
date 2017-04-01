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
package org.neo5j.server.modules;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.neo5j.concurrent.RecentK;
import org.neo5j.graphdb.DependencyResolver;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.logging.Log;
import org.neo5j.logging.LogProvider;
import org.neo5j.server.configuration.ServerSettings;
import org.neo5j.server.plugins.PluginManager;
import org.neo5j.server.rest.web.BatchOperationService;
import org.neo5j.server.rest.web.CollectUserAgentFilter;
import org.neo5j.server.rest.web.CypherService;
import org.neo5j.server.rest.web.DatabaseMetadataService;
import org.neo5j.server.rest.web.ExtensionService;
import org.neo5j.server.rest.web.ResourcesService;
import org.neo5j.server.rest.web.RestfulGraphDatabase;
import org.neo5j.server.rest.web.TransactionalService;
import org.neo5j.server.web.WebServer;
import org.neo5j.udc.UsageData;
import org.neo5j.udc.UsageDataKeys;

import static java.util.Arrays.asList;

/**
 * Mounts the database REST API.
 */
public class RESTApiModule implements ServerModule
{
    private final Config config;
    private final WebServer webServer;
    private DependencyResolver dependencyResolver;
    private final LogProvider logProvider;
    private final Log log;

    private PluginManager plugins;

    public RESTApiModule( WebServer webServer, Config config, DependencyResolver dependencyResolver,
            LogProvider logProvider )
    {
        this.webServer = webServer;
        this.config = config;
        this.dependencyResolver = dependencyResolver;
        this.logProvider = logProvider;
        this.log = logProvider.getLog( getClass() );
    }

    @Override
    public void start()
    {
        try
        {
            URI restApiUri = restApiUri( );

            webServer.addFilter( new CollectUserAgentFilter( clientNames() ), "/*" );
            webServer.addJAXRSClasses( getClassNames(), restApiUri.toString(), null );
            loadPlugins();
        }
        catch ( URISyntaxException e )
        {
            log.warn( "Unable to mount REST API", e );
        }
    }

    /**
     * The modules are instantiated before the database is, meaning we can't access the UsageData service before we
     * start. This resolves UsageData at start time.
     *
     * Obviously needs to be refactored, pending discussion on unifying module frameworks between kernel and server
     * and hashing out associated dependency hierarchy and lifecycles.
     */
    private RecentK<String> clientNames()
    {
        return dependencyResolver.resolveDependency( UsageData.class ).get( UsageDataKeys.clientNames );
    }

    private List<String> getClassNames()
    {
        return asList(
                RestfulGraphDatabase.class.getName(),
                TransactionalService.class.getName(),
                CypherService.class.getName(),
                DatabaseMetadataService.class.getName(),
                ExtensionService.class.getName(),
                ResourcesService.class.getName(),
                BatchOperationService.class.getName() );
    }

    @Override
    public void stop()
    {
        try
        {
            webServer.removeJAXRSClasses( getClassNames(), restApiUri().toString() );
            unloadPlugins();
        }
        catch ( URISyntaxException e )
        {
            log.warn( "Unable to unmount REST API", e );
        }
    }

    private URI restApiUri() throws URISyntaxException
    {
        return config.get( ServerSettings.rest_api_path );
    }

    private void loadPlugins()
    {
        plugins = new PluginManager( config, logProvider );
    }

    private void unloadPlugins()
    {
        // TODO
    }

    public PluginManager getPlugins()
    {
        return plugins;
    }
}
