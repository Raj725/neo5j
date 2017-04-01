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
package org.neo5j.cypher.internal.javacompat;

import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.neo5j.cypher.internal.CommunityCompatibilityFactory;
import org.neo5j.cypher.javacompat.internal.GraphDatabaseCypherService;
import org.neo5j.graphdb.Result;
import org.neo5j.kernel.GraphDatabaseQueryService;
import org.neo5j.kernel.api.KernelAPI;
import org.neo5j.kernel.api.KernelTransaction;
import org.neo5j.kernel.api.security.SecurityContext;
import org.neo5j.kernel.impl.coreapi.InternalTransaction;
import org.neo5j.kernel.impl.coreapi.PropertyContainerLocker;
import org.neo5j.kernel.impl.query.Neo5jTransactionalContextFactory;
import org.neo5j.kernel.impl.query.TransactionalContext;
import org.neo5j.kernel.impl.query.TransactionalContextFactory;
import org.neo5j.kernel.impl.query.clientconnection.ClientConnectionInfo;
import org.neo5j.kernel.monitoring.Monitors;
import org.neo5j.logging.NullLogProvider;
import org.neo5j.test.rule.DatabaseRule;
import org.neo5j.test.rule.ImpermanentDatabaseRule;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ExecutionEngineTest
{
    private static final Map<String,Object> NO_PARAMS = Collections.emptyMap();

    @Rule
    public DatabaseRule database = new ImpermanentDatabaseRule();

    @Test
    public void shouldConvertListsAndMapsWhenPassingFromScalaToJava() throws Exception
    {
        GraphDatabaseQueryService graph = new GraphDatabaseCypherService( this.database.getGraphDatabaseAPI() );
        KernelAPI kernelAPI = graph.getDependencyResolver().resolveDependency( KernelAPI.class );
        Monitors monitors = graph.getDependencyResolver().resolveDependency( Monitors.class );

        NullLogProvider nullLogProvider = NullLogProvider.getInstance();
        CommunityCompatibilityFactory compatibilityFactory =
                new CommunityCompatibilityFactory( graph, kernelAPI, monitors, nullLogProvider );
        ExecutionEngine executionEngine = new ExecutionEngine( graph, nullLogProvider, compatibilityFactory );

        Result result;
        try ( InternalTransaction tx = graph
                .beginTransaction( KernelTransaction.Type.implicit, SecurityContext.AUTH_DISABLED ) )
        {
            String query = "RETURN { key : 'Value' , collectionKey: [{ inner: 'Map1' }, { inner: 'Map2' }]}";
            TransactionalContext tc = createTransactionContext( graph, tx, query );
            result = executionEngine.executeQuery( query, NO_PARAMS, tc );
            tx.success();
        }

        Map firstRowValue = (Map) result.next().values().iterator().next();
        assertThat( firstRowValue.get( "key" ), is( "Value" ) );
        List theList = (List) firstRowValue.get( "collectionKey" );
        assertThat( ((Map) theList.get( 0 )).get( "inner" ), is( "Map1" ) );
        assertThat( ((Map) theList.get( 1 )).get( "inner" ), is( "Map2" ) );
    }

    private TransactionalContext createTransactionContext( GraphDatabaseQueryService graph, InternalTransaction tx,
            String query )
    {
        PropertyContainerLocker locker = new PropertyContainerLocker();
        TransactionalContextFactory contextFactory = Neo5jTransactionalContextFactory.create( graph, locker );
        return contextFactory.newContext( ClientConnectionInfo.EMBEDDED_CONNECTION, tx, query, Collections.emptyMap() );
    }
}
