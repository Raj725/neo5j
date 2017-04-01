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
package org.neo5j.jmx.impl;

import org.neo5j.helpers.Service;
import org.neo5j.kernel.internal.KernelData;
import org.neo5j.kernel.extension.KernelExtensionFactory;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.spi.KernelContext;
import org.neo5j.kernel.lifecycle.Lifecycle;

@Service.Implementation(KernelExtensionFactory.class)
public final class JmxExtensionFactory extends KernelExtensionFactory<JmxExtensionFactory.Dependencies>
{
    public interface Dependencies
    {
        KernelData getKernelData();

        LogService getLogService();
    }

    public static final String KEY = "kernel jmx";

    public JmxExtensionFactory()
    {
        super( KEY );
    }

    @Override
    public Lifecycle newInstance( KernelContext context, Dependencies dependencies ) throws Throwable
    {
        return new JmxKernelExtension(
                dependencies.getKernelData(), dependencies.getLogService().getInternalLogProvider() );
    }
}
