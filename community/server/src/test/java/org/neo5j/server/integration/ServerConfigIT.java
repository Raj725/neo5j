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
package org.neo5j.server.integration;

import java.io.File;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.management.ObjectName;

import org.neo5j.kernel.configuration.Settings;
import org.neo5j.jmx.impl.ConfigurationBean;
import org.neo5j.server.CommunityNeoServer;
import org.neo5j.server.configuration.ServerSettings;
import org.neo5j.server.helpers.CommunityServerBuilder;
import org.neo5j.shell.ShellException;
import org.neo5j.shell.ShellLobby;
import org.neo5j.shell.ShellSettings;
import org.neo5j.test.server.ExclusiveServerTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.neo5j.jmx.JmxUtils.getAttribute;
import static org.neo5j.jmx.JmxUtils.getObjectName;

public class ServerConfigIT extends ExclusiveServerTestBase
{
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private CommunityNeoServer server;

    @Test
    public void serverConfigShouldBeVisibleInJMX() throws Throwable
    {
        // Given
        String configValue = tempDir.newFile().getAbsolutePath();
        server = CommunityServerBuilder.server().withProperty(
        ServerSettings.run_directory.name(), configValue ).build();

        // When
        server.start();

        // Then
        ObjectName name = getObjectName( server.getDatabase().getGraph(), ConfigurationBean.CONFIGURATION_MBEAN_NAME );
        File attr = getAttribute( name, ServerSettings.run_directory.name() );
        assertThat( attr.toString(), equalTo( configValue ) );
    }

    @Test
    public void shouldBeAbleToOverrideShellConfig()  throws Throwable
    {
        // Given
        final int customPort = findFreeShellPortToUse( 8881 );

        server = CommunityServerBuilder.server()
                .withProperty( ShellSettings.remote_shell_enabled.name(), Settings.TRUE )
                .withProperty( ShellSettings.remote_shell_port.name(), "" + customPort )
                .build();

        // When
        this.server.start();

        // Then
        // Try to connect with a shell client to that custom port.
        // Throws exception if unable to connect
        ShellLobby.newClient( customPort )
                .shutdown();
    }

    @Test
    public void shouldNotBeAbleToConnectWithShellOnDefaultPortWhenNoShellConfigSupplied() throws Throwable
    {
        // Given
        server = CommunityServerBuilder.server().build();

        // When
        server.start();

        // Then
        try
        {
            ShellLobby.newClient().shutdown();
            fail( "Should not have been able to connect a shell client" );
        }
        catch ( Exception e )
        {
            assertThat( "Should have been got connection refused", e.getMessage(),
                    containsString( "Connection refused" ) );
        }
    }

    private int findFreeShellPortToUse( int startingPort )
    {
        // Make sure there's no other random stuff on that port
        while ( true )
        {
            try
            {
                ShellLobby.newClient( startingPort++ ).shutdown();
            }
            catch ( ShellException e )
            {   // Good
                return startingPort;
            }
        }
    }

    @After
    public void cleanup()
    {
        server.stop();
    }
}
