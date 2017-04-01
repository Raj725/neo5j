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
package org.neo5j.kernel.impl.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

import org.neo5j.kernel.api.KernelAPI;
import org.neo5j.kernel.api.KernelTransaction;
import org.neo5j.kernel.api.Statement;
import org.neo5j.kernel.api.exceptions.KernelException;
import org.neo5j.kernel.api.exceptions.TransactionFailureException;
import org.neo5j.kernel.api.exceptions.index.IndexNotFoundKernelException;
import org.neo5j.kernel.api.schema_new.SchemaDescriptorFactory;
import org.neo5j.kernel.api.schema_new.constaints.ConstraintDescriptor;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptor;
import org.neo5j.kernel.impl.api.index.SchemaIndexTestHelper;
import org.neo5j.kernel.internal.GraphDatabaseAPI;
import org.neo5j.test.rule.ImpermanentDatabaseRule;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.neo5j.kernel.api.security.SecurityContext.AUTH_DISABLED;

public class KernelSchemaStateFlushingTest
{
    @Rule
    public ImpermanentDatabaseRule dbRule = new ImpermanentDatabaseRule();

    private GraphDatabaseAPI db;
    private KernelAPI kernel;

    @Test
    public void shouldKeepSchemaStateIfSchemaIsNotModified() throws TransactionFailureException
    {
        // given
        String before = commitToSchemaState( "test", "before" );

        // then
        assertEquals( "before", before );

        // given
        String after = commitToSchemaState( "test", "after" );

        // then
        assertEquals( "before", after );
    }

    @Test
    public void shouldInvalidateSchemaStateOnCreateIndex() throws Exception
    {
        // given
        commitToSchemaState( "test", "before" );

        awaitIndexOnline( createIndex(), "test" );

        // when
        String after = commitToSchemaState( "test", "after" );

        // then
        assertEquals( "after", after );
    }

    @Test
    public void shouldInvalidateSchemaStateOnDropIndex() throws Exception
    {
        NewIndexDescriptor descriptor = createIndex();

        awaitIndexOnline( descriptor, "test" );

        commitToSchemaState( "test", "before" );

        dropIndex( descriptor );

        // when
        String after = commitToSchemaState( "test", "after" );

        // then
        assertEquals( "after", after );
    }

    @Test
    public void shouldInvalidateSchemaStateOnCreateConstraint() throws Exception
    {
        // given
        commitToSchemaState( "test", "before" );

        createConstraint();

        // when
        String after = commitToSchemaState( "test", "after" );

        // then
        assertEquals( "after", after );
    }

    @Test
    public void shouldInvalidateSchemaStateOnDropConstraint() throws Exception
    {
        // given
        ConstraintDescriptor descriptor = createConstraint();

        commitToSchemaState( "test", "before" );

        dropConstraint( descriptor );

        // when
        String after = commitToSchemaState( "test", "after" );

        // then
        assertEquals( "after", after );
    }

    private ConstraintDescriptor createConstraint() throws KernelException
    {

        try ( KernelTransaction transaction = kernel.newTransaction( KernelTransaction.Type.implicit, AUTH_DISABLED );
              Statement statement = transaction.acquireStatement() )
        {
            ConstraintDescriptor descriptor = statement.schemaWriteOperations().uniquePropertyConstraintCreate(
                    SchemaDescriptorFactory.forLabel( 1, 1 ) );
            transaction.success();
            return descriptor;
        }
    }

    private void dropConstraint( ConstraintDescriptor descriptor ) throws KernelException
    {
        try ( KernelTransaction transaction = kernel.newTransaction( KernelTransaction.Type.implicit, AUTH_DISABLED );
              Statement statement = transaction.acquireStatement() )
        {
            statement.schemaWriteOperations().constraintDrop( descriptor );
            transaction.success();
        }
    }

    private NewIndexDescriptor createIndex() throws KernelException
    {
        try ( KernelTransaction transaction = kernel.newTransaction( KernelTransaction.Type.implicit, AUTH_DISABLED );
              Statement statement = transaction.acquireStatement() )
        {
            NewIndexDescriptor descriptor = statement.schemaWriteOperations().indexCreate(
                    SchemaDescriptorFactory.forLabel( 1, 1 ) );
            transaction.success();
            return descriptor;
        }
    }

    private void dropIndex( NewIndexDescriptor descriptor ) throws KernelException
    {
        try ( KernelTransaction transaction = kernel.newTransaction( KernelTransaction.Type.implicit, AUTH_DISABLED );
              Statement statement = transaction.acquireStatement() )
        {
            statement.schemaWriteOperations().indexDrop( descriptor );
            transaction.success();
        }
    }

    private void awaitIndexOnline( NewIndexDescriptor descriptor, String keyForProbing )
            throws IndexNotFoundKernelException, TransactionFailureException
    {
        try ( KernelTransaction transaction = kernel.newTransaction( KernelTransaction.Type.implicit, AUTH_DISABLED );
              Statement statement = transaction.acquireStatement() )
        {
            SchemaIndexTestHelper.awaitIndexOnline( statement.readOperations(), descriptor );
            transaction.success();
        }
        awaitSchemaStateCleared( keyForProbing );
    }

    private void awaitSchemaStateCleared( String keyForProbing ) throws TransactionFailureException
    {
        try ( KernelTransaction transaction = kernel.newTransaction( KernelTransaction.Type.implicit, AUTH_DISABLED );
              Statement statement = transaction.acquireStatement() )
        {
            while ( statement.readOperations().schemaStateGetOrCreate( keyForProbing, (ignored) -> null ) != null )
            {
                LockSupport.parkNanos( MILLISECONDS.toNanos( 10 ) );
            }
            transaction.success();
        }
    }

    private String commitToSchemaState( String key, String value ) throws TransactionFailureException
    {
        try ( KernelTransaction transaction = kernel.newTransaction( KernelTransaction.Type.implicit, AUTH_DISABLED ) )
        {
            String result = getOrCreateFromState( transaction, key, value );
            transaction.success();
            return result;
        }
    }

    private String getOrCreateFromState( KernelTransaction tx, String key, final String value )
    {
        try ( Statement statement = tx.acquireStatement() )
        {
            return statement.readOperations().schemaStateGetOrCreate( key, from -> value );
        }
    }

    @Before
    public void setup()
    {
        db = dbRule.getGraphDatabaseAPI();
        kernel = db.getDependencyResolver().resolveDependency( KernelAPI.class );
    }

    @After
    public void after()
    {
        db.shutdown();
    }
}
