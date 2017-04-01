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
package org.neo5j.server.enterprise;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nonnull;

import org.neo5j.causalclustering.core.CausalClusterConfigurationValidator;
import org.neo5j.configuration.HaConfigurationValidator;
import org.neo5j.kernel.GraphDatabaseDependencies;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.configuration.ConfigurationValidator;
import org.neo5j.logging.LogProvider;
import org.neo5j.server.CommunityBootstrapper;
import org.neo5j.server.NeoServer;

public class EnterpriseBootstrapper extends CommunityBootstrapper
{
    @Override
    protected NeoServer createNeoServer( Config configurator, GraphDatabaseDependencies dependencies,
            LogProvider userLogProvider )
    {
        return new EnterpriseNeoServer( configurator, dependencies, userLogProvider );
    }

    @Override
    @Nonnull
    protected Collection<ConfigurationValidator> configurationValidators()
    {
        ArrayList<ConfigurationValidator> validators = new ArrayList<>();
        validators.addAll( super.configurationValidators() );
        validators.add( new HaConfigurationValidator() );
        validators.add( new CausalClusterConfigurationValidator() );
        return validators;
    }
}
