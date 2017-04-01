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
package org.neo5j.backup;

import java.util.function.Supplier;

import org.neo5j.helpers.Service;
import org.neo5j.io.fs.FileSystemAbstraction;
import org.neo5j.io.pagecache.PageCache;
import org.neo5j.kernel.NeoStoreDataSource;
import org.neo5j.kernel.configuration.Config;
import org.neo5j.kernel.extension.KernelExtensionFactory;
import org.neo5j.kernel.impl.logging.LogService;
import org.neo5j.kernel.impl.spi.KernelContext;
import org.neo5j.kernel.impl.transaction.log.LogFileInformation;
import org.neo5j.kernel.impl.transaction.log.LogicalTransactionStore;
import org.neo5j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo5j.kernel.impl.transaction.log.checkpoint.CheckPointer;
import org.neo5j.kernel.impl.transaction.log.checkpoint.StoreCopyCheckPointMutex;
import org.neo5j.kernel.internal.GraphDatabaseAPI;
import org.neo5j.kernel.lifecycle.Lifecycle;
import org.neo5j.kernel.monitoring.Monitors;

@Service.Implementation(KernelExtensionFactory.class)
public class OnlineBackupExtensionFactory extends KernelExtensionFactory<OnlineBackupExtensionFactory.Dependencies>
{
    static final String KEY = "online backup";

    public interface Dependencies
    {
        Config getConfig();

        GraphDatabaseAPI getGraphDatabaseAPI();

        LogService logService();

        Monitors monitors();

        NeoStoreDataSource neoStoreDataSource();

        Supplier<CheckPointer> checkPointer();

        Supplier<TransactionIdStore> transactionIdStoreSupplier();

        Supplier<LogicalTransactionStore> logicalTransactionStoreSupplier();

        Supplier<LogFileInformation> logFileInformationSupplier();

        FileSystemAbstraction fileSystemAbstraction();

        PageCache pageCache();

        StoreCopyCheckPointMutex storeCopyCheckPointMutex();
    }

    public OnlineBackupExtensionFactory()
    {
        super( KEY );
    }

    @Override
    public Class<OnlineBackupSettings> getSettingsClass()
    {
        return OnlineBackupSettings.class;
    }

    @Override
    public Lifecycle newInstance( KernelContext context, Dependencies dependencies ) throws Throwable
    {
        return new OnlineBackupKernelExtension( dependencies.getConfig(), dependencies.getGraphDatabaseAPI(),
                dependencies.logService().getInternalLogProvider(), dependencies.monitors(),
                dependencies.neoStoreDataSource(),
                dependencies.checkPointer(),
                dependencies.transactionIdStoreSupplier(),
                dependencies.logicalTransactionStoreSupplier(),
                dependencies.logFileInformationSupplier(),
                dependencies.fileSystemAbstraction(),
                dependencies.pageCache(),
                dependencies.storeCopyCheckPointMutex() );
    }
}
