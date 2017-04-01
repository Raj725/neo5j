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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.neo5j.kernel.api.index.InternalIndexState;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptor;
import org.neo5j.kernel.impl.api.index.IndexMap;
import org.neo5j.kernel.impl.api.index.IndexMapSnapshotProvider;
import org.neo5j.kernel.impl.api.index.IndexProxy;
import org.neo5j.kernel.impl.util.JobScheduler;
import org.neo5j.kernel.impl.util.JobScheduler.JobHandle;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.neo5j.kernel.impl.api.index.sampling.IndexSamplingMode.BACKGROUND_REBUILD_UPDATED;
import static org.neo5j.kernel.impl.util.JobScheduler.Groups.indexSamplingController;

public class IndexSamplingController
{
    private final IndexSamplingJobFactory jobFactory;
    private final IndexSamplingJobQueue<Long> jobQueue;
    private final IndexSamplingJobTracker jobTracker;
    private final IndexMapSnapshotProvider indexMapSnapshotProvider;
    private final JobScheduler scheduler;
    private final RecoveryCondition indexRecoveryCondition;
    private final boolean backgroundSampling;
    private final Lock samplingLock = new ReentrantLock( true );

    private JobHandle backgroundSamplingHandle;

    // use IndexSamplingControllerFactory.create do not instantiate directly
    IndexSamplingController( IndexSamplingConfig config,
                             IndexSamplingJobFactory jobFactory,
                             IndexSamplingJobQueue<Long> jobQueue,
                             IndexSamplingJobTracker jobTracker,
                             IndexMapSnapshotProvider indexMapSnapshotProvider,
                             JobScheduler scheduler,
                             RecoveryCondition indexRecoveryCondition )
    {
        this.backgroundSampling = config.backgroundSampling();
        this.jobFactory = jobFactory;
        this.indexMapSnapshotProvider = indexMapSnapshotProvider;
        this.jobQueue = jobQueue;
        this.jobTracker = jobTracker;
        this.scheduler = scheduler;
        this.indexRecoveryCondition = indexRecoveryCondition;
    }

    public void sampleIndexes( IndexSamplingMode mode )
    {
        IndexMap indexMap = indexMapSnapshotProvider.indexMapSnapshot();
        jobQueue.addAll( !mode.sampleOnlyIfUpdated, indexMap.indexIds() );
        scheduleSampling( mode, indexMap );
    }

    public void sampleIndex( long indexId, IndexSamplingMode mode )
    {
        IndexMap indexMap = indexMapSnapshotProvider.indexMapSnapshot();
        jobQueue.add( !mode.sampleOnlyIfUpdated, indexId );
        scheduleSampling( mode, indexMap );
    }

    public void recoverIndexSamples()
    {
        samplingLock.lock();
        try
        {
            IndexMap indexMap = indexMapSnapshotProvider.indexMapSnapshot();
            Iterator<Long> indexIds = indexMap.indexIds();
            while ( indexIds.hasNext() )
            {
                long indexId = indexIds.next();
                if ( indexRecoveryCondition.test( indexId, indexMap.getIndexProxy( indexId ).getDescriptor() ) )
                {
                    sampleIndexOnCurrentThread( indexMap, indexId );
                }
            }
        }
        finally
        {
            samplingLock.unlock();
        }
    }

    public interface RecoveryCondition
    {
        boolean test(long indexId, NewIndexDescriptor descriptor);
    }

    private void scheduleSampling( IndexSamplingMode mode, IndexMap indexMap )
    {
        if ( mode.blockUntilAllScheduled )
        {
            // Wait until last sampling job has been started
            scheduleAllSampling( indexMap );
        }
        else
        {
            // Try to schedule as many sampling jobs as possible
            tryScheduleSampling( indexMap );
        }
    }

    private void tryScheduleSampling( IndexMap indexMap )
    {
        if ( tryEmptyLock() )
        {
            try
            {
                while ( jobTracker.canExecuteMoreSamplingJobs() )
                {
                    Long indexId = jobQueue.poll();
                    if ( indexId == null )
                    {
                        return;
                    }

                    sampleIndexOnTracker( indexMap, indexId );
                }
            }
            finally
            {
                samplingLock.unlock();
            }
        }
    }

    private boolean tryEmptyLock()
    {
        try
        {
            return samplingLock.tryLock( 0, SECONDS );
        }
        catch ( InterruptedException ex )
        {
            // ignored
            return false;
        }
    }

    private void scheduleAllSampling( IndexMap indexMap )
    {
        samplingLock.lock();
        try
        {
            Iterable<Long> indexIds = jobQueue.pollAll();

            for ( Long indexId : indexIds )
            {
                jobTracker.waitUntilCanExecuteMoreSamplingJobs();
                sampleIndexOnTracker( indexMap, indexId );
            }
        }
        finally
        {
            samplingLock.unlock();
        }
    }

    private void sampleIndexOnTracker( IndexMap indexMap, long indexId )
    {
        IndexSamplingJob job = createSamplingJob( indexMap, indexId );
        if ( job != null )
        {
            jobTracker.scheduleSamplingJob( job );
        }
    }

    private void sampleIndexOnCurrentThread( IndexMap indexMap, long indexId )
    {
        IndexSamplingJob job = createSamplingJob( indexMap, indexId );
        if ( job != null )
        {
            job.run();
        }
    }

    private IndexSamplingJob createSamplingJob( IndexMap indexMap, long indexId )
    {
        IndexProxy proxy = indexMap.getIndexProxy( indexId );
        if ( proxy == null || proxy.getState() != InternalIndexState.ONLINE )
        {
            return null;
        }
        return jobFactory.create( indexId, proxy );
    }

    public void start()
    {
        if ( backgroundSampling )
        {
            Runnable samplingRunner = () -> sampleIndexes( BACKGROUND_REBUILD_UPDATED );
            backgroundSamplingHandle = scheduler.scheduleRecurring( indexSamplingController, samplingRunner, 10, SECONDS );
        }
    }

    public void awaitSamplingCompleted( long time, TimeUnit unit ) throws InterruptedException
    {
        jobTracker.awaitAllJobs( time, unit );
    }

    public void stop()
    {
        if ( backgroundSamplingHandle != null )
        {
            backgroundSamplingHandle.cancel( true );
        }
        jobTracker.stopAndAwaitAllJobs();
    }
}