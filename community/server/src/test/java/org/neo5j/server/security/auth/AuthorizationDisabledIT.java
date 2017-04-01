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
package org.neo5j.server.security.auth;

import org.junit.After;
import org.junit.Test;

import org.neo5j.graphdb.factory.GraphDatabaseSettings;
import org.neo5j.server.CommunityNeoServer;
import org.neo5j.server.helpers.CommunityServerBuilder;
import org.neo5j.test.server.ExclusiveServerTestBase;
import org.neo5j.test.server.HTTP;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.neo5j.test.server.HTTP.RawPayload.quotedJson;

public class AuthorizationDisabledIT extends ExclusiveServerTestBase
{

    private CommunityNeoServer server;

    @Test
    public void shouldAllowDisablingAuthorization() throws Exception
    {
        // Given
        server = CommunityServerBuilder.server().withProperty( GraphDatabaseSettings.auth_enabled.name(), "false" ).build();

        // When
        server.start();

        // Then I should have write access
        HTTP.Response response = HTTP.POST( server.baseUri().resolve( "db/data/node" ).toString(), quotedJson( "{'name':'My Node'}" ) );
        assertThat(response.status(), equalTo(201));
        String node = response.location();

        // Then I should have read access
        assertThat( HTTP.GET( node ).status(), equalTo( 200 ) );
    }

    @After
    public void cleanup()
    {
        if ( server != null )
        {
            server.stop();
        }
    }
}
