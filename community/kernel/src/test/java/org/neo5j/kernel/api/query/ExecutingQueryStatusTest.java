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
package org.neo5j.kernel.api.query;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.neo5j.kernel.impl.locking.ActiveLock;
import org.neo5j.storageengine.api.lock.ResourceType;
import org.neo5j.storageengine.api.lock.WaitStrategy;
import org.neo5j.test.FakeCpuClock;
import org.neo5j.time.Clocks;
import org.neo5j.time.FakeClock;

import static org.junit.Assert.assertEquals;

public class ExecutingQueryStatusTest
{
    private final FakeClock clock = Clocks.fakeClock( ZonedDateTime.parse( "2016-12-16T16:14:12+01:00" ) );

    @Test
    public void shouldProduceSensibleMapRepresentationInRunningState() throws Exception
    {
        // when
        String status = SimpleState.running().name();

        // then
        assertEquals( "running", status );
    }

    @Test
    public void shouldProduceSensibleMapRepresentationInPlanningState() throws Exception
    {
        // when
        String status = SimpleState.planning().name();

        // then
        assertEquals( "planning", status );
    }

    @Test
    public void shouldProduceSensibleMapRepresentationInWaitingOnLockState() throws Exception
    {
        // given
        long[] resourceIds = {17};
        WaitingOnLock status =
                new WaitingOnLock(
                        ActiveLock.EXCLUSIVE_MODE,
                        resourceType( "NODE" ),
                        resourceIds,
                        clock.nanos() );
        clock.forward( 17, TimeUnit.MILLISECONDS );

        // when
        Map<String,Object> statusMap = status.toMap( clock.nanos() );

        // then
        assertEquals( "waiting", status.name() );
        Map<String,Object> expected = new HashMap<>();
        expected.put( "waitTimeMillis", 17L );
        expected.put( "lockMode", "EXCLUSIVE" );
        expected.put( "resourceType", "NODE" );
        expected.put( "resourceIds", resourceIds );
        assertEquals( expected, statusMap );
    }

    @Test
    public void shouldProduceSensibleMapRepresentationInWaitingOnQueryState() throws Exception
    {
        // given
        WaitingOnQuery status =
                new WaitingOnQuery(
                        new ExecutingQuery(
                                12,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                Thread.currentThread(),
                                clock,
                                FakeCpuClock.CPU_CLOCK ), clock.nanos() );
        clock.forward( 1025, TimeUnit.MILLISECONDS );

        // when
        Map<String,Object> statusMap = status.toMap( clock.nanos() );

        // then
        assertEquals( "waiting", status.name() );
        Map<String,Object> expected = new HashMap<>();
        expected.put( "waitTimeMillis", 1025L );
        expected.put( "queryId", "query-12" );
        assertEquals( expected, statusMap );
    }

    static ResourceType resourceType(String name )
    {
        return new ResourceType()
        {
            @Override
            public String toString()
            {
                return name();
            }

            @Override
            public int typeId()
            {
                throw new UnsupportedOperationException( "not used" );
            }

            @Override
            public WaitStrategy waitStrategy()
            {
                throw new UnsupportedOperationException( "not used" );
            }

            @Override
            public String name()
            {
                return name;
            }
        };
    }
}