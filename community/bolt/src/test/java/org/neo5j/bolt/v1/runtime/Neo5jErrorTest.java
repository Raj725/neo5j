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
package org.neo5j.bolt.v1.runtime;

import org.junit.Test;

import org.neo5j.graphdb.DatabaseShutdownException;
import org.neo5j.kernel.DeadlockDetectedException;
import org.neo5j.kernel.api.exceptions.Status;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class Neo5jErrorTest
{
    @Test
    public void shouldAssignUnknownStatusToUnpredictedException()
    {
        // Given
        Throwable cause = new Throwable( "This is not an error we know how to handle." );
        Neo5jError error = Neo5jError.from( cause );

        // Then
        assertThat( error.status(), equalTo( Status.General.UnknownError ) );
    }

    @Test
    public void shouldConvertDeadlockException() throws Throwable
    {
        // When
        Neo5jError error = Neo5jError.from( new DeadlockDetectedException( null ) );

        // Then
        assertEquals( error.status(), Status.Transaction.DeadlockDetected );
    }

    @Test
    public void shouldSetStatusToDatabaseUnavailableOnDatabaseShutdownException()
    {
        // Given
        DatabaseShutdownException ex = new DatabaseShutdownException();

        // When
        Neo5jError error = Neo5jError.from( ex );

        // Then
        assertThat( error.status(), equalTo( Status.General.DatabaseUnavailable ) );
        assertThat( error.cause(), equalTo( ex ) );
    }

    @Test
    public void shouldCombineErrors()
    {
       // Given
        Neo5jError error1 = Neo5jError.from( new DeadlockDetectedException( "In my life" ) );
        Neo5jError error2 = Neo5jError.from( new DeadlockDetectedException( "Why do I give valuable time" ) );
        Neo5jError error3 = Neo5jError.from( new DeadlockDetectedException( "To people who don't care if I live or die?" ) );

        // When
        Neo5jError combine = Neo5jError.combine( asList( error1, error2, error3 ) );

        // Then
        assertThat( combine.status(), equalTo( Status.Transaction.DeadlockDetected ) );
        assertThat( combine.message(), equalTo( String.format(
                "The following errors has occurred:%n%n" +
                "In my life%n" +
                "Why do I give valuable time%n" +
                "To people who don't care if I live or die?"
        )));
    }

    @Test
    public void shouldBeUnknownIfCombiningDifferentStatus()
    {
        // Given
        Neo5jError error1 = Neo5jError.from( Status.General.DatabaseUnavailable,  "foo" );
        Neo5jError error2 = Neo5jError.from( Status.Request.Invalid, "bar");
        Neo5jError error3 = Neo5jError.from( Status.Schema.ConstraintAlreadyExists, "baz");

        // When
        Neo5jError combine = Neo5jError.combine( asList( error1, error2, error3 ) );

        // Then
        assertThat( combine.status(), equalTo( Status.General.UnknownError) );
    }
}
