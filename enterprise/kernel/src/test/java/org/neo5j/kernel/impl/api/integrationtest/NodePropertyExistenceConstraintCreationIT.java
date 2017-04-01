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
import org.neo5j.kernel.api.TokenWriteOperations;
import org.neo5j.kernel.api.ReadOperations;
import org.neo5j.kernel.api.SchemaWriteOperations;
import org.neo5j.kernel.api.exceptions.KernelException;
import org.neo5j.kernel.api.exceptions.schema.DropConstraintFailureException;
import org.neo5j.kernel.api.exceptions.schema.NoSuchConstraintException;
import org.neo5j.kernel.api.schema_new.LabelSchemaDescriptor;
import org.neo5j.kernel.api.schema_new.SchemaDescriptorFactory;
import org.neo5j.kernel.api.schema_new.constaints.ConstraintBoundary;
import org.neo5j.kernel.api.schema_new.constaints.ConstraintDescriptor;
import org.neo5j.kernel.api.schema_new.constaints.ConstraintDescriptorFactory;
import org.neo5j.kernel.api.schema_new.constaints.NodeExistenceConstraintDescriptor;
import org.neo5j.kernel.api.schema_new.constaints.UniquenessConstraintDescriptor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.neo5j.graphdb.Label.label;
import static org.neo5j.helpers.collection.Iterators.single;

public class NodePropertyExistenceConstraintCreationIT
        extends AbstractConstraintCreationIT<NodeExistenceConstraintDescriptor,LabelSchemaDescriptor>
{
    @Override
    int initializeLabelOrRelType( TokenWriteOperations tokenWriteOperations, String name ) throws KernelException
    {
        return tokenWriteOperations.labelGetOrCreateForName( name );
    }

    @Override
    NodeExistenceConstraintDescriptor createConstraint( SchemaWriteOperations writeOps, LabelSchemaDescriptor descriptor )
            throws Exception
    {
        return writeOps.nodePropertyExistenceConstraintCreate( descriptor );
    }

    @Override
    void createConstraintInRunningTx( GraphDatabaseService db, String label, String property )
    {
        SchemaHelper.createNodePropertyExistenceConstraint( db, label, property );
    }

    @Override
    NodeExistenceConstraintDescriptor newConstraintObject(LabelSchemaDescriptor descriptor )
    {
        return ConstraintDescriptorFactory.existsForSchema( descriptor );
    }

    @Override
    void dropConstraint( SchemaWriteOperations writeOps, NodeExistenceConstraintDescriptor constraint )
            throws Exception
    {
        writeOps.constraintDrop( constraint );
    }

    @Override
    void createOffendingDataInRunningTx( GraphDatabaseService db )
    {
        db.createNode( label( KEY ) );
    }

    @Override
    void removeOffendingDataInRunningTx( GraphDatabaseService db )
    {
        try ( ResourceIterator<Node> nodes = db.findNodes( label( KEY ) ) )
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
        return SchemaDescriptorFactory.forLabel( typeId, propertyKeyId );
    }

    @Test
    public void shouldNotDropPropertyExistenceConstraintThatDoesNotExistWhenThereIsAUniquePropertyConstraint()
            throws Exception
    {
        // given
        UniquenessConstraintDescriptor constraint;
        {
            SchemaWriteOperations statement = schemaWriteOperationsInNewTransaction();
            constraint = statement.uniquePropertyConstraintCreate( descriptor );
            commit();
        }

        // when
        try
        {
            SchemaWriteOperations statement = schemaWriteOperationsInNewTransaction();
            statement.constraintDrop( ConstraintDescriptorFactory.existsForSchema( constraint.schema() ) );

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

            assertEquals( constraint, single( constraints ) );
        }
    }
}
