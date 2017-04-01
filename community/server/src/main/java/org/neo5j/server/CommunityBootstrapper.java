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
package org.neo5j.server;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nonnull;

import org.neo5j.kernel.GraphDatabaseDependencies;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.configuration.ConfigurationValidator;
import org.neo5j.kernel.configuration.ServerConfigurationValidator;
import org.neo5j.logging.LogProvider;

public class CommunityBootstrapper extends ServerBootstrapper
{

    @Override
    protected NeoServer createNeoServer( Config config, GraphDatabaseDependencies dependencies,
                                         LogProvider logProvider )
    {
        return new CommunityNeoServer( config, dependencies, logProvider );
    }

    @Override
    @Nonnull
    protected Collection<ConfigurationValidator> configurationValidators()
    {
        return Collections.singletonList( new ServerConfigurationValidator() );
    }

}
