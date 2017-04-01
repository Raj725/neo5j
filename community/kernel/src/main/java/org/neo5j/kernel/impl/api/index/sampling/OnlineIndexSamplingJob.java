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
package org.neo5j.kernel.impl.api.index.sampling;

import org.neo5j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo5j.kernel.impl.api.index.IndexProxy;
import org.neo5j.kernel.impl.api.index.IndexStoreView;
import org.neo5j.kernel.impl.util.DurationLogger;
import org.neo5j.logging.Log;
import org.neo5j.logging.LogProvider;
import org.neo5j.storageengine.api.schema.IndexReader;
import org.neo5j.storageengine.api.schema.IndexSample;
import org.neo5j.storageengine.api.schema.IndexSampler;

import static java.lang.String.format;
import static org.neo5j.kernel.api.index.InternalIndexState.ONLINE;

class OnlineIndexSamplingJob implements IndexSamplingJob
{
    private final long indexId;
    private final IndexProxy indexProxy;
    private final IndexStoreView storeView;
    private final Log log;
    private final String indexUserDescription;

    OnlineIndexSamplingJob( long indexId, IndexProxy indexProxy, IndexStoreView storeView, String indexUserDescription,
            LogProvider logProvider )
    {
        this.indexId = indexId;
        this.indexProxy = indexProxy;
        this.storeView = storeView;
        this.log = logProvider.getLog( getClass() );
        this.indexUserDescription = indexUserDescription;
    }

    @Override
    public long indexId()
    {
        return indexId;
    }

    @Override
    public void run()
    {
        try ( DurationLogger durationLogger = new DurationLogger( log, "Sampling index " + indexUserDescription ) )
        {
            try
            {
                try ( IndexReader reader = indexProxy.newReader() )
                {
                    IndexSampler sampler = reader.createSampler();
                    IndexSample sample = sampler.sampleIndex();

                    // check again if the index is online before saving the counts in the store
                    if ( indexProxy.getState() == ONLINE )
                    {
                        storeView.replaceIndexCounts( indexId, sample.uniqueValues(), sample.sampleSize(),
                                sample.indexSize() );
                        durationLogger.markAsFinished();
                        log.debug(
                                format( "Sampled index %s with %d unique values in sample of avg size %d taken from " +
                                        "index containing %d entries",
                                        indexUserDescription, sample.uniqueValues(), sample.sampleSize(),
                                        sample.indexSize() ) );
                    }
                    else
                    {
                        durationLogger.markAsAborted( "Index no longer ONLINE" );
                    }
                }
            }
            catch ( IndexNotFoundKernelException e )
            {
                durationLogger.markAsAborted(
                        "Attempted to sample missing/already deleted index " + indexUserDescription );
            }
        }
    }

}
