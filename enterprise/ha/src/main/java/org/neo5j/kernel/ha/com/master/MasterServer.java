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
package org.neo5j.kernel.ha.com.master;

import org.neo5j.com.Protocol;
import org.neo5j.com.RequestContext;
import org.neo5j.com.RequestType;
import org.neo5j.com.Server;
import org.neo5j.com.TxChecksumVerifier;
import org.neo5j.com.monitor.RequestMonitor;
import org.neo5j.kernel.ha.HaRequestType210;
import org.neo5j.kernel.ha.MasterClient320;
import org.neo5j.kernel.impl.transaction.log.ReadableClosablePositionAwareChannel;
import org.neo5j.kernel.impl.transaction.log.entry.LogEntryReader;
import org.neo5j.kernel.monitoring.ByteCounterMonitor;
import org.neo5j.logging.LogProvider;
import org.neo5j.time.Clocks;

import static org.neo5j.kernel.ha.com.slave.MasterClient.CURRENT;

/**
 * Sits on the master side, receiving serialized requests from slaves (via
 * {@link org.neo5j.kernel.ha.com.slave.MasterClient}). Delegates actual work to {@link MasterImpl}.
 */
public class MasterServer extends Server<Master, Void>
{
    public static final int FRAME_LENGTH = Protocol.DEFAULT_FRAME_LENGTH;
    private final ConversationManager conversationManager;
    private final HaRequestType210 requestTypes;

    public MasterServer( Master requestTarget, LogProvider logProvider, Configuration config,
                         TxChecksumVerifier txVerifier, ByteCounterMonitor byteCounterMonitor,
                         RequestMonitor requestMonitor, ConversationManager conversationManager,
                         LogEntryReader<ReadableClosablePositionAwareChannel> entryReader )
    {
        super( requestTarget, config, logProvider, FRAME_LENGTH, CURRENT, txVerifier,
                Clocks.systemClock(), byteCounterMonitor, requestMonitor );
        this.conversationManager = conversationManager;
        this.requestTypes = new HaRequestType210( entryReader, MasterClient320.LOCK_RESULT_OBJECT_SERIALIZER );
    }

    @Override
    protected RequestType<Master> getRequestContext( byte id )
    {
        return requestTypes.type( id );
    }

    @Override
    protected void stopConversation( RequestContext context )
    {
        conversationManager.stop( context );
    }
}
