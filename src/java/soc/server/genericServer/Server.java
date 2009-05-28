/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * Portions of this file Copyright (C) 2007-2009 Jeremy D. Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The author of this program can be reached at thomas@infolab.northwestern.edu
 **/
package soc.server.genericServer;

import soc.debug.D; // JM

import java.io.IOException;
import java.io.Serializable;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.Vector;


/** a general purpose server.
 *<P>
 *  This is the real stuff. Server subclasses won't have to care about
 *  reading/writing on the net, data consistency among threads, etc.
 *<P>
 *  Newly connecting clients arrive in {@link #run()},
 *  start a thread for the server side of their Connection or LocalStringConnection,
 *  and are integrated into server data via {@link #addConnection(StringConnection)}
 *  called from that thread.
 *<P>
 *  The first processed message over the connection will be from the server to the client,
 *  in {@link #newConnection1(StringConnection)} or {@link #newConnection2(StringConnection)}.
 *  You can send to client, but can't yet receive messages from them,
 *  until after newConnection2 returns.
 *  The client should ideally be named and versioned in newConnection1, but this
 *  can also be done later.
 *<P>
 *  @version 1.5
 *  @author Original author: <A HREF="http://www.nada.kth.se/~cristi">Cristian Bogdan</A> <br>
 *  Lots of mods by Robert S. Thomas and Jay Budzik <br>
 *  Local (StringConnection) network system by Jeremy D Monin <jeremy@nand.net> <br>
 *  Version-tracking system by Jeremy D Monin <jeremy@nand.net>
 */
public abstract class Server extends Thread implements Serializable, Cloneable
{
    StringServerSocket ss;
    boolean up = false;
    protected Exception error = null;
    protected int port;  // -1 for local mode (LocalStringServerSocket, etc)
    protected String strSocketName;  // null for network mode

    /**
     * total number of connections made
     */
    protected int numberOfConnections;

    /**
     * total number of current connections
     * @since 1.1.06
     */
    protected int numberCurrentConnections;

    /** the named connections */
    protected Hashtable conns = new Hashtable();

    /** the newly connected, unnamed connections.
     *  Adding/removing/naming connections synchronizes on this Vector.
     */
    protected Vector unnamedConns = new Vector();

    /** clients in process of connecting */
    public Vector inQueue = new Vector();

    /**
     * Versions of currently connected clients, according to
     * {@link StringConnection#getVersion()}.
     * Key = Integer(version). Value = ConnVersionCounter.
     * Synchronized on {@link #unnamedConns}, like many other
     * client-related structures.
     * @see #clientVersionAdd()
     * @see #clientVersionRem()
     * @since 1.1.06
     */
    private TreeMap cliVersionsConnected;

    /**
     * Minimum and maximum client version currently connected.
     * Meaningless if {@linkplain #numberOfConnections} is 0.
     * @see #cliVersionsConnected
     * @since 1.1.06
     */
    private int cliVersionMin, cliVersionMax;

    /** start listening to the given port */
    public Server(int port)
    {
        this.port = port;
        this.strSocketName = null;
        numberOfConnections = 0;
        cliVersionsConnected = new TreeMap();

        try
        {
            ss = new NetStringServerSocket(port, this);
        }
        catch (IOException e)
        {
            System.err.println("Could not listen to port " + port + ": " + e);
            error = e;
        }

        setName("server-" + port);  // Thread name for debugging
    }

    /** start listening to the given local string port (practice game) */
    public Server(String stringSocketName)
    {
        if (stringSocketName == null)
            throw new IllegalArgumentException("stringSocketName null");

        this.port = -1;
        this.strSocketName = stringSocketName;
        numberOfConnections = 0;
        cliVersionsConnected = new TreeMap();

        ss = new LocalStringServerSocket(stringSocketName);
        setName("server-localstring-" + stringSocketName);  // Thread name for debugging
    }

    /**
     * Given a key data, return the connected client.
     * @param connKey Object key data, as in {@link StringConnection#getData()}; if null, returns null
     * @return The connection with this key, or null if none
     */
    protected StringConnection getConnection(Object connKey)
    {
        if (connKey != null)
            return (StringConnection) conns.get(connKey);
        else
            return null;
    }

    /**
     * @return the list of named connections: StringConnections where {@link StringConnection#getData()}
     *         is not null
     */
    protected Enumeration getConnections()
    {
        return conns.elements();
    }

    /**
     * @return the count of named connections: StringConnections where {@link StringConnection#getData()}
     *         is not null
     */
    protected synchronized int connectionCount()
    {
        return conns.size();
    }

    public synchronized boolean isUp()
    {
        return up;
    }

    /** run method for Server */
    public void run()
    {
        Treater treater = new Treater(this);

        if (error != null)
        {
            return;
        }

        up = true;
        
        treater.start();  // Set "up" before starting treater (race condition)

        while (isUp())
        {
            try
            {
                while (isUp())
                {
                    // we could limit the number of accepted connections here
                    StringConnection con = ss.accept();
                    if (port != -1)
                    {
                        ((Connection) con).start();
                    }
                    else
                    {
                        ((LocalStringConnection) con).setServer(this);
                        new Thread((LocalStringConnection) con).start();
                    }

                    //addConnection(new StringConnection());
                }
            }
            catch (IOException e)
            {
                error = e;
                D.ebugPrintln("Exception " + e + " during accept");

                //System.out.println("STOPPING SERVER");
                //stopServer();
            }

            try
            {
                ss.close();
                if (strSocketName == null)
                    ss = new NetStringServerSocket(port, this);
                else
                    ss = new LocalStringServerSocket(strSocketName);
            }
            catch (IOException e)
            {
                System.err.println("Could not listen to port " + port + ": " + e);
                up = false;
                error = e;
            }
        }
    }

    /** treat a request from the given connection */
    public void treat(String s, StringConnection c)
    {
        // D.ebugPrintln("IN got: " + s);
        synchronized (inQueue)
        {
            inQueue.addElement(new Command(s, c));
            inQueue.notify();
        }
    }

    /**
     * Remove a queued incoming message from a client, and treat it.
     * Called from the single 'treater' thread.
     * <em>Do not block or sleep</em> because this is single-threaded.
     *
     * @param str Contents of message from the client
     * @param con Connection (client) sending this message
     */
    abstract public void processCommand(String str, StringConnection con);

    /**
     * Callback to process the client's first message command specially.
     * This default implementation does nothing and returns false;
     * override it in your app if needed.
     *
     * @param str Contents of first message from the client
     * @param con Connection (client) sending this message
     * @return true if processed here, false if this message should be
     *         queued up and processed by the normal {@link #processCommand(String, StringConnection)}.
     */
    public boolean processFirstCommand(String str, StringConnection con)
    {
	return false;
    }

    /** placeholder for doing things when server gets down */
    protected void serverDown() {}

    /**
     * placeholder for doing things when a new connection comes, part 1 -
     * decide whether to accept.
     * Unless you override this method, always returns true.
     *<P>
     * If the connection is accepted, it's added to a list ({@link #unnamedConns}
     * or {@link #conns}), and also added to the version collection.
     *<P>
     * This method is called within a per-client thread.
     * You can send to client, but can't yet receive messages from them.
     *<P>
     * Should send a message to the client in either {@link #newConnection1(StringConnection)}
     * or {@link #newConnection2(StringConnection)}.
     * You may also name the connection here by calling c.setData, which will help add to conns or unnamedConns.
     * This is also where the version should be set.
     *<P>
     * Note that {@link #addConnection(StringConnection)} won't close the channel or
     * take other action to disconnect a rejected client.
     *<P>
     * SYNCHRONIZATION NOTE: During the call to newConnection1, the monitor lock of
     * {@link #unnamedConns} is held.  Thus, defer as much as possible until
     * {@link #newConnection2(StringConnection)} (after the connection is accepted).
     *
     * @param c incoming connection to evaluate and act on
     * @return true to accept and continue, false if you have rejected this connection;
     *         if false, addConnection will call {@link StringConnection#disconnectSoft()}.
     *
     * @see #addConnection(StringConnection)
     * @see #newConnection2(StringConnection)
     * @see #nameConnection(StringConnection)
     */
    protected boolean newConnection1(StringConnection c) { return true; }

    /** placeholder for doing things when a new connection comes, part 2 -
     *  has been accepted and added to a connection list.
     *  Unlike {@link #newConnection1(StringConnection)},
     *  no connection-list locks are held when this method is called.
     *<P>
     * This method is called within a per-client thread.
     * You can send to client, but can't yet receive messages from them.
     */
    protected void newConnection2(StringConnection c) {}

    /** placeholder for doing things when a connection is closed.
     *  called after connection is removed from conns collection
     *  and version collection, and after c.disconnect() has been called.
     *<P>
     * This method is called within a per-client thread.
     */
    protected void leaveConnection(StringConnection c) {}

    /** The server is being cleanly stopped, disconnect all the connections.
     * Calls {@link #serverDown()} before disconnect; if your child class has more work
     * to do (such as sending a final message to all clients, or
     * disconnecting from a database), override serverDown() or stopServer().
     * Check {@link #isUp()} before calling.
     */
    public synchronized void stopServer()
    {
        up = false;
        serverDown();

        for (Enumeration e = conns.elements(); e.hasMoreElements();)
        {
            ((StringConnection) e.nextElement()).disconnect();
        }

        conns.clear();
    }

    /**
     * remove a connection from the system; synchronized on list of connections.
     * The callback {@link #leaveConnection(StringConnection)} will be called,
     * after calling {@link StringConnection#disconnect()} on c.
     *<P>
     * This method is called within a per-client thread.
     *
     * @param c Connection to remove; will call its disconnect() method
     *          and remove it from the server state.
     */
    public void removeConnection(StringConnection c)
    {
        Object cKey = c.getData();
        synchronized (unnamedConns)
        {
            if (cKey != null)
            {
                if (null == conns.remove(cKey))
                {
                    // Was not a member
                    return;
                }
            }
            else
            {
                unnamedConns.removeElement(c);
            }

            clientVersionRem(c.getVersion());  // One less of the cli's version
            c.setVersionTracking(false);
        }

        c.disconnect();
        leaveConnection(c);
        --numberCurrentConnections;
        D.ebugPrintln(c.host() + " left (" + connectionCount() + ")  " + (new Date()).toString() + ((c.getError() != null) ? (": " + c.getError().toString()) : ""));
    }

    /** do cleanup after a remove connection */
    protected void removeConnectionCleanup(StringConnection c) {}

    /**
     * Add a connection to the system.
     * Called within a per-client thread.
     * c.connect() is called at the start of this method.
     *<P>
     * App-specific work should be done by overriding
     * {@link #newConnection1(StringConnection)} and
     * {@link #newConnection2(StringConnection)}.
     * The connection naming and version is checked here (after newConnection1).
     *<P>
     * <b>Locking:</b> Synchronized on unnamedConns, although
     * named conns (getData not null) are added to conns, not unnamedConns.
     *
     * @param c Connecting client; its key data ({@link StringConnection#getData()}) must not be null.
     * @see #nameConnection(StringConnection)
     * @see #removeConnection(StringConnection)
     */
    public void addConnection(StringConnection c)
    {
        boolean connAccepted;

        synchronized (unnamedConns)
        {
            if (c.connect())
            {
                connAccepted = newConnection1(c);  // <-- App-specific #1 --
                if (connAccepted)
                {
                    Object cKey = c.getData();  // May be null
                    if (cKey != null)
                        conns.put(cKey, c);
                    else
                        unnamedConns.add(c);

                    clientVersionAdd(c.getVersion());  // Count one more client with that version
                    c.setVersionTracking(true);
                }
                else
                {
                    c.disconnectSoft();
                }
            } else {
                return;  // <--- early return: c.connect failed ---
            }
        }
        
        // Now that they're accepted, finish their init/welcome
        if (connAccepted)
        {
            numberOfConnections++;
            numberCurrentConnections++;
            D.ebugPrintln(c.host() + " came (" + connectionCount() + ")  " + (new Date()).toString());
            newConnection2(c);  // <-- App-specific #2 --
        } else {
            D.ebugPrintln(c.host() + " came but rejected (" + connectionCount() + ")  " + (new Date()).toString());
        }
    }

    /**
     * Name a current connection to the system.
     * Can be called once per connection (once named, cannot be changed).
     * Synchronized on unnamedConns.
     *<P>
     * If you name the connection inside {@link #newConnection1(StringConnection)},
     * you don't need to call nameConnection, because it hasn't yet been added
     * to a connection list.
     *
     * @param c Connected client; its key data ({@link StringConnection#getData()}) must not be null
     * @throws IllegalArgumentException If c isn't already connected, if c.getData() returns null,
     *          or if nameConnection has previously been called for this connection.
     * @see #addConnection(StringConnection)
     */
    public void nameConnection(StringConnection c)
        throws IllegalArgumentException
    {
        Object cKey = c.getData();
        if (cKey == null)
            throw new IllegalArgumentException("null c.getData");

        synchronized (unnamedConns)
        {
            if (unnamedConns.removeElement(c))
            {
                conns.put(cKey, c);            
            }
            else
            {
                throw new IllegalArgumentException("was not connected and unnamed");
            }
        }
    }

     /**
     * Add 1 client, with this version, to {@linkplain #cliVersionsConnected}.
     * <b>Locks:</b> Caller should synchronize on {@linkplain #unnamedConns}.
     * @see #clientVersionRem(int)
     * @see #getMinConnectedCliVersion()
     * @see #getMaxConnectedCliVersion()
     */
    public void clientVersionAdd(final int cvers)
    {
        Integer cvkey = new Integer(cvers);
        ConnVersionCounter cv = (ConnVersionCounter) cliVersionsConnected.get(cvkey);
        if (cv == null)
        {
            cv = new ConnVersionCounter(cvers);
            cliVersionsConnected.put (cvkey, cv);  // with cliCount == 1
        } else {
	        cv.cliCount++;
	    	return;  // <---- Early return: We already have this version ----
        }

        if (1 == cliVersionsConnected.size())
        {
            // This is the first connection.
            // Use its version# as the min/max.
            cliVersionMin = cvers;
            cliVersionMax = cvers;
        } else {
            if (cvers < cliVersionMin)
                cliVersionMin = cvers;
            else if (cvers > cliVersionMax)
                cliVersionMax = cvers;
        }
    }

    /**
     * Remove 1 client, with this version, from {@linkplain #cliVersionsConnected}.
     * <b>Locks:</b> Caller should synchronize on {@linkplain #unnamedConns}.
     * @see #clientVersionAdd(int)
     * @see #getMinConnectedCliVersion()
     * @see #getMaxConnectedCliVersion()
     */
    public void clientVersionRem(final int cvers)
    {
        Integer cvkey = new Integer(cvers);
        ConnVersionCounter cv = (ConnVersionCounter) cliVersionsConnected.get(cvkey);
        if (cv == null)
        {
            // TODO not found - must rebuild
        } else {
            cv.cliCount--;
            if (cv.cliCount > 0)
            {
                return;  // <---- Early return: Nothing else to do ----
            }

            // We've removed the last client of a particular version.
            // Update min/max if needed.
            // (If there are not any clients connected, doesn't matter.)

            cliVersionsConnected.remove(cvkey);
        }

        if (cliVersionsConnected.size() == 0)
        {
            return;  // <---- Early return: No other clients ----
        }

        if (cvers == cliVersionMin)
        {
            cliVersionMin = ((Integer) cliVersionsConnected.firstKey()).intValue();
        }
        else if (cvers == cliVersionMax)
        {
            cliVersionMax = ((Integer) cliVersionsConnected.lastKey()).intValue();
        }

        if (cv.cliCount < 0)
        {
            // TODO must rebuild - got below 0 somehow
        }
    }

    /**
     * @return the version number of the oldest-version client
     *         that is currently connected
     */
    public int getMinConnectedCliVersion()
    {
        return cliVersionMin;
    }

    /**
     * @return the version number of the newest-version client
     *         that is currently connected
     */
    public int getMaxConnectedCliVersion()
    {
        return cliVersionMax;
    }

    /**
     * Broadcast a SOCmessage to all connected clients, named and unnamed.
     *
     * @param m SOCmessage string, generated by {@link soc.message.SOCMessage#toCmd()}
     * @see #broadcastToVers(String, int, int)
     */
    protected synchronized void broadcast(String m)
    {
        for (Enumeration e = getConnections(); e.hasMoreElements();)
        {
            ((StringConnection) e.nextElement()).put(m);
        }
        for (Enumeration e = unnamedConns.elements(); e.hasMoreElements();)
        {
            ((StringConnection) e.nextElement()).put(m);
        }
    }

    /**
     * Broadcast a SOCmessage to all connected clients (named and
     * unnamed) within a certain version range.
     *
     * @param m SOCmessage string, generated by {@link soc.message.SOCMessage#toCmd()}
     * @param vmin Minimum version, as returned by {@link StringConnection#getVersion()},
     *             or {@link Integer#MIN_VALUE}
     * @param vmax Maximum version, or {@link Integer#MAX_VALUE}
     * @since 1.1.06
     * @see #broadcast(String)
     */
    protected synchronized void broadcastToVers(String m, final int vmin, final int vmax)
    {
        for (Enumeration e = getConnections(); e.hasMoreElements();)
        {
	    StringConnection c = (StringConnection) e.nextElement();
	    int cvers = c.getVersion();
	    if ((cvers >= vmin) && (cvers <= vmax))
		c.put(m);
        }
        for (Enumeration e = unnamedConns.elements(); e.hasMoreElements();)
        {
            StringConnection c = (StringConnection) e.nextElement();
	    int cvers = c.getVersion();
	    if ((cvers >= vmin) && (cvers <= vmax))
		c.put(m);
         }
     }

    class Command
    {
        public String str;
        public StringConnection con;

        public Command(String s, StringConnection c)
        {
            str = s;
            con = c;
        }
    }

    class Treater extends Thread
    {
        Server svr;

        public Treater(Server s)
        {
            svr = s;
            setName("treater");  // Thread name for debug
        }

        public void run()
        {
            while (svr.isUp())
            {
                //D.ebugPrintln("treater server is up");
                Command c = null;

                synchronized (inQueue)
                {
                    if (inQueue.size() > 0)
                    {
                        //D.ebugPrintln("treater getting command");
                        c = (Command) inQueue.elementAt(0);
                        inQueue.removeElementAt(0);
                    }
                }

                try
                {
                    if (c != null)
                    {
                        svr.processCommand(c.str, c.con);
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Exception in treater (processCommand) - " + e);
                }

                yield();

                synchronized (inQueue)
                {
                    if (inQueue.size() == 0)
                    {
                        try
                        {
                            //D.ebugPrintln("treater waiting");
                            inQueue.wait(1000);
                        }
                        catch (Exception ex)
                        {
                            ;
                        }
                    }
                }
            }

            // D.ebugPrintln("treater returning; server not up");
        }
    }

    /**
     * Uses ServerSocket to implement StringServerSocket over a network.
     */
    protected class NetStringServerSocket implements StringServerSocket
    {
        private ServerSocket implServSocket;
        private Server server;

        public NetStringServerSocket (int port, Server serv) throws IOException
        {
            implServSocket = new ServerSocket(port);
            server = serv;
        }

        public StringConnection accept() throws SocketException, IOException
        {
            Socket s = implServSocket.accept();
            return new Connection(s, server);  // Good old net, not generic StringConnection
        }

        public void close() throws IOException
        {
            implServSocket.close();
        }
    }

    /**
     * Hold info about 1 version of connected clients; for use in {@link #cliVersionsConnected} TreeMap.
     *
     * @since 1.1.06
     */
    protected static class ConnVersionCounter implements Comparable
    {
        public final int vers;
        public int cliCount;

        public ConnVersionCounter(final int version)
        {
            vers = version;
            cliCount = 1;
        }

        public boolean equals(Object o)
        {
            return (o instanceof ConnVersionCounter)
                && (this.vers == ((ConnVersionCounter) o).vers);
        }

        public int compareTo(Object o)
            throws ClassCastException
        {
            return (this.vers - ((ConnVersionCounter) o).vers);
        }

    }  // ConnVersionSetMember

}  // Server
