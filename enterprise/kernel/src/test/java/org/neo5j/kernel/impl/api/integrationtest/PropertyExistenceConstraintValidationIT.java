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
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import java.util.UUID;

import org.neo5j.collection.primitive.PrimitiveLongCollections;
import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo5j.kernel.api.DataWriteOperations;
import org.neo5j.kernel.api.ReadOperations;
import org.neo5j.kernel.api.SchemaWriteOperations;
import org.neo5j.kernel.api.Statement;
import org.neo5j.kernel.api.TokenWriteOperations;
import org.neo5j.kernel.api.exceptions.ConstraintViolationTransactionFailureException;
import org.neo5j.kernel.api.exceptions.KernelException;
import org.neo5j.kernel.api.exceptions.Status;
import org.neo5j.kernel.api.exceptions.TransactionFailureException;
import org.neo5j.kernel.api.properties.DefinedProperty;
import org.neo5j.kernel.api.security.AnonymousContext;
import org.neo5j.test.TestEnterpriseGraphDatabaseFactory;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.neo5j.kernel.api.properties.Property.property;
import static org.neo5j.kernel.api.schema_new.SchemaDescriptorFactory.forLabel;
import static org.neo5j.kernel.api.schema_new.SchemaDescriptorFactory.forRelType;
import static org.neo5j.kernel.impl.api.integrationtest.PropertyExistenceConstraintValidationIT.NodePropertyExistenceExistenceConstraintValidationIT;
import static org.neo5j.kernel.impl.api.integrationtest.PropertyExistenceConstraintValidationIT.RelationshipPropertyExistenceExistenceConstraintValidationIT;

@RunWith( Suite.class )
@SuiteClasses( {
        NodePropertyExistenceExistenceConstraintValidationIT.class,
        RelationshipPropertyExistenceExistenceConstraintValidationIT.class
} )
public class PropertyExistenceConstraintValidationIT
{
    public static class NodePropertyExistenceExistenceConstraintValidationIT
            extends AbstractPropertyExistenceConstraintValidationIT
    {
        @Test
        public void shouldAllowNoopLabelUpdate() throws Exception
        {
            // given
            long entityId = createConstraintAndEntity( "Label1", "key1", "value1" );

            Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );

            // when
            int label = statement.tokenWriteOperations().labelGetOrCreateForName( "Label1" );
            statement.dataWriteOperations().nodeAddLabel( entityId, label );

            // then should not throw exception
        }

        @Override
        void createConstraint( String key, String property ) throws KernelException
        {
            TokenWriteOperations tokenWriteOperations = tokenWriteOperationsInNewTransaction();
            int label = tokenWriteOperations.labelGetOrCreateForName( key );
            int propertyKey = tokenWriteOperations.propertyKeyGetOrCreateForName( property );
            commit();

            SchemaWriteOperations schemaWrite = schemaWriteOperationsInNewTransaction();
            schemaWrite.nodePropertyExistenceConstraintCreate( forLabel( label, propertyKey ) );
            commit();
        }

        @Override
        long createEntity( Statement statement, String type ) throws Exception
        {
            long node = statement.dataWriteOperations().nodeCreate();
            int labelId = statement.tokenWriteOperations().labelGetOrCreateForName( type );
            statement.dataWriteOperations().nodeAddLabel( node, labelId );
            return node;
        }

        @Override
        long createEntity( Statement statement, String property, String value ) throws Exception
        {
            long node = statement.dataWriteOperations().nodeCreate();
            int propertyKey = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( property );
            statement.dataWriteOperations().nodeSetProperty( node, property( propertyKey, value ) );
            return node;
        }

        @Override
        long createEntity( Statement statement, String type, String property, String value ) throws Exception
        {
            long node = createEntity( statement, type );
            int propertyKey = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( property );
            statement.dataWriteOperations().nodeSetProperty( node, property( propertyKey, value ) );
            return node;
        }

        @Override
        long createConstraintAndEntity( String type, String property, String value ) throws Exception
        {
            Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );
            int label = statement.tokenWriteOperations().labelGetOrCreateForName( type );
            long node = statement.dataWriteOperations().nodeCreate();
            statement.dataWriteOperations().nodeAddLabel( node, label );
            int propertyKey = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( property );
            statement.dataWriteOperations().nodeSetProperty( node, property( propertyKey, value ) );
            commit();

            createConstraint( type, property );

            return node;
        }

        @Override
        void setProperty( DataWriteOperations writeOps, long entityId, DefinedProperty property ) throws Exception
        {
            writeOps.nodeSetProperty( entityId, property );
        }

        @Override
        void removeProperty( DataWriteOperations writeOps, long entityId, int propertyKey ) throws Exception
        {
            writeOps.nodeRemoveProperty( entityId, propertyKey );
        }

        @Override
        int entityCount() throws TransactionFailureException
        {
            ReadOperations readOps = readOperationsInNewTransaction();
            int result = PrimitiveLongCollections.count( readOps.nodesGetAll() );
            rollback();
            return result;
        }
    }

    public static class RelationshipPropertyExistenceExistenceConstraintValidationIT
            extends AbstractPropertyExistenceConstraintValidationIT
    {
        @Override
        void createConstraint( String key, String property ) throws KernelException
        {
            TokenWriteOperations tokenWriteOperations = tokenWriteOperationsInNewTransaction();
            int relTypeId = tokenWriteOperations.relationshipTypeGetOrCreateForName( key );
            int propertyKeyId = tokenWriteOperations.propertyKeyGetOrCreateForName( property );
            commit();

            SchemaWriteOperations schemaWrite = schemaWriteOperationsInNewTransaction();
            schemaWrite.relationshipPropertyExistenceConstraintCreate( forRelType( relTypeId, propertyKeyId ) );
            commit();
        }

        @Override
        long createEntity( Statement statement, String type ) throws Exception
        {
            long start = statement.dataWriteOperations().nodeCreate();
            long end = statement.dataWriteOperations().nodeCreate();
            int relType = statement.tokenWriteOperations().relationshipTypeGetOrCreateForName( type );
            return statement.dataWriteOperations().relationshipCreate( relType, start, end );
        }

        @Override
        long createEntity( Statement statement, String property, String value ) throws Exception
        {
            long start = statement.dataWriteOperations().nodeCreate();
            long end = statement.dataWriteOperations().nodeCreate();
            String relationshipTypeName = UUID.randomUUID().toString();
            int relType = statement.tokenWriteOperations().relationshipTypeGetOrCreateForName( relationshipTypeName );
            long relationship = statement.dataWriteOperations().relationshipCreate( relType, start, end );

            int propertyKey = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( property );
            statement.dataWriteOperations().relationshipSetProperty( relationship, property( propertyKey, value ) );
            return relationship;
        }

        @Override
        long createEntity( Statement statement, String type, String property, String value ) throws Exception
        {
            long relationship = createEntity( statement, type );
            int propertyKey = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( property );
            statement.dataWriteOperations().relationshipSetProperty( relationship, property( propertyKey, value ) );
            return relationship;
        }

        @Override
        long createConstraintAndEntity( String type, String property, String value ) throws Exception
        {
            Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );
            int relType = statement.tokenWriteOperations().relationshipTypeGetOrCreateForName( type );
            long start = statement.dataWriteOperations().nodeCreate();
            long end = statement.dataWriteOperations().nodeCreate();
            long relationship = statement.dataWriteOperations().relationshipCreate( relType, start, end );
            int propertyKey = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( property );
            statement.dataWriteOperations().relationshipSetProperty( relationship, property( propertyKey, value ) );
            commit();

            createConstraint( type, property );

            return relationship;
        }

        @Override
        void setProperty( DataWriteOperations writeOps, long entityId, DefinedProperty property ) throws Exception
        {
            writeOps.relationshipSetProperty( entityId, property );
        }

        @Override
        void removeProperty( DataWriteOperations writeOps, long entityId, int propertyKey ) throws Exception
        {
            writeOps.relationshipRemoveProperty( entityId, propertyKey );
        }

        @Override
        int entityCount() throws TransactionFailureException
        {
            ReadOperations readOps = readOperationsInNewTransaction();
            int result = PrimitiveLongCollections.count( readOps.relationshipsGetAll() );
            rollback();
            return result;
        }
    }

    public abstract static class AbstractPropertyExistenceConstraintValidationIT extends KernelIntegrationTest
    {
        abstract void createConstraint( String key, String property ) throws KernelException;

        abstract long createEntity( Statement statement, String type ) throws Exception;

        abstract long createEntity( Statement statement, String property, String value ) throws Exception;

        abstract long createEntity( Statement statement, String type, String property, String value )
                throws Exception;

        abstract long createConstraintAndEntity( String type, String property, String value ) throws Exception;

        abstract void setProperty( DataWriteOperations writeOps, long entityId, DefinedProperty property )
                throws Exception;

        abstract void removeProperty( DataWriteOperations writeOps, long entityId, int propertyKey ) throws Exception;

        abstract int entityCount() throws TransactionFailureException;

        @Override
        protected GraphDatabaseService createGraphDatabase( EphemeralFileSystemAbstraction fs )
        {
            return new TestEnterpriseGraphDatabaseFactory()
                    .setFileSystem( fs )
                    .newImpermanentDatabaseBuilder()
                    .newGraphDatabase();
        }

        @Test
        public void shouldEnforcePropertyExistenceConstraintWhenCreatingEntityWithoutProperty() throws Exception
        {
            // given
            createConstraint( "Type1", "key1" );

            Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );

            // when
            createEntity( statement, "Type1" );
            try
            {
                commit();
                fail( "should have thrown exception" );
            }
            // then
            catch ( ConstraintViolationTransactionFailureException e )
            {
                Status expected = Status.Schema.ConstraintValidationFailed;
                assertThat( e.status(), is( expected ) );
            }
        }

        @Test
        public void shouldEnforceConstraintWhenRemoving() throws Exception
        {
            // given
            long entity = createConstraintAndEntity( "Type1", "key1", "value1" );
            Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );

            // when
            int key = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( "key1" );
            removeProperty( statement.dataWriteOperations(), entity, key );
            try
            {
                commit();
                fail( "should have thrown exception" );
            }
            // then
            catch ( ConstraintViolationTransactionFailureException e )
            {
                Status expected = Status.Schema.ConstraintValidationFailed;
                assertThat( e.status(), is( expected ) );
            }
        }

        @Test
        public void shouldAllowTemporaryViolationsWithinTransactions() throws Exception
        {
            // given
            long entity = createConstraintAndEntity( "Type1", "key1", "value1" );
            Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );

            // when
            int key = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( "key1" );
            //remove and put back
            removeProperty( statement.dataWriteOperations(), entity, key );
            setProperty( statement.dataWriteOperations(), entity, property( key, "value2" ) );

            commit();
        }

        @Test
        public void shouldAllowNoopPropertyUpdate() throws Exception
        {
            // given
            long entity = createConstraintAndEntity( "Type1", "key1", "value1" );

            Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );

            // when
            int key = statement.tokenWriteOperations().propertyKeyGetOrCreateForName( "key1" );
            setProperty( statement.dataWriteOperations(), entity, property( key, "value1" ) );

            // then should not throw exception
        }

        @Test
        public void shouldAllowCreationOfNonConflictingData() throws Exception
        {
            // given
            createConstraintAndEntity( "Type1", "key1", "value1" );

            Statement statement = statementInNewTransaction( AnonymousContext.writeToken() );

            // when
            createEntity( statement, "key1", "value1" );
            createEntity( statement, "Type2" );
            createEntity( statement, "Type1", "key1", "value2" );
            createEntity( statement, "Type1", "key1", "value1" );

            commit();

            // then
            assertEquals( 5, entityCount() );
        }
    }
}
