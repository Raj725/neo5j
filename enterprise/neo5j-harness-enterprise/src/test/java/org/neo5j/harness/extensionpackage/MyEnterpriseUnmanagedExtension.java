/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.harness.extensionpackage;

import org.eclipse.jetty.http.HttpStatus;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.neo5j.graphdb.GraphDatabaseService;
import org.neo5j.graphdb.Result;
import org.neo5j.graphdb.Transaction;

@Path( "myExtension" )
public class MyEnterpriseUnmanagedExtension
{
    private final GraphDatabaseService db;

    public MyEnterpriseUnmanagedExtension( @Context GraphDatabaseService db )
    {
        this.db = db;
    }

    @GET
    @Path( "doSomething" )
    public Response doSomething()
    {
        return Response.status( 234 ).build();
    }

    @GET
    @Path( "createConstraint" )
    public Response createProperty()
    {
        try ( Transaction tx = db.beginTx() )
        {
            try ( Result result = db.execute( "CREATE CONSTRAINT ON (user:User) ASSERT exists(user.name)" ) )
            {
                // nothing to-do
            }
            tx.success();
            return Response.status( HttpStatus.CREATED_201 ).build();
        }
        catch ( Exception e )
        {
            return Response.status( HttpStatus.NOT_IMPLEMENTED_501 ).build();
        }
    }
}
