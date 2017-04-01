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
package org.neo5j.kernel.impl.proc;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.neo5j.kernel.api.proc.FieldSignature;
import org.neo5j.kernel.api.proc.Neo5jTypes;
import org.neo5j.kernel.api.proc.UserFunctionSignature;

import static org.junit.Assert.assertEquals;
import static org.neo5j.kernel.api.proc.UserFunctionSignature.functionSignature;

public class UserFunctionSignatureTest
{
    @Rule
    public ExpectedException exception = ExpectedException.none();
    private final UserFunctionSignature signature =
            functionSignature( "asd" ).in( "in", Neo5jTypes.NTAny ).out( Neo5jTypes.NTAny ).build();

    @Test
    public void inputSignatureShouldNotBeModifiable() throws Throwable
    {
        // Expect
        exception.expect( UnsupportedOperationException.class );

        // When
        signature.inputSignature().add( new FieldSignature( "in2", Neo5jTypes.NTAny ));
    }

    @Test
    public void toStringShouldMatchCypherSyntax() throws Throwable
    {
        // When
        String toStr = functionSignature( "org", "myProcedure" )
                .in( "in", Neo5jTypes.NTList( Neo5jTypes.NTString ) )
                .out( Neo5jTypes.NTNumber )
                .build()
                .toString();

        // Then
        assertEquals( "org.myProcedure(in :: LIST? OF STRING?) :: (NUMBER?)", toStr );
    }
}
