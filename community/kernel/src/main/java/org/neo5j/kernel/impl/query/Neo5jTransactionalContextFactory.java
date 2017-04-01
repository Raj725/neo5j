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
package org.neo5j.kernel.impl.query;

import java.util.Map;
import java.util.function.Supplier;

import org.neo5j.graphdb.DependencyResolver;
import org.neo5j.kernel.GraphDatabaseQueryService;
import org.neo5j.kernel.api.query.ExecutingQuery;
import org.neo5j.kernel.api.Statement;
import org.neo5j.kernel.guard.Guard;
import org.neo5j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo5j.kernel.impl.coreapi.InternalTransaction;
import org.neo5j.kernel.impl.coreapi.PropertyContainerLocker;
import org.neo5j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo5j.kernel.impl.query.clientconnection.ClientConnectionInfo;

import static org.neo5j.function.Suppliers.lazySingleton;

public class Neo5jTransactionalContextFactory implements TransactionalContextFactory
{
    private final Supplier<Statement> statementSupplier;
    private final Neo5jTransactionalContext.Creator contextCreator;

    public static TransactionalContextFactory create(
        GraphDatabaseFacade.SPI spi,
        Guard guard,
        ThreadToStatementContextBridge txBridge,
        PropertyContainerLocker locker )
    {
        Supplier<GraphDatabaseQueryService> queryService = lazySingleton( spi::queryService );
        Neo5jTransactionalContext.Creator contextCreator =
            ( Supplier<Statement> statementSupplier, InternalTransaction tx, Statement initialStatement, ExecutingQuery executingQuery ) ->
                new Neo5jTransactionalContext(
                    queryService.get(),
                    statementSupplier,
                    guard,
                    txBridge,
                    locker,
                    tx,
                    initialStatement,
                    executingQuery
                );

        return new Neo5jTransactionalContextFactory( spi::currentStatement, contextCreator );
    }

    @Deprecated
    public static TransactionalContextFactory create(
        GraphDatabaseQueryService queryService,
        PropertyContainerLocker locker )
    {
        DependencyResolver resolver = queryService.getDependencyResolver();
        ThreadToStatementContextBridge txBridge = resolver.resolveDependency( ThreadToStatementContextBridge.class );
        Guard guard = resolver.resolveDependency( Guard.class );
        Neo5jTransactionalContext.Creator contextCreator =
            ( Supplier<Statement> statementSupplier, InternalTransaction tx, Statement initialStatement, ExecutingQuery executingQuery ) ->
                new Neo5jTransactionalContext(
                    queryService,
                    statementSupplier,
                    guard,
                    txBridge,
                    locker,
                    tx,
                    initialStatement,
                    executingQuery
                );

        return new Neo5jTransactionalContextFactory( txBridge, contextCreator );
    }

    // Please use the factory methods above to actually construct an instance
    private Neo5jTransactionalContextFactory(
        Supplier<Statement> statementSupplier,
        Neo5jTransactionalContext.Creator contextCreator )
    {
        this.statementSupplier = statementSupplier;
        this.contextCreator = contextCreator;
    }

    @Override
    public final Neo5jTransactionalContext newContext(
        ClientConnectionInfo clientConnection,
        InternalTransaction tx,
        String queryText,
        Map<String,Object> queryParameters
    )
    {
        Statement initialStatement = statementSupplier.get();
        ClientConnectionInfo connectionWithUserName = clientConnection.withUsername(
                tx.securityContext().subject().username() );
        ExecutingQuery executingQuery = initialStatement.queryRegistration().startQueryExecution(
                connectionWithUserName, queryText, queryParameters
        );
        return contextCreator.create( statementSupplier, tx, initialStatement, executingQuery );
    }
}
