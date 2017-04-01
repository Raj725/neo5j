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
package org.neo5j.storageengine.api.schema;

import org.neo5j.kernel.api.exceptions.schema.MalformedSchemaRuleException;
import org.neo5j.kernel.api.schema_new.LabelSchemaDescriptor;
import org.neo5j.kernel.api.schema_new.RelationTypeSchemaDescriptor;
import org.neo5j.kernel.api.schema_new.SchemaComputer;
import org.neo5j.kernel.api.schema_new.SchemaDescriptor;
import org.neo5j.kernel.api.schema_new.constaints.ConstraintDescriptor;
import org.neo5j.kernel.api.schema_new.index.NewIndexDescriptor;
import org.neo5j.kernel.impl.store.record.ConstraintRule;
import org.neo5j.kernel.impl.store.record.IndexRule;

/**
 * Represents a stored schema rule.
 */
public abstract class SchemaRule implements SchemaDescriptor.Supplier
{
    protected final long id;
    protected final String name;

    protected SchemaRule( long id )
    {
        this( id, null );
    }

    protected SchemaRule( long id, String name )
    {
        this.id = id;
        this.name = name == null? generateName( id, getClass() ) : checkName( name );
    }

    private String checkName( String name )
    {
        int length = name.length();
        if ( length == 0 )
        {
            throw new IllegalArgumentException( "Schema rule name cannot be the empty string" );
        }
        for ( int i = 0; i < length; i++ )
        {
            char ch = name.charAt( i );
            if ( ch == '\0' )
            {
                throw new IllegalArgumentException( "Illegal schema rule name: '" + name + "'" );
            }
        }
        return name;
    }

    /**
     * The persistence id for this rule.
     */
    public final long getId()
    {
        return this.id;
    }

    /**
     * @return The (possibly user supplied) name of this schema rule.
     */
    public final String getName()
    {
        return name;
    }

    public abstract byte[] serialize();

    public static String generateName( long id, Class<? extends SchemaRule> type )
    {
        if ( type == IndexRule.class )
        {
            return "index_" + id;
        }
        if ( type == ConstraintRule.class )
        {
            return "constraint_" + id;
        }
        return "schema_" + id;
    }

    /**
     * This enum is used for the legacy schema store, and should not be extended.
     * @see org.neo5j.kernel.impl.store.record.SchemaRuleSerialization for the new (de)serialisation code instead.
     */
    @Deprecated
    public enum Kind
    {
        INDEX_RULE( "Index" ),
        CONSTRAINT_INDEX_RULE( "Constraint index" ),
        UNIQUENESS_CONSTRAINT( "Uniqueness constraint" ),
        NODE_PROPERTY_EXISTENCE_CONSTRAINT( "Node property existence constraint" ),
        RELATIONSHIP_PROPERTY_EXISTENCE_CONSTRAINT( "Relationship property existence constraint" );

        private static final Kind[] ALL = values();

        private final String userString;

        Kind( String userString )
        {
            this.userString = userString;
        }

        public byte id()
        {
            return (byte) (ordinal() + 1);
        }

        public String userString()
        {
            return userString;
        }

        public static Kind forId( byte id ) throws MalformedSchemaRuleException
        {
            if ( id >= 1 && id <= ALL.length )
            {
                return values()[id-1];
            }
            throw new MalformedSchemaRuleException( null, "Unknown kind id %d", id );
        }

        public static Kind map( NewIndexDescriptor descriptor )
        {
            switch ( descriptor.type() )
            {
            case GENERAL:
                return INDEX_RULE;
            case UNIQUE:
                return CONSTRAINT_INDEX_RULE;
            default:
                throw new IllegalStateException(
                        "Cannot map descriptor type to legacy schema rule: " + descriptor.type() );
            }
        }

        public static Kind map( ConstraintDescriptor descriptor )
        {
            switch ( descriptor.type() )
            {
            case UNIQUE:
                return UNIQUENESS_CONSTRAINT;
            case EXISTS:
                return descriptor.schema().computeWith( existenceKindMapper );
            default:
                throw new IllegalStateException(
                        "Cannot map descriptor type to legacy schema rule: " + descriptor.type() );
            }
        }

        private static SchemaComputer<Kind> existenceKindMapper = new SchemaComputer<Kind>()
        {
            @Override
            public Kind computeSpecific( LabelSchemaDescriptor schema )
            {
                return NODE_PROPERTY_EXISTENCE_CONSTRAINT;
            }

            @Override
            public Kind computeSpecific( RelationTypeSchemaDescriptor schema )
            {
                return RELATIONSHIP_PROPERTY_EXISTENCE_CONSTRAINT;
            }
        };
    }
}
