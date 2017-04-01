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
package org.neo5j.causalclustering.readreplica;


import java.util.LinkedHashSet;
import java.util.NoSuchElementException;

import org.neo5j.causalclustering.identity.MemberId;
import org.neo5j.logging.Log;
import org.neo5j.logging.LogProvider;
import org.neo5j.logging.NullLogProvider;

import static org.neo5j.helpers.collection.Iterables.empty;

public class UpstreamDatabaseStrategySelector
{
    private LinkedHashSet<UpstreamDatabaseSelectionStrategy> strategies = new LinkedHashSet<>();
    private MemberId myself;
    private Log log;

    UpstreamDatabaseStrategySelector( UpstreamDatabaseSelectionStrategy defaultStrategy )
    {
        this( defaultStrategy, empty(), null, NullLogProvider.getInstance() );
    }

    UpstreamDatabaseStrategySelector( UpstreamDatabaseSelectionStrategy defaultStrategy,
                                      Iterable<UpstreamDatabaseSelectionStrategy> otherStrategies, MemberId myself,
                                      LogProvider logProvider )
    {
        this.myself = myself;
        this.log = logProvider.getLog( getClass() );

        if ( otherStrategies != null )
        {
            for ( UpstreamDatabaseSelectionStrategy otherStrategy : otherStrategies )
            {
                strategies.add( otherStrategy );
            }
        }
        strategies.add( defaultStrategy );
    }

    public MemberId bestUpstreamDatabase() throws UpstreamDatabaseSelectionException
    {
        MemberId result = null;
        for ( UpstreamDatabaseSelectionStrategy strategy : strategies )
        {
            log.debug( "Trying selection strategy [%s]", strategy.toString() );
            try
            {
                if ( strategy.upstreamDatabase().isPresent() )
                {
                    result = strategy.upstreamDatabase().get();
                    break;
                }
            }
            catch ( NoSuchElementException ex )
            {
                // Do nothing, this strategy failed
            }
        }

        if ( result == null )
        {
            throw new UpstreamDatabaseSelectionException(
                    "Could not find an upstream database with which to connect." );
        }

        log.debug( "Selected upstream database [%s]", result );
        return result;
    }
}
