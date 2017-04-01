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
package org.neo5j.kernel.api.security;

import org.neo5j.helpers.Service;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.kernel.api.exceptions.KernelException;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.proc.Procedures;
import org.neo5j.kernel.impl.util.DependencySatisfier;
import org.neo5j.kernel.impl.util.JobScheduler;
import org.neo5j.kernel.lifecycle.LifeSupport;

public abstract class SecurityModule extends Service
{
    public SecurityModule( String key, String... altKeys )
    {
        super( key, altKeys );
    }

    public abstract void setup( Dependencies dependencies ) throws KernelException;

    public interface Dependencies
    {
        LogService logService();

        Config config();

        Procedures procedures();

        JobScheduler scheduler();

        FileSystemAbstraction fileSystem();

        LifeSupport lifeSupport();

        DependencySatisfier dependencySatisfier();
    }
}
