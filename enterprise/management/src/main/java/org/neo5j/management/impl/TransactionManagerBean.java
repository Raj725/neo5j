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
import org.neo5j.kernel.impl.transaction.TransactionStats;
import org.neo5j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo5j.kernel.impl.transaction.state.DataSourceManager;
import org.neo5j.management.TransactionManager;

@Service.Implementation(ManagementBeanProvider.class)
public final class TransactionManagerBean extends ManagementBeanProvider
{
    public TransactionManagerBean()
    {
        super( TransactionManager.class );
    }

    @Override
    protected Neo5jMBean createMBean( ManagementData management ) throws NotCompliantMBeanException
    {
        return new TransactionManagerImpl( management );
    }

    private static class TransactionManagerImpl extends Neo5jMBean implements TransactionManager
    {
        private final TransactionStats txMonitor;
        private final DataSourceManager xadsm;

        TransactionManagerImpl( ManagementData management ) throws NotCompliantMBeanException
        {
            super( management );
            this.txMonitor = management.resolveDependency( TransactionStats.class );
            this.xadsm = management.resolveDependency( DataSourceManager.class );
        }

        @Override
        public long getNumberOfOpenTransactions()
        {
            return txMonitor.getNumberOfActiveTransactions();
        }

        @Override
        public long getPeakNumberOfConcurrentTransactions()
        {
            return txMonitor.getPeakConcurrentNumberOfTransactions();
        }

        @Override
        public long getNumberOfOpenedTransactions()
        {
            return txMonitor.getNumberOfStartedTransactions();
        }

        @Override
        public long getNumberOfCommittedTransactions()
        {
            return txMonitor.getNumberOfCommittedTransactions();
        }

        @Override
        public long getNumberOfRolledBackTransactions()
        {
            return txMonitor.getNumberOfRolledBackTransactions();
        }

        @Override
        public long getLastCommittedTxId()
        {
            NeoStoreDataSource neoStoreDataSource = xadsm.getDataSource();
            if ( neoStoreDataSource == null )
            {
                return -1;
            }
            return neoStoreDataSource.getDependencyResolver().resolveDependency( TransactionIdStore.class )
                    .getLastCommittedTransactionId();
        }
    }
}
