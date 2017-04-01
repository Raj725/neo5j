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
package org.neo5j.causalclustering.core.consensus.log.segmented;

import java.io.File;

import org.neo5j.causalclustering.core.consensus.log.ConcurrentStressIT;
import org.neo5j.causalclustering.core.consensus.log.DummyRaftableContentSerializer;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.logging.LogProvider;
import org.neo5j.test.OnDemandJobScheduler;
import org.neo5j.time.Clocks;

import static org.neo5j.causalclustering.core.CausalClusteringSettings.raft_log_pruning_strategy;
import static org.neo5j.logging.NullLogProvider.getInstance;

public class SegmentedConcurrentStressIT extends ConcurrentStressIT<SegmentedRaftLog>
{
    @Override
    public SegmentedRaftLog createRaftLog( FileSystemAbstraction fsa, File dir ) throws Throwable
    {
        long rotateAtSize = 8 * 1024 * 1024;
        LogProvider logProvider = getInstance();
        int readerPoolSize = 8;
        CoreLogPruningStrategy pruningStrategy =
                new CoreLogPruningStrategyFactory( raft_log_pruning_strategy.getDefaultValue(), logProvider )
                        .newInstance();
        SegmentedRaftLog raftLog =
                new SegmentedRaftLog( fsa, dir, rotateAtSize, new DummyRaftableContentSerializer(), logProvider,
                        readerPoolSize, Clocks.fakeClock(), new OnDemandJobScheduler(), pruningStrategy );
        raftLog.start();
        return raftLog;
    }
}
