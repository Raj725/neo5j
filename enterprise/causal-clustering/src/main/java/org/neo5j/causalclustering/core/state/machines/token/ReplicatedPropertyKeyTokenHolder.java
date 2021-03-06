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
package org.neo5j.causalclustering.core.state.machines.token;

import org.neo5j.causalclustering.core.replication.RaftReplicator;
import org.neo5j.kernel.api.txstate.TransactionState;
import org.neo5j.kernel.impl.core.PropertyKeyTokenHolder;
import org.neo5j.kernel.impl.store.id.IdGeneratorFactory;
import org.neo5j.kernel.impl.store.id.IdType;
import org.neo5j.kernel.impl.util.Dependencies;
import org.neo5j.storageengine.api.Token;

public class ReplicatedPropertyKeyTokenHolder extends ReplicatedTokenHolder<Token> implements
        PropertyKeyTokenHolder
{
    public ReplicatedPropertyKeyTokenHolder( TokenRegistry<Token> registry, RaftReplicator replicator,
            IdGeneratorFactory idGeneratorFactory, Dependencies dependencies )
    {
        super( registry, replicator, idGeneratorFactory, IdType.PROPERTY_KEY_TOKEN, dependencies, TokenType.PROPERTY );
    }

    @Override
    protected void createToken( TransactionState txState, String tokenName, int tokenId )
    {
        txState.propertyKeyDoCreateForName( tokenName, tokenId );
    }
}
