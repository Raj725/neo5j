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
package org.neo5j.kernel.api;

import java.util.function.Supplier;

import org.neo5j.collection.pool.Pool;
import org.neo5j.io.pagecache.tracing.cursor.PageCursorTracerSupplier;
import org.neo5j.kernel.api.security.SecurityContext;
import org.neo5j.kernel.impl.api.KernelTransactionImplementation;
import org.neo5j.kernel.impl.api.SchemaWriteGuard;
import org.neo5j.kernel.impl.api.StatementOperationContainer;
import org.neo5j.kernel.impl.api.TransactionHeaderInformation;
import org.neo5j.kernel.impl.api.TransactionHooks;
import org.neo5j.kernel.impl.api.TransactionRepresentationCommitProcess;
import org.neo5j.kernel.impl.api.state.ConstraintIndexCreator;
import org.neo5j.kernel.impl.factory.CanWrite;
import org.neo5j.kernel.impl.locking.LockTracer;
import org.neo5j.kernel.impl.locking.NoOpClient;
import org.neo5j.kernel.impl.locking.SimpleStatementLocks;
import org.neo5j.kernel.impl.locking.StatementLocks;
import org.neo5j.kernel.impl.proc.Procedures;
import org.neo5j.kernel.impl.transaction.TransactionHeaderInformationFactory;
import org.neo5j.kernel.impl.transaction.TransactionMonitor;
import org.neo5j.storageengine.api.StorageEngine;
import org.neo5j.storageengine.api.StorageStatement;
import org.neo5j.storageengine.api.StoreReadLayer;
import org.neo5j.time.Clocks;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.neo5j.kernel.impl.transaction.tracing.TransactionTracer.NULL;

public class KernelTransactionFactory
{
    public static class Instances
    {
        public KernelTransactionImplementation transaction;
        public StorageEngine storageEngine;
        public StoreReadLayer storeReadLayer;
        public StorageStatement storageStatement;

        public Instances( KernelTransactionImplementation transaction, StorageEngine storageEngine,
                StoreReadLayer storeReadLayer, StorageStatement storageStatement )
        {
            this.transaction = transaction;
            this.storageEngine = storageEngine;
            this.storeReadLayer = storeReadLayer;
            this.storageStatement = storageStatement;
        }
    }

    static Instances kernelTransactionWithInternals( SecurityContext securityContext )
    {
        TransactionHeaderInformation headerInformation = new TransactionHeaderInformation( -1, -1, new byte[0] );
        TransactionHeaderInformationFactory headerInformationFactory = mock( TransactionHeaderInformationFactory.class );
        when( headerInformationFactory.create() ).thenReturn( headerInformation );

        StorageEngine storageEngine = mock( StorageEngine.class );
        StoreReadLayer storeReadLayer = mock( StoreReadLayer.class );
        StorageStatement storageStatement = mock( StorageStatement.class );
        when( storeReadLayer.newStatement() ).thenReturn( storageStatement );
        when( storageEngine.storeReadLayer() ).thenReturn( storeReadLayer );

        KernelTransactionImplementation transaction = new KernelTransactionImplementation(
                mock( StatementOperationContainer.class ),
                mock( SchemaWriteGuard.class ),
                new TransactionHooks(),
                mock( ConstraintIndexCreator.class ), new Procedures(), headerInformationFactory,
                mock( TransactionRepresentationCommitProcess.class ), mock( TransactionMonitor.class ),
                mock( Supplier.class ),
                mock( Pool.class ),
                Clocks.systemClock(),
                NULL,
                LockTracer.NONE,
                PageCursorTracerSupplier.NULL,
                storageEngine, new CanWrite() );

        StatementLocks statementLocks = new SimpleStatementLocks( new NoOpClient() );

        transaction.initialize( 0, 0, statementLocks, KernelTransaction.Type.implicit, securityContext, 0L );

        return new Instances( transaction, storageEngine, storeReadLayer, storageStatement );
    }

    static KernelTransaction kernelTransaction( SecurityContext securityContext )
    {
        return kernelTransactionWithInternals( securityContext ).transaction;
    }
}
