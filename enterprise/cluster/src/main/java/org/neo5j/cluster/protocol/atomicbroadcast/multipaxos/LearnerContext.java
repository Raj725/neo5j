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
package org.neo5j.cluster.protocol.atomicbroadcast.multipaxos;

import org.neo5j.cluster.protocol.ConfigurationContext;
import org.neo5j.cluster.protocol.LoggingContext;
import org.neo5j.cluster.protocol.TimeoutsContext;
import org.neo5j.cluster.protocol.atomicbroadcast.AtomicBroadcastSerializer;

/**
 * Context for the Learner Paxos state machine.
 */
public interface LearnerContext
    extends TimeoutsContext, LoggingContext, ConfigurationContext
{
    long getLastDeliveredInstanceId();

    void setLastDeliveredInstanceId( long lastDeliveredInstanceId );

    long getLastLearnedInstanceId();

    long getLastKnownLearnedInstanceInCluster();

    void learnedInstanceId( long instanceId );

    boolean hasDeliveredAllKnownInstances();

    void leave();

    PaxosInstance getPaxosInstance( org.neo5j.cluster.protocol.atomicbroadcast.multipaxos.InstanceId instanceId );

    AtomicBroadcastSerializer newSerializer();

    Iterable<org.neo5j.cluster.InstanceId> getAlive();

    void setNextInstanceId( long id );

    void notifyLearnMiss( org.neo5j.cluster.protocol.atomicbroadcast.multipaxos.InstanceId instanceId );

    org.neo5j.cluster.InstanceId getLastKnownAliveUpToDateInstance();

    void setLastKnownLearnedInstanceInCluster( long lastKnownLearnedInstanceInCluster, org.neo5j.cluster.InstanceId instanceId );
}
