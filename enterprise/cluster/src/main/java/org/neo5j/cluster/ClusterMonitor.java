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
package org.neo5j.cluster;

import org.neo5j.cluster.com.BindingNotifier;
import org.neo5j.cluster.protocol.cluster.ClusterListener;
import org.neo5j.cluster.protocol.heartbeat.Heartbeat;

/**
 * Bundles up different ways of listening in on events going on in a cluster.
 *
 * {@link BindingNotifier} for notifications about which URI is used for sending
 * events of the network. {@link Heartbeat} for notifications about failed/alive
 * members. {@link #addClusterListener(ClusterListener)},
 * {@link #removeClusterListener(ClusterListener)} for getting notified about
 * cluster membership events.
 *
 * @author Mattias Persson
 */
public interface ClusterMonitor extends BindingNotifier, Heartbeat
{
    void addClusterListener( ClusterListener listener);

    void removeClusterListener( ClusterListener listener);
}