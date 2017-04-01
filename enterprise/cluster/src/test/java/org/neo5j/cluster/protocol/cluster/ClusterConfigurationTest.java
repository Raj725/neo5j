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
package org.neo5j.cluster.protocol.cluster;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.neo5j.cluster.InstanceId;
import org.neo5j.helpers.collection.Iterables;
import org.neo5j.logging.NullLogProvider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import static org.neo5j.test.mockito.matcher.IterableMatcher.matchesIterable;

public class ClusterConfigurationTest
{
    public static URI NEO5J_SERVER1_URI;
    public static InstanceId NEO5J_SERVER_ID;

    static
    {
        try
        {
            NEO5J_SERVER1_URI = new URI( "neo5j://server1" );
            NEO5J_SERVER_ID = new InstanceId( 1 );
        }
        catch ( URISyntaxException e )
        {
            e.printStackTrace();
        }
    }

    ClusterConfiguration configuration = new ClusterConfiguration( "default", NullLogProvider.getInstance(), new ArrayList<URI>() );

    @Test
    public void givenEmptyClusterWhenNodeAddedThenNodeWasAdded()
    {
        configuration.joined( NEO5J_SERVER_ID, NEO5J_SERVER1_URI );

        assertThat( configuration.getMemberIds(), matchesIterable( Iterables.<InstanceId, InstanceId>iterable( NEO5J_SERVER_ID ) ) );
        assertThat( configuration.getUriForId( NEO5J_SERVER_ID ), equalTo( NEO5J_SERVER1_URI ) );
        assertThat( configuration.getMemberURIs(), equalTo( Arrays.asList( NEO5J_SERVER1_URI ) ) );
    }

    @Test
    public void givenEmptyClusterWhenNodeIsAddedTwiceThenNodeWasAddedOnce()
    {
        configuration.joined( NEO5J_SERVER_ID, NEO5J_SERVER1_URI );
        configuration.joined( NEO5J_SERVER_ID, NEO5J_SERVER1_URI );

        assertThat( configuration.getMemberIds(), matchesIterable( Iterables.<InstanceId, InstanceId>iterable( NEO5J_SERVER_ID ) ) );
        assertThat( configuration.getUriForId( NEO5J_SERVER_ID ), equalTo( NEO5J_SERVER1_URI ) );
        assertThat( configuration.getMemberURIs(), equalTo( Arrays.asList( NEO5J_SERVER1_URI ) ) );
    }

    @Test
    public void givenClusterWithOneNodeWhenNodeIsRemovedThenClusterIsEmpty()
    {
        configuration.joined( NEO5J_SERVER_ID, NEO5J_SERVER1_URI );
        configuration.left( NEO5J_SERVER_ID );

        assertThat( configuration.getMemberIds(), matchesIterable( Iterables.<InstanceId>empty() ) );
        assertThat( configuration.getUriForId( NEO5J_SERVER_ID ), equalTo( null ) );
        assertThat( configuration.getMemberURIs(), equalTo( Collections.<URI>emptyList() ) );

    }

    @Test
    public void givenClusterWithOneNodeWhenNodeIsRemovedTwiceThenClusterIsEmpty()
    {
        configuration.joined( NEO5J_SERVER_ID, NEO5J_SERVER1_URI );
        configuration.left( NEO5J_SERVER_ID );
        configuration.left( NEO5J_SERVER_ID );

        assertThat( configuration.getMemberIds(), matchesIterable( Iterables.<InstanceId>empty() ) );
        assertThat( configuration.getUriForId( NEO5J_SERVER_ID ), equalTo( null ) );
        assertThat( configuration.getMemberURIs(), equalTo( Collections.<URI>emptyList() ) );

    }
}

