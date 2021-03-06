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
package org.neo5j.bolt.v1.messaging;

import java.util.Map;

import org.neo5j.bolt.v1.runtime.Neo5jError;

/**
 * Interface defining simple handler methods for each defined
 * Bolt request message.
 *
 * @param <E> an exception that may be thrown by each handler method
 */
public interface BoltRequestMessageHandler<E extends Exception>
{
    void onInit( String userAgent, Map<String,Object> authToken ) throws E;

    void onAckFailure() throws E;

    void onReset() throws E;

    void onRun( String statement, Map<String,Object> params ) throws E;

    void onDiscardAll() throws E;

    void onPullAll() throws E;

    void onExternalError( Neo5jError error ) throws E;

}
