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

import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.helpers.collection.Iterators;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.server.NeoServer;
import org.neo5j.server.configuration.ServerSettings;
import org.neo5j.server.plugins.Injectable;
import org.neo5j.server.plugins.PluginLifecycle;

import static org.junit.Assert.assertTrue;
import static org.neo5j.helpers.collection.MapUtil.stringMap;


public class ExtensionInitializerTest
{

    @Test
    public void testPluginInitialization()
    {
        Config config = Config.embeddedDefaults( stringMap( ServerSettings.transaction_idle_timeout.name(), "600" ) );
        NeoServer neoServer = Mockito.mock( NeoServer.class, Mockito.RETURNS_DEEP_STUBS );
        Mockito.when( neoServer.getConfig() ).thenReturn( config );
        ExtensionInitializer extensionInitializer = new ExtensionInitializer( neoServer );

        Collection<Injectable<?>> injectableProperties =
                extensionInitializer.initializePackages( Arrays.asList( "org.neo5j.server.modules" ) );

        assertTrue(
                injectableProperties.stream()
                        .anyMatch( i -> ServerSettings
                                .transaction_idle_timeout.name().equals( i.getValue() ) ) );
    }

    public static class PropertyCollectorPlugin implements PluginLifecycle
    {

        @Override
        public Collection<Injectable<?>> start( GraphDatabaseService graphDatabaseService, Configuration config )
        {
            return Iterators.asList( Iterators.map( new StringToInjectableFunction(), config.getKeys() ) );
        }

        @Override
        public void stop()
        {

        }

        private class StringToInjectableFunction implements Function<String,Injectable<?>>
        {

            @Override
            public Injectable<String> apply( final String value )
            {
                return new Injectable<String>()
                {
                    @Override
                    public String getValue()
                    {
                        return value;
                    }

                    @Override
                    public Class<String> getType()
                    {
                        return String.class;
                    }
                };
            }
        }
    }
}
