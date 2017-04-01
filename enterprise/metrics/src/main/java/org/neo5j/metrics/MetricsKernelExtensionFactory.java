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
package org.neo5j.metrics;

import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.extension.KernelExtensionFactory;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.spi.KernelContext;
import org.neo5j.kernel.lifecycle.Lifecycle;
import org.neo5j.metrics.source.Neo5jMetricsBuilder;

public class MetricsKernelExtensionFactory extends KernelExtensionFactory<MetricsKernelExtensionFactory.Dependencies>
{
    public interface Dependencies extends Neo5jMetricsBuilder.Dependencies
    {
        Config configuration();

        LogService logService();
    }

    public MetricsKernelExtensionFactory()
    {
        super( "metrics" );
    }

    @Override
    public Lifecycle newInstance( KernelContext context, Dependencies dependencies ) throws Throwable
    {
        return new MetricsExtension( context, dependencies );
    }

    @Override
    public Class<MetricsSettings> getSettingsClass()
    {
        return MetricsSettings.class;
    }
}
