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
package org.neo5j.server.rest.management.console;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;

import org.neo5j.cypher.SyntaxException;
import org.neo5j.cypher.internal.javacompat.ExecutionEngine;
import org.neo5j.graphdb.Result;
import org.neo5j.helpers.collection.Pair;
import org.neo5j.kernel.impl.query.TransactionalContext;
import org.neo5j.logging.Log;
import org.neo5j.logging.LogProvider;
import org.neo5j.server.database.CypherExecutor;

import static java.util.Collections.emptyMap;

public class CypherSession implements ScriptSession
{
    private final CypherExecutor cypherExecutor;
    private final Log log;
    private final HttpServletRequest request;

    public CypherSession( CypherExecutor cypherExecutor, LogProvider logProvider, HttpServletRequest request )
    {
        this.cypherExecutor = cypherExecutor;
        this.log = logProvider.getLog( getClass() );
        this.request = request;
    }

    @Override
    public Pair<String, String> evaluate( String script )
    {
        if ( StringUtils.EMPTY.equals( script.trim() ) )
        {
            return Pair.of( StringUtils.EMPTY, null );
        }

        String resultString;
        try
        {
            TransactionalContext tc = cypherExecutor.createTransactionContext( script, emptyMap(), request );
            ExecutionEngine engine = cypherExecutor.getExecutionEngine();
            Result result = engine.executeQuery( script, emptyMap(), tc );
            resultString = result.resultAsString();
        }
        catch ( SyntaxException error )
        {
            resultString = error.getMessage();
        }
        catch ( Exception exception )
        {
            log.error( "Unknown error executing cypher query", exception );
            resultString = "Error: " + exception.getClass().getSimpleName() + " - " + exception.getMessage();
        }
        return Pair.of( resultString, null );
    }
}
