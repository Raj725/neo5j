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
package org.neo5j.kernel.impl.transaction.state.storeview;

import java.util.Collection;
import java.util.function.IntPredicate;

import org.neo5j.collection.primitive.PrimitiveLongResourceIterator;
import org.neo5j.helpers.collection.Visitor;
import org.neo5j.kernel.api.index.IndexEntryUpdate;
import org.neo5j.kernel.impl.api.index.NodeUpdates;
import org.neo5j.kernel.api.labelscan.LabelScanStore;
import org.neo5j.kernel.api.labelscan.NodeLabelUpdate;
import org.neo5j.kernel.impl.api.index.MultipleIndexPopulator;
import org.neo5j.kernel.impl.locking.LockService;
import org.neo5j.kernel.impl.store.NodeStore;
import org.neo5j.kernel.impl.store.PropertyStore;

/**
 * Store scan view that will try to minimize amount of scanned nodes by using label scan store {@link LabelScanStore}
 * as a source of known labeled node ids.
 * @param <FAILURE>
 */
public class LabelScanViewNodeStoreScan<FAILURE extends Exception> extends StoreViewNodeStoreScan<FAILURE>
{
    private final LabelScanStore labelScanStore;
    // flag that indicated presence of concurrent updates that are not visible to current label index scan
    private boolean outdated;

    public LabelScanViewNodeStoreScan( NodeStore nodeStore, LockService locks,
            PropertyStore propertyStore,
            LabelScanStore labelScanStore, Visitor<NodeLabelUpdate,FAILURE> labelUpdateVisitor,
            Visitor<NodeUpdates,FAILURE> propertyUpdatesVisitor, int[] labelIds,
            IntPredicate propertyKeyIdFilter )
    {
        super( nodeStore, locks, propertyStore, labelUpdateVisitor, propertyUpdatesVisitor, labelIds,
                propertyKeyIdFilter );
        this.labelScanStore = labelScanStore;
    }

    @Override
    public PrimitiveLongResourceIterator getNodeIdIterator()
    {
        return new LabelScanViewIdIterator( this, labelScanStore, labelIds );
    }

    @Override
    public void configure( Collection<MultipleIndexPopulator.IndexPopulation> populations )
    {
        populations.forEach( population -> population.populator.configureSampling( false ) );
    }

    @Override
    public void acceptUpdate( MultipleIndexPopulator.MultipleIndexUpdater updater, IndexEntryUpdate update,
            long currentlyIndexedNodeId )
    {
        super.acceptUpdate( updater, update, currentlyIndexedNodeId );
        if ( update.getEntityId() > currentlyIndexedNodeId )
        {
            markOutdated();
        }
    }

    boolean isOutdated()
    {
        return outdated;
    }

    void clearOutdatedFlag()
    {
        outdated = false;
    }

    private void markOutdated()
    {
        outdated = true;
    }

}
