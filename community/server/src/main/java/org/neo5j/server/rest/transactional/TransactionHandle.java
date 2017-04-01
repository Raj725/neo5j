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
package org.neo5j.server.rest.transactional;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.neo5j.cypher.CypherException;
import org.neo5j.cypher.InvalidSemanticsException;
import org.neo5j.graphdb.Result;
import org.neo5j.graphdb.security.AuthorizationViolationException;
import org.neo5j.graphdb.security.WriteOperationsNotAllowedException;
import org.neo5j.kernel.DeadlockDetectedException;
import org.neo5j.kernel.GraphDatabaseQueryService;
import org.neo5j.kernel.api.KernelTransaction.Type;
import org.neo5j.kernel.api.exceptions.KernelException;
import org.neo5j.kernel.api.exceptions.Status;
import org.neo5j.kernel.api.exceptions.TransactionFailureException;
import org.neo5j.kernel.api.security.SecurityContext;
import org.neo5j.kernel.impl.query.QueryExecutionEngine;
import org.neo5j.kernel.impl.query.QueryExecutionKernelException;
import org.neo5j.kernel.impl.query.TransactionalContext;
import org.neo5j.logging.Log;
import org.neo5j.logging.LogProvider;
import org.neo5j.server.rest.transactional.error.InternalBeginTransactionError;
import org.neo5j.server.rest.transactional.error.Neo5jError;
import org.neo5j.server.rest.web.TransactionUriScheme;

import static org.neo5j.helpers.collection.Iterators.addToCollection;

/**
 * Encapsulates executing statements in a transaction, committing the transaction, or rolling it back.
 *
 * Constructing a {@link TransactionHandle} does not immediately ask the kernel to create a
 * {@link org.neo5j.kernel.api.KernelTransaction}; instead a {@link org.neo5j.kernel.api.KernelTransaction} is
 * only created when the first statements need to be executed.
 *
 * At the end of each statement-executing method, the {@link org.neo5j.kernel.api.KernelTransaction} is either
 * suspended (ready to be resumed by a later operation), or committed, or rolled back.
 *
 * If you acquire instances of this class from {@link TransactionHandleRegistry}, it will prevent concurrent access to
 * the same instance. Therefore the implementation assumes that a single instance will only be accessed from
 * a single thread.
 *
 * All of the public methods on this class are "single-shot"; once you have called one method, the handle returns
 * itself
 * to the registry. If you want to use it again, you'll need to acquire it back from the registry to ensure exclusive
 * use.
 */
public class TransactionHandle implements TransactionTerminationHandle
{
    private final TransitionalPeriodTransactionMessContainer txManagerFacade;
    private final QueryExecutionEngine engine;
    private final TransactionRegistry registry;
    private final TransactionUriScheme uriScheme;
    private final Type type;
    private final SecurityContext securityContext;
    private long customTransactionTimeout;
    private final Log log;
    private final long id;
    private TransitionalTxManagementKernelTransaction context;
    private GraphDatabaseQueryService queryService;

    TransactionHandle( TransitionalPeriodTransactionMessContainer txManagerFacade, QueryExecutionEngine engine,
            GraphDatabaseQueryService queryService, TransactionRegistry registry, TransactionUriScheme uriScheme,
            boolean implicitTransaction, SecurityContext securityContext, long customTransactionTimeout,
            LogProvider logProvider )
    {
        this.txManagerFacade = txManagerFacade;
        this.engine = engine;
        this.queryService = queryService;
        this.registry = registry;
        this.uriScheme = uriScheme;
        this.type = implicitTransaction ? Type.implicit : Type.explicit;
        this.securityContext = securityContext;
        this.customTransactionTimeout = customTransactionTimeout;
        this.log = logProvider.getLog( getClass() );
        this.id = registry.begin( this );
    }

    public URI uri()
    {
        return uriScheme.txUri( id );
    }

    public boolean isImplicit()
    {
        return type == Type.implicit;
    }

    public void execute( StatementDeserializer statements, ExecutionResultSerializer output,
            HttpServletRequest request )
    {
        List<Neo5jError> errors = new LinkedList<>();
        try
        {
            output.transactionCommitUri( uriScheme.txCommitUri( id ) );
            ensureActiveTransaction();
            execute( statements, output, errors, request );
        }
        catch ( InternalBeginTransactionError e )
        {
            errors.add( e.toNeo5jError() );
        }
        finally
        {
            output.errors( errors );
            output.finish();
        }
    }

    @Override
    public boolean terminate()
    {
        if ( context != null )
        {
            context.terminate();
        }
        return true;
    }

    public void commit( StatementDeserializer statements, ExecutionResultSerializer output, HttpServletRequest request )
    {
        List<Neo5jError> errors = new LinkedList<>();
        try
        {
            try
            {
                Statement peek = statements.peek();
                if ( isImplicit() && peek == null ) /* JSON parse error */
                {
                    addToCollection( statements.errors(), errors );
                }
                else
                {
                    ensureActiveTransaction();
                    executeStatements( statements, output, errors, request );
                    closeContextAndCollectErrors( errors );
                }
            }
            finally
            {
                registry.forget( id );
            }
        }
        catch ( InternalBeginTransactionError e )
        {
            errors.add( e.toNeo5jError() );
        }
        catch ( CypherException e )
        {
            errors.add( new Neo5jError( e.status(), e ) );
            throw e;
        }
        finally
        {
            output.errors( errors );
            output.finish();
        }
    }

    public void rollback( ExecutionResultSerializer output )
    {
        List<Neo5jError> errors = new LinkedList<>();
        try
        {
            ensureActiveTransaction();
            rollback( errors );
        }
        catch ( InternalBeginTransactionError e )
        {
            errors.add( e.toNeo5jError() );
        }
        finally
        {
            output.errors( errors );
            output.finish();
        }
    }

    void forceRollback() throws TransactionFailureException
    {
        context.resumeSinceTransactionsAreStillThreadBound();
        context.rollback();
    }

    private void ensureActiveTransaction() throws InternalBeginTransactionError
    {
        if ( context == null )
        {
            try
            {
                context = txManagerFacade.newTransaction( type, securityContext, customTransactionTimeout );
            }
            catch ( RuntimeException e )
            {
                log.error( "Failed to start transaction.", e );
                throw new InternalBeginTransactionError( e );
            }
        }
        else
        {
            context.resumeSinceTransactionsAreStillThreadBound();
        }
    }

    private void execute( StatementDeserializer statements, ExecutionResultSerializer output,
            List<Neo5jError> errors, HttpServletRequest request )
    {
        executeStatements( statements, output, errors, request );

        if ( Neo5jError.shouldRollBackOn( errors ) )
        {
            rollback( errors );
        }
        else
        {
            context.suspendSinceTransactionsAreStillThreadBound();
            long lastActiveTimestamp = registry.release( id, this );
            output.transactionStatus( lastActiveTimestamp );
        }
    }

    private void closeContextAndCollectErrors( List<Neo5jError> errors )
    {
        if ( errors.isEmpty() )
        {
            try
            {
                context.commit();
            }
            catch ( Exception e )
            {
                if ( e.getCause() instanceof Status.HasStatus )
                {
                    errors.add( new Neo5jError( ((Status.HasStatus) e.getCause()).status(), e ) );
                }
                else
                {
                    log.error( "Failed to commit transaction.", e );
                    errors.add( new Neo5jError( Status.Transaction.TransactionCommitFailed, e ) );
                }
            }
        }
        else
        {
            try
            {
                context.rollback();
            }
            catch ( Exception e )
            {
                log.error( "Failed to rollback transaction.", e );
                errors.add( new Neo5jError( Status.Transaction.TransactionRollbackFailed, e ) );
            }
        }
    }

    private void rollback( List<Neo5jError> errors )
    {
        try
        {
            context.rollback();
        }
        catch ( Exception e )
        {
            log.error( "Failed to rollback transaction.", e );
            errors.add( new Neo5jError( Status.Transaction.TransactionRollbackFailed, e ) );
        }
        finally
        {
            registry.forget( id );
        }
    }

    private void executeStatements( StatementDeserializer statements, ExecutionResultSerializer output,
            List<Neo5jError> errors, HttpServletRequest request )
    {
        try
        {
            boolean hasPrevious = false;
            while ( statements.hasNext() )
            {
                Statement statement = statements.next();
                try
                {
                    boolean hasPeriodicCommit = engine.isPeriodicCommit( statement.statement() );
                    if ( (statements.hasNext() || hasPrevious) && hasPeriodicCommit )
                    {
                        throw new QueryExecutionKernelException(
                                new InvalidSemanticsException( "Cannot execute another statement after executing " +
                                                               "PERIODIC COMMIT statement in the same transaction" ) );
                    }

                    if ( !hasPrevious && hasPeriodicCommit )
                    {
                        context.closeTransactionForPeriodicCommit();
                    }

                    hasPrevious = true;
                    TransactionalContext tc = txManagerFacade.create( request, queryService, type, securityContext,
                            statement.statement(), statement.parameters() );
                    Result result = safelyExecute( statement, hasPeriodicCommit, tc );
                    output.statementResult( result, statement.includeStats(), statement.resultDataContents() );
                    output.notifications( result.getNotifications() );
                }
                catch ( KernelException | CypherException | AuthorizationViolationException |
                        WriteOperationsNotAllowedException e )
                {
                    errors.add( new Neo5jError( e.status(), e ) );
                    break;
                }
                catch ( DeadlockDetectedException e )
                {
                    errors.add( new Neo5jError( Status.Transaction.DeadlockDetected, e ) );
                }
                catch ( IOException e )
                {
                    errors.add( new Neo5jError( Status.Network.CommunicationError, e ) );
                    break;
                }
                catch ( Exception e )
                {
                    Throwable cause = e.getCause();
                    if ( cause instanceof Status.HasStatus )
                    {
                        errors.add( new Neo5jError( ((Status.HasStatus) cause).status(), cause ) );
                    }
                    else
                    {
                        errors.add( new Neo5jError( Status.Statement.ExecutionFailed, e ) );
                    }

                    break;
                }
            }

            addToCollection( statements.errors(), errors );
        }
        catch ( Throwable e )
        {
            errors.add( new Neo5jError( Status.General.UnknownError, e ) );
        }
    }

    private Result safelyExecute( Statement statement, boolean hasPeriodicCommit, TransactionalContext tc )
            throws QueryExecutionKernelException
    {
        try
        {
            return engine.executeQuery( statement.statement(), statement.parameters(), tc );
        }
        finally
        {
            if ( hasPeriodicCommit )
            {
                context.reopenAfterPeriodicCommit();
            }
        }
    }
}
