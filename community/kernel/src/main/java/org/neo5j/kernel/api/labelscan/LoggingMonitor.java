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
package org.neo5j.kernel.api.labelscan;

import java.util.Map;

import org.neo5j.kernel.api.labelscan.LabelScanStore.Monitor;
import org.neo5j.logging.Log;

import static java.lang.String.format;

/**
 * Logs about important events about {@link LabelScanStore} {@link Monitor}.
 */
public class LoggingMonitor implements Monitor
{
    private final Log log;
    private final Monitor delegate;

    public LoggingMonitor( Log log )
    {
        this( log, Monitor.EMPTY );
    }

    public LoggingMonitor( Log log, Monitor delegate )
    {
        this.log = log;
        this.delegate = delegate;
    }

    @Override
    public void init()
    {
        delegate.init();
    }

    @Override
    public void noIndex()
    {
        log.info( "No scan store found, this might just be first use. Preparing to rebuild." );
        delegate.noIndex();
    }

    @Override
    public void lockedIndex( Exception e )
    {
        log.error( "Scan store is locked by another process or database", e );
        delegate.lockedIndex( e );
    }

    @Override
    public void notValidIndex()
    {
        log.warn( "Scan store could not be read. Preparing to rebuild." );
        delegate.notValidIndex();
    }

    @Override
    public void rebuilding()
    {
        log.info( "Rebuilding scan store, this may take a while" );
        delegate.rebuilding();
    }

    @Override
    public void rebuilt( long roughNodeCount )
    {
        log.info( "Scan store rebuilt (roughly " + roughNodeCount + " nodes)" );
        delegate.rebuilt( roughNodeCount );
    }

    @Override
    public void recoveryCompleted( Map<String,Object> data )
    {
        StringBuilder builder = new StringBuilder( "Scan store recovery completed:" );
        data.forEach( (key,value) -> builder.append( format( " %s: %s", key, value ) ) );
        log.info( builder.toString() );
        delegate.recoveryCompleted( data );
    }
}
