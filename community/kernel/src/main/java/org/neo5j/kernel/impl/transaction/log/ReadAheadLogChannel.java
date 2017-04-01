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
package org.neo5j.kernel.impl.transaction.log;

import java.io.IOException;

import org.neo5j.io.fs.StoreChannel;

/**
 * Basically a sequence of {@link StoreChannel channels} seamlessly seen as one.
 */
public class ReadAheadLogChannel extends ReadAheadChannel<LogVersionedStoreChannel> implements ReadableLogChannel
{
    private final LogVersionBridge bridge;

    public ReadAheadLogChannel( LogVersionedStoreChannel startingChannel, LogVersionBridge bridge )
    {
        this( startingChannel, bridge, DEFAULT_READ_AHEAD_SIZE );
    }

    public ReadAheadLogChannel( LogVersionedStoreChannel startingChannel, LogVersionBridge bridge, int readAheadSize )
    {
        super(startingChannel, readAheadSize);
        this.bridge = bridge;
    }

    @Override
    public long getVersion()
    {
        return channel.getVersion();
    }

    @Override
    public byte getLogFormatVersion()
    {
        return channel.getLogFormatVersion();
    }

    @Override
    public LogPositionMarker getCurrentPosition( LogPositionMarker positionMarker ) throws IOException
    {
        positionMarker.mark( channel.getVersion(), position() );
        return positionMarker;
    }

    @Override
    protected LogVersionedStoreChannel next( LogVersionedStoreChannel channel ) throws IOException
    {
        return bridge.next( channel );
    }
}