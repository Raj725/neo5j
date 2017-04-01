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
package org.neo5j.harness;

import org.codehaus.jackson.JsonNode;
import org.junit.Rule;
import org.junit.Test;

import org.neo5j.procedure.UserAggregationFunction;
import org.neo5j.procedure.UserAggregationResult;
import org.neo5j.procedure.UserAggregationUpdate;
import org.neo5j.test.rule.SuppressOutput;
import org.neo5j.test.rule.TestDirectory;
import org.neo5j.test.server.HTTP;

import static org.junit.Assert.assertEquals;
import static org.neo5j.test.server.HTTP.RawPayload.quotedJson;

public class JavaAggregationFunctionsTest
{
    @Rule
    public TestDirectory testDir = TestDirectory.testDirectory();

    @Rule
    public SuppressOutput suppressOutput = SuppressOutput.suppressAll();

    public static class MyFunctions
    {
        @UserAggregationFunction
        public EliteAggregator myFunc()
        {
            return new EliteAggregator();
        }

        @UserAggregationFunction
        public EliteAggregator funcThatThrows()
        {
            throw new RuntimeException( "This is an exception" );
        }

    }

    public static class EliteAggregator
    {
        @UserAggregationUpdate
        public void update()
        {
        }

        @UserAggregationResult
        public long result()
        {
            return 1337L;
        }
    }

    @Test
    public void shouldLaunchWithDeclaredFunctions() throws Exception
    {
        // When
        try ( ServerControls server = TestServerBuilders.newInProcessBuilder().withAggregationFunction( MyFunctions.class )
                .newServer() )
        {
            // Then
            HTTP.Response response = HTTP.POST( server.httpURI().resolve( "db/data/transaction/commit" ).toString(),
                    quotedJson(
                            "{ 'statements': [ { 'statement': 'RETURN org.neo5j.harness.myFunc() AS someNumber' } ] " +
                            "}" ) );

            JsonNode result = response.get( "results" ).get( 0 );
            assertEquals( "someNumber", result.get( "columns" ).get( 0 ).asText() );
            assertEquals( 1337, result.get( "data" ).get( 0 ).get( "row" ).get( 0 ).asInt() );
            assertEquals( "[]", response.get( "errors" ).toString() );
        }
    }

    @Test
    public void shouldGetHelpfulErrorOnProcedureThrowsException() throws Exception
    {
        // When
        try ( ServerControls server = TestServerBuilders.newInProcessBuilder().withAggregationFunction( MyFunctions.class )
                .newServer() )
        {
            // Then
            HTTP.Response response = HTTP.POST( server.httpURI().resolve( "db/data/transaction/commit" ).toString(),
                    quotedJson(
                            "{ 'statements': [ { 'statement': 'RETURN org.neo5j.harness.funcThatThrows()' } ] }" ) );

            String error = response.get( "errors" ).get( 0 ).get( "message" ).asText();
            assertEquals(
                    "Failed to invoke function `org.neo5j.harness.funcThatThrows`: Caused by: java.lang" +
                    ".RuntimeException: This is an exception",
                    error );
        }
    }
}
