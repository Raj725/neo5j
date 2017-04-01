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
package org.neo5j.shell.kernel.apps;

import java.rmi.RemoteException;

import org.neo5j.graphdb.Direction;
import org.neo5j.graphdb.Node;
import org.neo5j.graphdb.Relationship;
import org.neo5j.graphdb.RelationshipType;
import org.neo5j.helpers.Service;
import org.neo5j.shell.App;
import org.neo5j.shell.AppCommandParser;
import org.neo5j.shell.Continuation;
import org.neo5j.shell.OptionDefinition;
import org.neo5j.shell.OptionValueType;
import org.neo5j.shell.Output;
import org.neo5j.shell.Session;
import org.neo5j.shell.ShellException;

/**
 * Mimics the POSIX application "mkdir", but neo5j has relationships instead of
 * directories (if you look at Neo5j in a certain perspective).
 */
@Service.Implementation( App.class )
public class Mkrel extends TransactionProvidingApp
{
    public static final String KEY_LAST_CREATED_NODE = "LAST_CREATED_NODE";
    public static final String KEY_LAST_CREATED_RELATIONSHIP = "LAST_CREATED_RELATIONSHIP";

    /**
     * Constructs a new application which can create relationships and nodes
     * in Neo5j.
     */
    public Mkrel()
    {
        this.addOptionDefinition( "t", new OptionDefinition( OptionValueType.MUST,
            "The relationship type" ) );
        this.addOptionDefinition( "d", new OptionDefinition( OptionValueType.MUST,
            "The direction: " + this.directionAlternatives() + "." ) );
        this.addOptionDefinition( "c", new OptionDefinition( OptionValueType.NONE,
            "Supplied if there should be created a new node" ) );
        this.addOptionDefinition( "v", new OptionDefinition( OptionValueType.NONE,
            "Verbose mode: display created nodes/relationships" ) );
        this.addOptionDefinition( "np", new OptionDefinition( OptionValueType.MUST,
            "Properties (a json map) to set for the new node (if one is created)" ) );
        addOptionDefinition( "l", new OptionDefinition( OptionValueType.MUST,
            "Labels to attach to the created node (if one is created), either a single label or a JSON array" ) );
        this.addOptionDefinition( "rp", new OptionDefinition( OptionValueType.MUST,
            "Properties (a json map) to set for the new relationship" ) );
        this.addOptionDefinition( "cd", new OptionDefinition( OptionValueType.NONE,
            "Go to the created node, like doing 'cd'" ) );
    }

    @Override
    public String getDescription()
    {
        return "Creates a relationship to a new or existing node, f.ex:\n" +
                "mkrel -ct KNOWS (will create a relationship to a new node)\n" +
                "mkrel -t KNOWS 123 (will create a relationship to node with id 123)";
    }

    @Override
    protected Continuation exec( AppCommandParser parser, Session session, Output out )
        throws ShellException, RemoteException
    {
        assertCurrentIsNode( session );

        boolean createNode = parser.options().containsKey( "c" );
        boolean suppliedNode = !parser.arguments().isEmpty();
        Node node = null;
        if ( createNode )
        {
            node = getServer().getDb().createNode( parseLabels( parser ) );
            session.set( KEY_LAST_CREATED_NODE, "" + node.getId() );
            setProperties( node, parser.options().get( "np" ) );
        }
        else if ( suppliedNode )
        {
            node = getNodeById( Long.parseLong( parser.arguments().get( 0 ) ) );
        }
        else
        {
            throw new ShellException( "Must either create node (-c)"
                + " or supply node id as the first argument" );
        }

        if ( parser.options().get( "t" ) == null )
        {
            throw new ShellException( "Must supply relationship type "
                + "(-t <relationship-type-name>)" );
        }
        RelationshipType type = getRelationshipType( parser.options().get( "t" ) );
        Direction direction = getDirection( parser.options().get( "d" ) );
        NodeOrRelationship current = getCurrent( session );
        Node currentNode = current.asNode();
        Node startNode = direction == Direction.OUTGOING ? currentNode : node;
        Node endNode = direction == Direction.OUTGOING ? node : currentNode;
        Relationship relationship =
            startNode.createRelationshipTo( endNode, type );
        setProperties( relationship, parser.options().get( "rp" ) );
        session.set( KEY_LAST_CREATED_RELATIONSHIP, relationship.getId() );
        boolean verbose = parser.options().containsKey( "v" );
        if ( createNode && verbose )
        {
            out.println( "Node " + getDisplayName(
                getServer(), session, node, false ) + " created" );
        }
        if ( verbose )
        {
            out.println( "Relationship " + getDisplayName(
                getServer(), session, relationship, true, false ) +
                " created" );
        }

        if ( parser.options().containsKey( "cd" ) )
        {
            cdTo( session, node );
        }
        return Continuation.INPUT_COMPLETE;
    }
}
