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
package org.neo5j.kernel.ha.com.slave;

import org.neo5j.com.ProtocolVersion;
import org.neo5j.com.RequestContext;
import org.neo5j.com.RequestType;
import org.neo5j.com.Server;
import org.neo5j.com.monitor.RequestMonitor;
import org.neo5j.kernel.ha.com.master.Slave;
import org.neo5j.kernel.ha.com.master.SlaveClient.SlaveRequestType;
import org.neo5j.kernel.monitoring.ByteCounterMonitor;
import org.neo5j.logging.LogProvider;
import org.neo5j.time.Clocks;

import static org.neo5j.com.Protocol.DEFAULT_FRAME_LENGTH;
import static org.neo5j.com.ProtocolVersion.INTERNAL_PROTOCOL_VERSION;
import static org.neo5j.com.TxChecksumVerifier.ALWAYS_MATCH;

public class SlaveServer extends Server<Slave, Void>
{
    public static final byte APPLICATION_PROTOCOL_VERSION = 1;
    public static final ProtocolVersion SLAVE_PROTOCOL_VERSION =
            new ProtocolVersion( (byte) 1, INTERNAL_PROTOCOL_VERSION );

    public SlaveServer( Slave requestTarget, Configuration config, LogProvider logProvider, ByteCounterMonitor byteCounterMonitor, RequestMonitor requestMonitor )
    {
        super( requestTarget, config, logProvider, DEFAULT_FRAME_LENGTH, SLAVE_PROTOCOL_VERSION, ALWAYS_MATCH,
                Clocks.systemClock(), byteCounterMonitor, requestMonitor );
    }

    @Override
    protected RequestType<Slave> getRequestContext( byte id )
    {
        return SlaveRequestType.values()[id];
    }

    @Override
    protected void stopConversation( RequestContext context )
    {
    }
}
