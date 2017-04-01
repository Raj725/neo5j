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
package org.neo5j.kernel.impl.index;

import org.neo5j.graphdb.Node;
import org.neo5j.graphdb.PropertyContainer;
import org.neo5j.graphdb.Relationship;

public enum IndexEntityType
{
    Node( (byte) 0 )
    {
        @Override
        public Class<? extends PropertyContainer> entityClass()
        {
            return Node.class;
        }
    },
    Relationship( (byte) 1 )
    {
        @Override
        public Class<? extends PropertyContainer> entityClass()
        {
            return Relationship.class;
        }
    };

    private byte id;

    IndexEntityType( byte id )
    {
        this.id = id;
    }

    public byte id()
    {
        return id;
    }

    public abstract Class<? extends PropertyContainer> entityClass();

    public static IndexEntityType byId( byte id )
    {
        for ( IndexEntityType type : values() )
        {
            if ( type.id() == id )
            {
                return type;
            }
        }
        throw new IllegalArgumentException( "Unknown id " + id );
    }

    public String nameToLowerCase()
    {
        return this.name().toLowerCase();
    }
}