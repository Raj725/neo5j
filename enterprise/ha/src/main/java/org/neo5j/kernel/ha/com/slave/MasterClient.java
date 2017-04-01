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

import org.neo5j.com.ComExceptionHandler;
import org.neo5j.com.Deserializer;
import org.neo5j.com.ObjectSerializer;
import org.neo5j.com.ProtocolVersion;
import org.neo5j.com.RequestContext;
import org.neo5j.com.Response;
import org.neo5j.com.storecopy.ResponseUnpacker.TxHandler;
import org.neo5j.com.storecopy.StoreWriter;
import org.neo5j.kernel.ha.MasterClient320;
import org.neo5j.kernel.ha.com.master.Master;
import org.neo5j.kernel.ha.lock.LockResult;
import org.neo5j.kernel.impl.transaction.TransactionRepresentation;

public interface MasterClient extends Master
{
    ProtocolVersion CURRENT = MasterClient320.PROTOCOL_VERSION;

    @Override
    Response<Integer> createRelationshipType( RequestContext context, String name );

    @Override
    Response<Void> newLockSession( RequestContext context );

    @Override
    Response<Long> commit( RequestContext context, TransactionRepresentation channel );

    @Override
    Response<Void> pullUpdates( RequestContext context );

    Response<Void> pullUpdates( RequestContext context, TxHandler txHandler );

    @Override
    Response<Void> copyStore( RequestContext context, StoreWriter writer );

    void setComExceptionHandler( ComExceptionHandler handler );

    ProtocolVersion getProtocolVersion();

    ObjectSerializer<LockResult> createLockResultSerializer();

    Deserializer<LockResult> createLockResultDeserializer();
}
