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
package org.neo5j.bolt.v1.runtime;

import java.util.Map;

import org.neo5j.bolt.security.auth.Authentication;
import org.neo5j.bolt.security.auth.AuthenticationException;
import org.neo5j.bolt.security.auth.AuthenticationResult;
import org.neo5j.kernel.api.bolt.BoltConnectionTracker;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.internal.Version;
import org.neo5j.udc.UsageData;
import org.neo5j.udc.UsageDataKeys;

class BoltStateMachineSPI implements BoltStateMachine.SPI
{
    private final BoltConnectionDescriptor connectionDescriptor;
    private final UsageData usageData;
    private final ErrorReporter errorReporter;
    private final BoltConnectionTracker connectionTracker;
    private final Authentication authentication;
    private final String version;
    private final TransactionStateMachine.SPI transactionSpi;

    BoltStateMachineSPI( BoltConnectionDescriptor connectionDescriptor,
                         UsageData usageData,
                         LogService logging,
                         Authentication authentication,
                         BoltConnectionTracker connectionTracker,
                         TransactionStateMachine.SPI transactionStateMachineSPI )
    {
        this.connectionDescriptor = connectionDescriptor;
        this.usageData = usageData;
        this.errorReporter = new ErrorReporter( logging );
        this.connectionTracker = connectionTracker;
        this.authentication = authentication;
        this.transactionSpi = transactionStateMachineSPI;
        this.version = "Neo5j/" + Version.getNeo5jVersion();
    }

    @Override
    public BoltConnectionDescriptor connectionDescriptor()
    {
        return connectionDescriptor;
    }

    @Override
    public void register( BoltStateMachine machine, String owner )
    {
        connectionTracker.onRegister( machine, owner );
    }

    @Override
    public TransactionStateMachine.SPI transactionSpi()
    {
        return transactionSpi;
    }

    @Override
    public void onTerminate( BoltStateMachine machine )
    {
        connectionTracker.onTerminate( machine );
    }

    @Override
    public void reportError( Neo5jError err )
    {
        errorReporter.report( err );
    }

    @Override
    public AuthenticationResult authenticate( Map<String,Object> authToken ) throws AuthenticationException
    {
        return authentication.authenticate( authToken );
    }

    @Override
    public void udcRegisterClient( String clientName )
    {
        usageData.get( UsageDataKeys.clientNames ).add( clientName );
    }

    @Override
    public String version()
    {
        return version;
    }
}
