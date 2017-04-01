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
package org.neo5j.storageengine.api;

import java.io.File;
import java.util.Optional;

import org.neo5j.kernel.impl.store.StoreType;

public class StoreFileMetadata
{
    private final File file;
    private final Optional<StoreType> storeType;
    private final int recordSize;

    public StoreFileMetadata( File file, Optional<StoreType> storeType, int recordSize )
    {
        this.file = file;
        this.storeType = storeType;
        this.recordSize = recordSize;
    }

    public File file()
    {
        return file;
    }

    public Optional<StoreType> storeType()
    {
        return storeType;
    }

    public int recordSize()
    {
        return recordSize;
    }
}
