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
package org.neo5j.kernel.api.impl.schema;

import org.neo5j.helpers.Service;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.kernel.api.impl.index.storage.DirectoryFactory;
import org.neo5j.kernel.api.index.SchemaIndexProvider;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.extension.KernelExtensionFactory;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacadeFactory;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.spi.KernelContext;
import org.neo5j.logging.LogProvider;

import static org.neo5j.kernel.api.impl.index.LuceneKernelExtensions.directoryFactory;

@Service.Implementation(KernelExtensionFactory.class)
public class LuceneSchemaIndexProviderFactory extends
        KernelExtensionFactory<LuceneSchemaIndexProviderFactory.Dependencies>
{
    public static final String KEY = "lucene";

    public static final SchemaIndexProvider.Descriptor PROVIDER_DESCRIPTOR =
            new SchemaIndexProvider.Descriptor( KEY, "1.0" );

    public interface Dependencies
    {
        Config getConfig();

        LogService getLogging();

        FileSystemAbstraction fileSystem();
    }

    public LuceneSchemaIndexProviderFactory()
    {
        super( KEY );
    }

    @Override
    public LuceneSchemaIndexProvider newInstance( KernelContext context, Dependencies dependencies ) throws Throwable
    {
        Config config = dependencies.getConfig();
        LogProvider logging = dependencies.getLogging().getInternalLogProvider();
        boolean ephemeral = config.get( GraphDatabaseFacadeFactory.Configuration.ephemeral );

        FileSystemAbstraction fileSystem = dependencies.fileSystem();
        DirectoryFactory directoryFactory = directoryFactory( ephemeral, fileSystem );

        return new LuceneSchemaIndexProvider( fileSystem, directoryFactory, context.storeDir(), logging, config,
                context.databaseInfo().operationalMode );
    }
}
