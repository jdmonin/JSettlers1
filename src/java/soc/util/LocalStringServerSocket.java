/**
 * GPL (TODO JM)
 */
package soc.util;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import soc.server.genericServer.StringConnection;

/**
 * 
 * Clients who want to connect, call connectTo and are queued. (Thread.wait is used internally)
 * Server-side calls accept to retrieve them.
 * 
 * @author Jeremy D. Monin <jeremy@nand.net>
 *
 */
public class LocalStringServerSocket implements StringServerSocket
{
    protected static Hashtable allSockets = new Hashtable();
    
    /** Length of queue for accepting new connections; default 100. */
    public static int ACCEPT_QUEUELENGTH = 100; 
    
    /** Server-peer sides of connected clients; Added by accept method */
    protected Vector allConnected;
    
    /** Waiting client connections (client-peer sides); Added by connectClient, removed by accept method */
    protected Vector acceptQueue;

    private String socketName;
    boolean out_setEOF;
    
    private Object sync_out_setEOF;  // For synchronized methods, so we don't sync on "this".
    
    public LocalStringServerSocket(String name)
    {
        socketName = name;
        allConnected = new Vector();
        acceptQueue = new Vector();
        out_setEOF = false;
        sync_out_setEOF = new Object();
        allSockets.put(name, this);
    }
    
    // TODO exception choice, method name choice
    //   Intended to be called by client thread
    // TODO note will block-wait
    public static LocalStringConnection connectTo(String name)
        throws ConnectException
    {
        LocalStringConnection cn = new LocalStringConnection();
        return connectTo (name, cn);
    }

    
    // TODO exception choice, method name choice
    //   Intended to be called by client thread
    // TODO note will block-wait
    public static LocalStringConnection connectTo(String name, LocalStringConnection client)
        throws ConnectException
    {
        if (name == null)
            throw new IllegalArgumentException("name null");
        if (client == null)
            throw new IllegalArgumentException("client null");
        if (client.getPeer() != null)
            throw new IllegalArgumentException("client already peered");
        
        if (! allSockets.containsKey(name))
            throw new ConnectException("LocalStringServerSocket name not found: " + name);
        
        LocalStringServerSocket ss = (LocalStringServerSocket) allSockets.get(name);       
        if (ss.isOutEOF())
            throw new ConnectException("LocalStringServerSocket name is EOF: " + name);

        LocalStringConnection servSidePeer;
        try
        {
            servSidePeer = ss.queueAcceptClient(client);
        }
        catch (Throwable t)
        {
            ConnectException ce = new ConnectException("Error queueing to accept for " + name);
            ce.initCause(t);
            throw ce;
        }
        
        // Since we called queueAcceptClient, that server-side thread may have woken
        // and accepted the connection from this client-side thread already.
        // So, check if we're accepted, before waiting to be accepted.
        //
        if (! servSidePeer.isAccepted())
        {
            try
            {
                synchronized (servSidePeer)
                {
                    servSidePeer.wait();  // Notified by accept method
                }
            }
            catch (InterruptedException e)
            {
                // We'll loop and wait again.
            }
        }
        
        if (client != servSidePeer.getPeer())
            throw new IllegalStateException("Internal error: Peer is wrong");
        
        return client;
    }
    
    /**
     * Queue this client to be accepted, and return their new server-peer;
     * if calling this from methods initiated by the client, check if accepted.
     * If not accepted yet, call Thread.wait on the returned new peer object.
     * Once the server has accepted them, it will call Thread.notify on that object.
     * 
     * @param client Client to connect
     * @return peer Server-side peer of this client
     * @throws IllegalStateException If we are at EOF already
     * @throws IllegalArgumentException If client is or was accepted somewhere already
     * @throws EOFException  If client is at EOF already
     */
    protected LocalStringConnection queueAcceptClient(LocalStringConnection client)
        throws IllegalStateException, IllegalArgumentException, EOFException
    {
        if (isOutEOF())
            throw new IllegalStateException("Internal error, already at EOF");
        if (client.isAccepted())
            throw new IllegalArgumentException("Client is already accepted somewhere");
        if (client.isOutEOF() || client.isInEOF())
            throw new EOFException("client is already at EOF");

        acceptQueue.add(client);  // TODO full?
        
        // Create obj, then notify any server thread waiting to accept clients.
        // Callers thread-wait on the newly created object to prevent possible
        // contention with other objects; we know this new object won't have
        // any locks on it.
        
        LocalStringConnection serverPeer = new LocalStringConnection(client);
        synchronized (acceptQueue)
        {
            acceptQueue.notifyAll();
        }
        
        return serverPeer;
    }
    
    /**
     * For server to call.  Blocks waiting for next inbound connection.
     * (Synchronizes on accept queue.)
     * 
     * @return The server-side peer to the inbound client connection
     * @throws EOFException if our setEOF() has been called, thus
     *    new clients won't receive any data from us
     * @throws IOException if a network problem occurs (Which won't happen with this local communication)
     */
    public StringConnection accept() throws EOFException, IOException
    {
        if (out_setEOF)
            throw new EOFException("Server socket already at EOF");
        
        LocalStringConnection cliPeer;

        synchronized (acceptQueue)
        {
            while (acceptQueue.isEmpty())
            {
                if (out_setEOF)
                    throw new EOFException();
                else
                {
                    try
                    {
                        acceptQueue.wait();  // Notified by queueAcceptClient 
                    }
                    catch (InterruptedException e) {}
                }
            }
            cliPeer = (LocalStringConnection) acceptQueue.elementAt(0);
            acceptQueue.removeElementAt(0);            
        }
        
        LocalStringConnection servPeer = cliPeer.getPeer();
        cliPeer.setAccepted();
        servPeer.setAccepted();
        allConnected.addElement(servPeer);
        
        synchronized (servPeer)
        {
            servPeer.notifyAll();  // Client has been waiting in connectTo for our accept
        }
        return servPeer;
    }
    
    /**
     * 
     * @return Server-peer sides of all currently connected clients (LocalStringConnections)
     */
    public Enumeration allClients()
    {
        return allConnected.elements();
    }
    
    /**
     * Send to all connected clients.
     * 
     * @param msg String to send
     *  
     * @see #allClients()
     */
    public void broadcast(String msg)
    {
        synchronized (allConnected)
        {
            for (int i = allConnected.size() - 1; i >= 0; --i)
            {
                LocalStringConnection c = (LocalStringConnection) allConnected.elementAt(i);
                c.put(msg);
            }
        }
    }
    
    /** 
     * If our server won't receive any more data from the client, disconnect them.
     * Considered EOF if the client's server-side peer connection inbound EOF is set.
     * Removes from allConnected and set outbound EOF flag on that connection.
     */
    public void disconnectEOFClients()
    {
        LocalStringConnection servPeer;

        synchronized (allConnected)
        {
            for (int i = allConnected.size() - 1; i >= 0; --i)
            {
                servPeer = (LocalStringConnection) allConnected.elementAt(i);
                if (servPeer.isInEOF())
                {
                    allConnected.removeElementAt(i);
                    servPeer.setEOF();
                }
            }
        }        
    }
    
    /**
     * @return Returns the socketName.
     */
    public String getSocketName()
    {
        return socketName;
    }

    /**
     * @param socketName The socketName to set.
     */
    public void setSocketName(String socketName)
    {
        this.socketName = socketName;
    }

    public void setEOF()
    {
        // TODO close method, let the remote-end know we're closing       
        synchronized (sync_out_setEOF)
        {
            out_setEOF = true;
        }
    }

    public boolean isOutEOF()
    {
        synchronized (sync_out_setEOF)
        {
            return out_setEOF;
        }
    }
    
    public void close() throws IOException
    {
        // TODO current connections?
        // TODO javadoc serversocket for when throw IOException
        setEOF();
    }

}
