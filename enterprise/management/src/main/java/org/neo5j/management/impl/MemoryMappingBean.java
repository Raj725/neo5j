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
package org.neo5j.management.impl;

import javax.management.NotCompliantMBeanException;

import org.neo5j.helpers.Service;
import org.neo5j.jmx.impl.ManagementBeanProvider;
import org.neo5j.jmx.impl.ManagementData;
import org.neo5j.jmx.impl.Neo5jMBean;
import org.neo5j.kernel.NeoStoreDataSource;
import org.neo5j.kernel.impl.transaction.state.DataSourceManager;
import org.neo5j.management.MemoryMapping;
import org.neo5j.management.WindowPoolInfo;

@Service.Implementation( ManagementBeanProvider.class )
public final class MemoryMappingBean extends ManagementBeanProvider
{
    public MemoryMappingBean()
    {
        super( MemoryMapping.class );
    }

    @Override
    protected Neo5jMBean createMBean( ManagementData management ) throws NotCompliantMBeanException
    {
        return new MemoryMappingImpl( management );
    }

    @Override
    protected Neo5jMBean createMXBean( ManagementData management ) throws NotCompliantMBeanException
    {
        return new MemoryMappingImpl( management, true );
    }

    private static class MemoryMappingImpl extends Neo5jMBean implements MemoryMapping
    {
        private final NeoStoreDataSource datasource;

        MemoryMappingImpl( ManagementData management ) throws NotCompliantMBeanException
        {
            super( management );
            this.datasource = neoDataSource( management );
        }

        private NeoStoreDataSource neoDataSource( ManagementData management )
        {
            return management.resolveDependency( DataSourceManager.class ).getDataSource();
        }

        MemoryMappingImpl( ManagementData management, boolean isMxBean )
        {
            super( management, isMxBean );
            this.datasource = neoDataSource( management );
        }

        @Override
        public WindowPoolInfo[] getMemoryPools()
        {
            return getMemoryPoolsImpl( datasource );
        }

        public static WindowPoolInfo[] getMemoryPoolsImpl( NeoStoreDataSource datasource )
        {
            return new WindowPoolInfo[0];
        }
    }
}
