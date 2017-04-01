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
package org.neo5j.kernel.impl.api.integrationtest;

import org.junit.Test;

import java.util.Iterator;

import org.neo5j.SchemaHelper;
import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.graphdb.Node;
import org.neo5j.graphdb.ResourceIterator;
import org.neo5j.kernel.api.ReadOperations;
import org.neo5j.kernel.api.SchemaWriteOperations;
import org.neo5j.kernel.api.Statement;
import org.neo5j.kernel.api.StatementTokenNameLookup;
import org.neo5j.kernel.api.TokenWriteOperations;
import org.neo5j.kernel.api.exceptions.KernelException;
import org.neo5j.kernel.api.exceptions.TransactionFailureException;
import org.neo5j.kernel.api.exceptions.schema.ConstraintValidationException;
import org.neo5j.kernel.api.exceptions.schema.CreateConstraintFailureException;
import org.neo5j.kernel.api.exceptions.schema.DropConstraintFailureException;
import org.neo5j.kernel.api.exceptions.schema.NoSuchConstraintException;
import org.neo5j.kernel.api.properties.Property;
import org.neo5j.kernel.api.schema_new.LabelSchemaDescriptor;
import org.neo5j.kernel.api.schema_new.SchemaDescriptorFactory;
import org.neo5j.kernel.api.schema_new.constaints.ConstraintDescriptor;
import org.neo5j.kernel.api.schema_new.constaints.ConstraintDescriptorFactory;
import org.neo5j.kernel.api.schema_new.constaints.UniquenessConstraintDescriptor;
import org.neo5j.kernel.api.security.AnonymousContext;
import org.neo5j.kernel.api.security.SecurityContext;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptor;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptorFactory;
import org.neo5j.kernel.impl.storageengine.impl.recordstorage.RecordStorageEngine;
import org.neo5j.kernel.impl.store.NeoStores;
import org.neo5j.kernel.impl.store.SchemaStorage;
import org.neo5j.kernel.impl.store.record.ConstraintRule;
import org.neo5j.kernel.impl.store.record.IndexRule;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.neo5j.graphdb.Label.label;
import static org.neo5j.helpers.collection.Iterators.asSet;
import static org.neo5j.helpers.collection.Iterators.emptySetOf;
import static org.neo5j.helpers.collection.Iterators.single;

public class UniquenessConstraintCreationIT
        extends AbstractConstraintCreationIT<UniquenessConstraintDescriptor,LabelSchemaDescriptor>
{
    private static final String DUPLICATED_VALUE = "apa";
    private NewIndexDescriptor uniqueIndex;

    @Override
    int initializeLabelOrRelType( TokenWriteOperations tokenWriteOperations, String name ) throws KernelException
    {
        return tokenWriteOperations.labelGetOrCreateForName( KEY );
    }

    @Override
    UniquenessConstraintDescriptor createConstraint( SchemaWriteOperations writeOps, LabelSchemaDescriptor descriptor )
            throws Exception
    {
        return writeOps.uniquePropertyConstraintCreate( descriptor );
    }

    @Override
    void createConstraintInRunningTx( GraphDatabaseService db, String type, String property )
    {
        SchemaHelper.createUniquenessConstraint( db, type, property );
    }

    @Override
    UniquenessConstraintDescriptor newConstraintObject( LabelSchemaDescriptor descriptor )
    {
        return ConstraintDescriptorFactory.uniqueForSchema( descriptor );
    }

    @Override
    void dropConstraint( SchemaWriteOperations writeOps, UniquenessConstraintDescriptor constraint ) throws Exception
    {
        writeOps.constraintDrop( constraint );
    }

    @Override
    void createOffendingDataInRunningTx( GraphDatabaseService db )
    {
        db.createNode( label( KEY ) ).setProperty( PROP, DUPLICATED_VALUE );
        db.createNode( label( KEY ) ).setProperty( PROP, DUPLICATED_VALUE );
    }

    @Override
    void removeOffendingDataInRunningTx( GraphDatabaseService db )
    {
        try ( ResourceIterator<Node> nodes = db.findNodes( label( KEY ), PROP, DUPLICATED_VALUE ) )
        {
            while ( nodes.hasNext() )
            {
                nodes.next().delete();
            }
        }
    }

    @Override
    LabelSchemaDescriptor makeDescriptor( int typeId, int propertyKeyId )
    {
        uniqueIndex = NewIndexDescriptorFactory.uniqueForLabel( typeId, propertyKeyId );
        return SchemaDescriptorFactory.forLabel( typeId, propertyKeyId );
    }

    @Test
    public void shouldAbortConstraintCreationWhenDuplicatesExist() throws Exception
    {
        // given
        Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );
        // name is not unique for Foo in the existing data

        int foo = statement.tokenWriteOperations().labelGetOrCreateForName( "Foo" );
        int name = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( "name" );

        long node1 = statement.dataWriteOperations().nodeCreate();

        statement.dataWriteOperations().nodeAddLabel( node1, foo );
        statement.dataWriteOperations().nodeSetProperty( node1, Property.stringProperty( name, "foo" ) );

        long node2 = statement.dataWriteOperations().nodeCreate();
        statement.dataWriteOperations().nodeAddLabel( node2, foo );

        statement.dataWriteOperations().nodeSetProperty( node2, Property.stringProperty( name, "foo" ) );
        commit();

        // when
        LabelSchemaDescriptor descriptor = SchemaDescriptorFactory.forLabel( foo, name );
        try
        {
            SchemaWriteOperations schemaWriteOperations = schemaWriteOperationsInNewTransaction();
            schemaWriteOperations.uniquePropertyConstraintCreate( descriptor );

            fail( "expected exception" );
        }
        // then
        catch ( CreateConstraintFailureException ex )
        {
            assertEquals( ConstraintDescriptorFactory.uniqueForSchema( descriptor ), ex.constraint() );
            Throwable cause = ex.getCause();
            assertThat( cause, instanceOf( ConstraintValidationException.class ) );

            String expectedMessage = String.format(
                    "Both Node(%d) and Node(%d) have the label `Foo` and property `name` = 'foo'", node1, node2 );
            String actualMessage = userMessage( (ConstraintValidationException) cause );
            assertEquals( expectedMessage, actualMessage );
        }
    }

    @Test
    public void shouldCreateAnIndexToGoAlongWithAUniquePropertyConstraint() throws Exception
    {
        // when
        SchemaWriteOperations schemaWriteOperations = schemaWriteOperationsInNewTransaction();
        schemaWriteOperations.uniquePropertyConstraintCreate( descriptor );
        commit();

        // then
        ReadOperations readOperations = readOperationsInNewTransaction();
        assertEquals( asSet( uniqueIndex ), asSet( readOperations.uniqueIndexesGetAll() ) );
    }

    @Test
    public void shouldDropCreatedConstraintIndexWhenRollingBackConstraintCreation() throws Exception
    {
        // given
        Statement statement = statementInNewTransaction( SecurityContext.AUTH_DISABLED );
        statement.schemaWriteOperations().uniquePropertyConstraintCreate( descriptor );
        assertEquals( asSet( uniqueIndex ),
                asSet( statement.readOperations().uniqueIndexesGetAll() ) );

        // when
        rollback();

        // then
        ReadOperations readOperations = readOperationsInNewTransaction();
        assertEquals( emptySetOf( NewIndexDescriptor.class ), asSet( readOperations.uniqueIndexesGetAll() ) );
        commit();
    }

    @Test
    public void shouldNotDropUniquePropertyConstraintThatDoesNotExistWhenThereIsAPropertyExistenceConstraint()
            throws Exception
    {
        // given
        SchemaWriteOperations schemaWriteOperations = schemaWriteOperationsInNewTransaction();
        schemaWriteOperations.nodePropertyExistenceConstraintCreate( descriptor );
        commit();

        // when
        try
        {
            SchemaWriteOperations statement = schemaWriteOperationsInNewTransaction();
            statement.constraintDrop( ConstraintDescriptorFactory.uniqueForSchema( descriptor ) );

            fail( "expected exception" );
        }
        // then
        catch ( DropConstraintFailureException e )
        {
            assertThat( e.getCause(), instanceOf( NoSuchConstraintException.class ) );
        }
        finally
        {
            rollback();
        }

        // then
        {
            ReadOperations statement = readOperationsInNewTransaction();

            Iterator<ConstraintDescriptor> constraints = statement.constraintsGetForSchema( descriptor );

            assertEquals( ConstraintDescriptorFactory.existsForSchema( descriptor ), single( constraints ) );
        }
    }

    @Test
    public void committedConstraintRuleShouldCrossReferenceTheCorrespondingIndexRule() throws Exception
    {
        // when
        SchemaWriteOperations statement = schemaWriteOperationsInNewTransaction();
        statement.uniquePropertyConstraintCreate( descriptor );
        commit();

        // then
        SchemaStorage schema = new SchemaStorage( neoStores().getSchemaStore() );
        IndexRule indexRule = schema.indexGetForSchema( NewIndexDescriptorFactory.uniqueForLabel( typeId, propertyKeyId ) );
        ConstraintRule constraintRule = schema.constraintsGetSingle(
                ConstraintDescriptorFactory.uniqueForLabel( typeId, propertyKeyId ) );
        assertEquals( constraintRule.getId(), indexRule.getOwningConstraint().longValue() );
        assertEquals( indexRule.getId(), constraintRule.getOwnedIndex() );
    }

    private NeoStores neoStores()
    {
        return db.getDependencyResolver().resolveDependency( RecordStorageEngine.class ).testAccessNeoStores();
    }

    @Test
    public void shouldDropConstraintIndexWhenDroppingConstraint() throws Exception
    {
        // given
        Statement statement = statementInNewTransaction( SecurityContext.AUTH_DISABLED );
        UniquenessConstraintDescriptor constraint =
                statement.schemaWriteOperations().uniquePropertyConstraintCreate( descriptor );
        assertEquals( asSet( uniqueIndex ),
                asSet( statement.readOperations().uniqueIndexesGetAll() ) );
        commit();

        // when
        SchemaWriteOperations schemaWriteOperations = schemaWriteOperationsInNewTransaction();
        schemaWriteOperations.constraintDrop( constraint );
        commit();

        // then
        ReadOperations readOperations = readOperationsInNewTransaction();
        assertEquals( emptySetOf( NewIndexDescriptor.class ), asSet( readOperations.uniqueIndexesGetAll() ) );
        commit();
    }

    private String userMessage( ConstraintValidationException cause )
            throws TransactionFailureException
    {
        StatementTokenNameLookup lookup = new StatementTokenNameLookup( readOperationsInNewTransaction() );
        String actualMessage = cause.getUserMessage( lookup );
        commit();
        return actualMessage;
    }
}
