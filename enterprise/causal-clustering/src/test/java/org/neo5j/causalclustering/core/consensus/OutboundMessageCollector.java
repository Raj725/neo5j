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
package org.neo5j.causalclustering.core.consensus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo5j.causalclustering.messaging.Message;
import org.neo5j.causalclustering.core.consensus.log.RaftLogEntry;
import org.neo5j.causalclustering.messaging.Outbound;
import org.neo5j.causalclustering.identity.MemberId;

public class OutboundMessageCollector implements Outbound<MemberId, RaftMessages.RaftMessage>
{
    Map<MemberId, List<RaftMessages.RaftMessage>> sentMessages = new HashMap<>();

    public void clear()
    {
        sentMessages.clear();
    }

    @Override
    public void send( MemberId to, RaftMessages.RaftMessage message )
    {
        raftMessages( to ).add( message );
    }

    private List<RaftMessages.RaftMessage> raftMessages( MemberId to )
    {
        List<RaftMessages.RaftMessage> messagesToMember = sentMessages.get( to );
        if ( messagesToMember == null )
        {
            messagesToMember = new ArrayList<>();
            sentMessages.put( to, messagesToMember );
        }
        return messagesToMember;
    }

    public List<RaftMessages.RaftMessage> sentTo( MemberId member )
    {
        List<RaftMessages.RaftMessage> messages = sentMessages.get( member );

        if ( messages == null )
        {
            messages = new ArrayList<>();
        }

        return messages;
    }

    public boolean hasAnyEntriesTo( MemberId member )
    {
        List<RaftMessages.RaftMessage> messages = sentMessages.get( member );
        return messages != null && messages.size() != 0;
    }

    public boolean hasEntriesTo( MemberId member, RaftLogEntry... expectedMessages )
    {
        List<RaftLogEntry> actualMessages = new ArrayList<>();

        for ( Message message : sentTo( member ) )
        {
            if ( message instanceof RaftMessages.AppendEntries.Request )
            {
                Collections.addAll( actualMessages, ((RaftMessages.AppendEntries.Request) message).entries() );
            }
        }

        return actualMessages.containsAll( Arrays.asList( expectedMessages ) );
    }
}
