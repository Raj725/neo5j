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
package org.neo5j.kernel.api.exceptions.schema;

import org.neo5j.kernel.api.TokenNameLookup;
import org.neo5j.kernel.api.exceptions.Status;
import org.neo5j.kernel.api.schema_new.SchemaDescriptor;
import org.neo5j.kernel.api.schema_new.SchemaUtil;

public class RepeatedPropertyInCompositeSchemaException extends SchemaKernelException
{
    private final SchemaDescriptor schema;
    private final OperationContext context;

    public RepeatedPropertyInCompositeSchemaException( SchemaDescriptor schema, OperationContext context )
    {
        super( Status.Schema.RepeatedPropertyInCompositeSchema, format(
                schema, context, SchemaUtil.idTokenNameLookup ) );
        this.schema = schema;
        this.context = context;
    }

    @Override
    public String getUserMessage( TokenNameLookup tokenNameLookup )
    {
        return format( schema, context, tokenNameLookup );
    }

    private static String format(
            SchemaDescriptor schema, OperationContext context, TokenNameLookup tokenNameLookup )
    {
        String schemaName;
        switch ( context )
        {
        case INDEX_CREATION:
            schemaName = "Index";
            break;

        case CONSTRAINT_CREATION:
            schemaName = "Constraint";
            break;

        default:
            schemaName = "Schema object";
            break;
        }
        return String.format( "%s on %s includes a property more than once.",
                schemaName, schema.userDescription( tokenNameLookup ) );

    }
}
