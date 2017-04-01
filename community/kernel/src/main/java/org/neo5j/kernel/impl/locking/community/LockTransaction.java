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
package org.neo5j.kernel.impl.locking.community;

import java.util.concurrent.atomic.AtomicInteger;

import org.neo5j.kernel.impl.locking.Locks;

/**
 * A transaction used for the sole purpose of acquiring locks via the community lock manager. This exists solely to
 * allow using the community lock manager with the {@link Locks} API.
 */
public class LockTransaction
{
    private static final AtomicInteger IDS = new AtomicInteger( 0 );

    private final int id = IDS.getAndIncrement();

    public int getId()
    {
        return id;
    }

    @Override
    public String toString()
    {
        return String.format( "LockClient[%d]", id );
    }
}