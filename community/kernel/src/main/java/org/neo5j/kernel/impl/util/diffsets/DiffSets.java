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
package org.neo5j.kernel.impl.util.diffsets;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import org.neo5j.collection.primitive.PrimitiveIntIterator;
import org.neo5j.collection.primitive.PrimitiveLongIterator;
import org.neo5j.helpers.collection.Iterables;
import org.neo5j.kernel.impl.util.DiffApplyingPrimitiveIntIterator;
import org.neo5j.kernel.impl.util.DiffApplyingPrimitiveLongIterator;
import org.neo5j.storageengine.api.txstate.ReadableDiffSets;

/**
 * Given a sequence of add and removal operations, instances of DiffSets track
 * which elements need to actually be added and removed at minimum from some
 * hypothetical target collection such that the result is equivalent to just
 * executing the sequence of additions and removals in order
 *
 * @param <T> type of elements
 */
public class DiffSets<T> extends SuperDiffSets<T,PrimitiveLongIterator> implements ReadableDiffSets<T>
{
    public DiffSets()
    {
        this( null, null );
    }

    public DiffSets( Set<T> addedElements, Set<T> removedElements )
    {
        super( addedElements, removedElements );
    }

    @Override
    public PrimitiveLongIterator augment( final PrimitiveLongIterator source )
    {
        return new DiffApplyingPrimitiveLongIterator( source, added( false ), removed( false ) );
    }

    @Override
    public PrimitiveIntIterator augment( final PrimitiveIntIterator source )
    {
        return new DiffApplyingPrimitiveIntIterator( source, added( false ), removed( false ) );
    }

    @Override
    public PrimitiveLongIterator augmentWithRemovals( final PrimitiveLongIterator source )
    {
        return new DiffApplyingPrimitiveLongIterator( source, Collections.emptySet(), removed( false ) );
    }

    @Override
    public DiffSets<T> filterAdded( Predicate<T> addedFilter )
    {
        return new DiffSets<>(
                Iterables.asSet( Iterables.filter( addedFilter, added( false ) ) ),
                Iterables.asSet( removed( false ) ) );
    }
}
