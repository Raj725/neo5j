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
package org.neo5j.kernel.impl.api.state;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import org.neo5j.collection.primitive.Primitive;
import org.neo5j.collection.primitive.PrimitiveIntCollections;
import org.neo5j.collection.primitive.PrimitiveIntSet;
import org.neo5j.cursor.Cursor;
import org.neo5j.kernel.api.exceptions.EntityNotFoundException;
import org.neo5j.kernel.api.properties.Property;
import org.neo5j.kernel.api.schema_new.LabelSchemaDescriptor;
import org.neo5j.kernel.api.schema_new.OrderedPropertyValues;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptor;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptorFactory;
import org.neo5j.kernel.api.txstate.TransactionState;
import org.neo5j.kernel.impl.api.KernelStatement;
import org.neo5j.kernel.impl.api.operations.EntityReadOperations;
import org.neo5j.kernel.impl.api.operations.SchemaReadOperations;
import org.neo5j.storageengine.api.NodeItem;
import org.neo5j.storageengine.api.PropertyItem;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.neo5j.helpers.collection.Iterators.filter;
import static org.neo5j.kernel.api.properties.Property.property;
import static org.neo5j.kernel.api.schema_new.SchemaDescriptorPredicates.hasLabel;
import static org.neo5j.kernel.api.schema_new.index.NewIndexDescriptor.Filter.GENERAL;
import static org.neo5j.kernel.api.schema_new.index.NewIndexDescriptor.Filter.UNIQUE;
import static org.neo5j.kernel.impl.api.state.IndexTxStateUpdater.LabelChangeType.ADDED_LABEL;
import static org.neo5j.kernel.impl.api.state.IndexTxStateUpdater.LabelChangeType.REMOVED_LABEL;

public class IndexTxStateUpdaterTest
{
    private static final int labelId1 = 10;
    private static final int labelId2 = 11;
    private static final int unIndexedLabelId = 12;
    private static final int propId1 = 20;
    private static final int propId2 = 21;
    private static final int propId3 = 22;
    private static final int newPropId = 23;
    private static final int unIndexedPropId = 24;

    private KernelStatement state;
    private TransactionState txState;
    private IndexTxStateUpdater indexTxUpdater;
    private NodeItem node;

    private NewIndexDescriptor indexOn1_1 = NewIndexDescriptorFactory.forLabel( labelId1, propId1 );
    private NewIndexDescriptor indexOn2_new = NewIndexDescriptorFactory.forLabel( labelId2, newPropId );
    private NewIndexDescriptor uniqueOn1_2 = NewIndexDescriptorFactory.uniqueForLabel( labelId1, propId2 );
    private NewIndexDescriptor indexOn1_1_new = NewIndexDescriptorFactory.forLabel( labelId1, propId1, newPropId );
    private NewIndexDescriptor uniqueOn2_2_3 = NewIndexDescriptorFactory.uniqueForLabel( labelId2, propId2, propId3 );
    private List<NewIndexDescriptor> indexes =
            Arrays.asList( indexOn1_1, indexOn2_new, uniqueOn1_2, indexOn1_1_new, uniqueOn2_2_3 );

    @Before
    public void setup()
    {
        state = mock( KernelStatement.class );
        txState = mock( TransactionState.class );
        when( state.txState() ).thenReturn( txState );

        SchemaReadOperations schemaReadOps = mock( SchemaReadOperations.class );
        when( schemaReadOps.indexesGetAll( state ) ).thenAnswer(
                x -> filter( GENERAL, indexes.iterator() ) );
        when( schemaReadOps.uniqueIndexesGetAll( state ) ).thenAnswer(
                x -> filter( UNIQUE, indexes.iterator() ) );
        when( schemaReadOps.indexesGetForLabel( state, labelId1 ) ).thenAnswer(
                x -> filter( GENERAL, filter( hasLabel( labelId1 ), indexes.iterator() ) ) );
        when( schemaReadOps.uniqueIndexesGetForLabel( state, labelId1 ) ).thenAnswer(
                x -> filter( UNIQUE, filter( hasLabel( labelId1 ), indexes.iterator() ) ) );
        when( schemaReadOps.indexesGetForLabel( state, labelId2 ) ).thenAnswer(
                x -> filter( GENERAL, filter( hasLabel( labelId2 ), indexes.iterator() ) ) );
        when( schemaReadOps.uniqueIndexesGetForLabel( state, labelId2 ) ).thenAnswer(
                x -> filter( UNIQUE, filter( hasLabel( labelId2 ), indexes.iterator() ) ) );

        PrimitiveIntSet labels = Primitive.intSet();
        labels.add( labelId1 );
        labels.add( labelId2 );

        Cursor<NodeItem> nodeItemCursor = StubCursors.asNodeCursor( 0, labels );
        nodeItemCursor.next();
        node = nodeItemCursor.get();

        PrimitiveIntSet defaultPropertyIds = PrimitiveIntCollections.asSet( new int[]{ propId1, propId2, propId3 } );
        EntityReadOperations readOps = mock( EntityReadOperations.class );
        when( readOps.nodeGetPropertyKeys( state, node ) ).thenReturn( defaultPropertyIds );
        when( readOps.nodeGetProperties( state, node ) ).thenAnswer( p -> StubCursors.asPropertyCursor(
                Property.property( propId1, "hi1" ),
                Property.property( propId2, "hi2" ),
                Property.property( propId3, "hi3" ) ) );
        when( readOps.nodeGetProperty( state, node, propId1 ) ).thenReturn( "hi1" );
        when( readOps.nodeGetProperty( state, node, propId2 ) ).thenReturn( "hi2" );
        when( readOps.nodeGetProperty( state, node, propId3 ) ).thenReturn( "hi3" );

        indexTxUpdater = new IndexTxStateUpdater( schemaReadOps, readOps );

    }

    // LABELS

    @Test
    public void shouldNotUpdateIndexesOnChangedIrrelevantLabel() throws EntityNotFoundException
    {
        // WHEN
        indexTxUpdater.onLabelChange( state, unIndexedLabelId, node, ADDED_LABEL );
        indexTxUpdater.onLabelChange( state, unIndexedLabelId, node, REMOVED_LABEL );

        // THEN
        verify( txState, times( 0 ) ).indexDoUpdateEntry( any(), anyInt(), any(), any() );
    }

    @Test
    public void shouldUpdateIndexesOnAddedLabel() throws EntityNotFoundException
    {
        // WHEN
        indexTxUpdater.onLabelChange( state, labelId1, node, ADDED_LABEL );

        // THEN
        verifyIndexUpdate( indexOn1_1.schema(), node.id(), null, values( "hi1" ) );
        verifyIndexUpdate( uniqueOn1_2.schema(), node.id(), null, values( "hi2" ) );
        verify( txState, times( 2 ) ).indexDoUpdateEntry( any(), anyInt(), any(), any() );
    }

    @Test
    public void shouldUpdateIndexesOnRemovedLabel() throws EntityNotFoundException
    {
        // WHEN
        indexTxUpdater.onLabelChange( state, labelId2, node, REMOVED_LABEL );

        // THEN
        verifyIndexUpdate( uniqueOn2_2_3.schema(), node.id(), values( "hi2", "hi3" ), null );
        verify( txState, times( 1 ) ).indexDoUpdateEntry( any(), anyInt(), any(), any() );
    }

    // PROPERTIES

    @Test
    public void shouldNotUpdateIndexesOnChangedIrrelevantProperty() throws EntityNotFoundException
    {
        // WHEN
        indexTxUpdater.onPropertyAdd( state, node, property( unIndexedPropId, "whAt" ) );
        indexTxUpdater.onPropertyRemove( state, node, property( unIndexedPropId, "whAt" ) );
        indexTxUpdater.onPropertyChange( state, node,
                property( unIndexedPropId, "whAt" ), property( unIndexedPropId, "whAt2" ) );

        // THEN
        verify( txState, times( 0 ) ).indexDoUpdateEntry( any(), anyInt(), any(), any() );
    }

    @Test
    public void shouldUpdateIndexesOnAddedProperty() throws EntityNotFoundException
    {
        // WHEN
        indexTxUpdater.onPropertyAdd( state, node, property( newPropId, "newHi" ) );

        // THEN
        verifyIndexUpdate( indexOn2_new.schema(), node.id(), null, values( "newHi" ) );
        verifyIndexUpdate( indexOn1_1_new.schema(), node.id(), null, values( "hi1", "newHi" ) );
        verify( txState, times( 2 ) ).indexDoUpdateEntry( any(), anyInt(), any(), any() );
    }

    @Test
    public void shouldUpdateIndexesOnRemovedProperty() throws EntityNotFoundException
    {
        // WHEN
        indexTxUpdater.onPropertyRemove( state, node, property( propId2, "hi2" ) );

        // THEN
        verifyIndexUpdate( uniqueOn1_2.schema(), node.id(), values( "hi2" ), null );
        verifyIndexUpdate( uniqueOn2_2_3.schema(), node.id(), values( "hi2", "hi3" ), null );
        verify( txState, times( 2 ) ).indexDoUpdateEntry( any(), anyInt(), any(), any() );
    }

    @Test
    public void shouldUpdateIndexesOnChangesProperty() throws EntityNotFoundException
    {
        // WHEN
        indexTxUpdater.onPropertyChange( state, node,
                property( propId2, "hi2" ), property( propId2, "new2" ) );

        // THEN
        verifyIndexUpdate( uniqueOn1_2.schema(), node.id(), values( "hi2" ), values( "new2" ) );
        verifyIndexUpdate( uniqueOn2_2_3.schema(), node.id(), values( "hi2", "hi3" ), values( "new2", "hi3" ) );
        verify( txState, times( 2 ) ).indexDoUpdateEntry( any(), anyInt(), any(), any() );
    }

    private OrderedPropertyValues values( Object... values )
    {
        return OrderedPropertyValues.ofUndefined( values );
    }

    private void verifyIndexUpdate(
            LabelSchemaDescriptor schema, long nodeId, OrderedPropertyValues before, OrderedPropertyValues after )
    {
        verify( txState ).indexDoUpdateEntry( eq( schema ), eq( nodeId), eq( before ), eq( after ) );
    }

}
