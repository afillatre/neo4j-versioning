/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
package org.neo4j.support.versioning;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.Config;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A database meant to be used in unit tests. It will always be empty on start.
 */
public class ImpermanentGraphDatabase extends AbstractGraphDatabase
{
    private EmbeddedGraphDatabase inner;
    private String storeDir;

    public ImpermanentGraphDatabase( String storeDir, Map<String, String> params )
    {
        this.storeDir = storeDir;
        deleteRecursively( new File( storeDir ) );
        inner = new EmbeddedGraphDatabase( storeDir, params );
    }

    public ImpermanentGraphDatabase( Map<String, String> params ) throws IOException
    {
        this( createTempDir(), params );
    }

    public ImpermanentGraphDatabase() throws IOException
    {
        this( createTempDir(), new HashMap<String, String>() );
    }

    public ImpermanentGraphDatabase( String storeDir )
    {
        this( storeDir, new HashMap<String, String>() );
    }


    private static String createTempDir() throws IOException
    {

        File d = File.createTempFile( "neo4j-test", "dir" );
        if ( !d.delete() )
        {
            throw new RuntimeException( "temp config directory pre-delete failed" );
        }
        if ( !d.mkdirs() )
        {
            throw new RuntimeException( "temp config directory not created" );
        }
        d.deleteOnExit();
        return d.getAbsolutePath();
    }

    private static void deleteRecursively( File file )
    {
        if ( !file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            for ( File child : file.listFiles() )
            {
                deleteRecursively( child );
            }
        }
        if ( !file.delete() )
        {
            throw new RuntimeException( "Couldn't empty database. Offending file:" + file );
        }
    }

    public Node createNode()
    {
        return inner.createNode();
    }

    public Node getNodeById( long id )
    {
        return inner.getNodeById( id );
    }

    public Relationship getRelationshipById( long id )
    {
        return inner.getRelationshipById( id );
    }

    public Node getReferenceNode()
    {
        return inner.getReferenceNode();
    }

    public Iterable<Node> getAllNodes()
    {
        return inner.getAllNodes();
    }

    public Iterable<RelationshipType> getRelationshipTypes()
    {
        return inner.getRelationshipTypes();
    }

    public void shutdown()
    {
        shutdown( true );
    }

    public void shutdown( boolean delete )
    {
        inner.shutdown();
        if ( delete )
        {
            deleteRecursively( new File( storeDir ) );
        }
        else
        {
            System.out.println("Store retained at: " + storeDir);
        }
    }

    public Transaction beginTx()
    {
        return inner.beginTx();
    }

    public <T> TransactionEventHandler<T> registerTransactionEventHandler(
        TransactionEventHandler<T> handler )
    {
        return inner.registerTransactionEventHandler( handler );
    }

    public <T> TransactionEventHandler<T> unregisterTransactionEventHandler(
        TransactionEventHandler<T> handler )
    {
        return inner.unregisterTransactionEventHandler( handler );
    }

    public KernelEventHandler registerKernelEventHandler(
        KernelEventHandler handler )
    {
        return inner.registerKernelEventHandler( handler );
    }

    public KernelEventHandler unregisterKernelEventHandler(
        KernelEventHandler handler )
    {
        return inner.unregisterKernelEventHandler( handler );
    }

    public IndexManager index()
    {
        return inner.index();
    }

    @Override
    public String getStoreDir()
    {
        return inner.getStoreDir();
    }

    @Override
    public Config getConfig()
    {
        return inner.getConfig();
    }

    @Override
    public <T> T getManagementBean( Class<T> type )
    {
        return inner.getManagementBean( type );
    }

    @Override
    public boolean isReadOnly()
    {
        return inner.isReadOnly();
    }
}
