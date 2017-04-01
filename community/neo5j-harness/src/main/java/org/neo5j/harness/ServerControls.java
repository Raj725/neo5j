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
package org.neo5j.harness;

import java.net.URI;
import java.util.Optional;

import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.graphdb.config.Configuration;

/**
 * Control panel for a Neo5j test instance.
 */
public interface ServerControls extends AutoCloseable
{
    /** Returns the URI to the Bolt Protocol connector of the instance. */
    URI boltURI();

    /** Returns the URI to the root resource of the instance. For example, http://localhost:7474/ */
    URI httpURI();

    /**
     * Returns ths URI to the root resource of the instance using the https protocol.
     * For example, https://localhost:7475/.
     */
    Optional<URI> httpsURI();

    /** Stop the test instance and delete all files related to it on disk. */
    @Override
    void close();

    /** Access the {@link org.neo5j.graphdb.GraphDatabaseService} used by the server */
    GraphDatabaseService graph();

    /** Returns the server's configuration */
    Configuration config();
}
