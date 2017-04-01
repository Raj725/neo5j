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
package org.neo5j.kernel.api.index;

import org.neo5j.collection.primitive.PrimitiveLongIterator;
import org.neo5j.kernel.api.exceptions.index.IndexNotApplicableKernelException;
import org.neo5j.kernel.api.schema_new.IndexQuery;
import org.neo5j.storageengine.api.schema.IndexReader;
import org.neo5j.storageengine.api.schema.IndexSampler;

public class DelegatingIndexReader implements IndexReader
{
    private final IndexReader delegate;

    public DelegatingIndexReader( IndexReader delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public long countIndexedNodes( long nodeId, Object... propertyValues )
    {
        return delegate.countIndexedNodes( nodeId, propertyValues );
    }

    @Override
    public IndexSampler createSampler()
    {
        return delegate.createSampler();
    }

    @Override
    public PrimitiveLongIterator query( IndexQuery... predicates ) throws IndexNotApplicableKernelException
    {
        return delegate.query( predicates );
    }

    @Override
    public void close()
    {
        delegate.close();
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }
}
