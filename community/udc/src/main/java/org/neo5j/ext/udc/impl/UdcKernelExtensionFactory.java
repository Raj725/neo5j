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
package org.neo5j.ext.udc.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.neo5j.ext.udc.UdcSettings;
import org.neo5j.helpers.Service;
import org.neo5j.helpers.collection.MapUtil;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.extension.KernelExtensionFactory;
import org.neo5j.kernel.impl.core.StartupStatistics;
import org.neo5j.kernel.impl.spi.KernelContext;
import org.neo5j.kernel.impl.store.id.IdGeneratorFactory;
import org.neo5j.kernel.impl.transaction.state.DataSourceManager;
import org.neo5j.kernel.lifecycle.Lifecycle;
import org.neo5j.udc.UsageData;

/**
 * Kernel extension for UDC, the Usage Data Collector. The UDC runs as a background
 * daemon, waking up once a day to collect basic usage information about a long
 * running graph database.
 * <p>
 * The first update is delayed to avoid needless activity during integration
 * testing and short-run applications. Subsequent updates are made at regular
 * intervals. Both times are specified in milliseconds.
 */
@Service.Implementation(KernelExtensionFactory.class)
public class UdcKernelExtensionFactory extends KernelExtensionFactory<UdcKernelExtensionFactory.Dependencies>
{
    static final String KEY = "kernel udc";

    public interface Dependencies
    {
        Config config();
        DataSourceManager dataSourceManager();
        UsageData usageData();
        IdGeneratorFactory idGeneratorFactory();
        StartupStatistics startupStats();
    }

    public UdcKernelExtensionFactory()
    {
        super( KEY );
    }

    @Override
    public Class<UdcSettings> getSettingsClass()
    {
        return UdcSettings.class;
    }

    @Override
    public Lifecycle newInstance( KernelContext kernelContext, UdcKernelExtensionFactory.Dependencies dependencies )
            throws Throwable
    {
        return new UdcKernelExtension(
                dependencies.config().with( loadUdcProperties() ),
                dependencies.dataSourceManager(),
                dependencies.idGeneratorFactory(),
                dependencies.startupStats(),
                dependencies.usageData(),
                new Timer( "Neo5j UDC Timer", isAlwaysDaemon() ) );
    }

    private boolean isAlwaysDaemon()
    {
        return true;
    }

    private Map<String, String> loadUdcProperties()
    {
        try
        {
            return MapUtil.load( getClass().getResourceAsStream( "/org/neo5j/ext/udc/udc.properties" ) );
        }
        catch ( Exception e )
        {
            return new HashMap<>();
        }
    }
}
