/**
 * Local (StringConnection) network system.
 * Copyright (C) 2007 Jeremy D Monin <jeremy@nand.net>.
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
 * The author of this program can be reached at jeremy@nand.net
 **/
package soc.server.genericServer;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Vector;

import soc.disableDebug.D;

/**
 * @author Jeremy D. Monin <jeremy@nand.net>
 *
 */
public class LocalStringConnection
    implements StringConnection, Runnable
{
    protected static Object EOF_MARKER = new Object();
    
    protected Vector in, out;
    protected boolean in_reachedEOF;
    protected boolean out_setEOF;
    protected boolean accepted;  // TODO consider sync for touching methods
    private LocalStringConnection ourPeer;
    
    protected Server ourServer;  // Optional. Notifies at EOF.
    protected Exception error;

    /**
     * the abritrary app-specific data associated with this connection
     */
    protected Object data;
    
    /**
     * After creation, call connect().
     *
     * @see #connect(String)
     */
    public LocalStringConnection()
    {
        in = new Vector();
        out = new Vector();
        in_reachedEOF = false;
        out_setEOF = false;
        accepted = false;
        data = null;
        ourServer = null;
        error = null;
    }

    /**
     * Constructor for a peer; we'll share two Vectors for in/out queues.
     * 
     * @param peer The peer to use.
     * 
     * @throws EOFException If peer is at EOF already 
     * @throws IllegalArgumentException if peer is null, or already
     *   has a peer.
     */
    public LocalStringConnection(LocalStringConnection peer) throws EOFException
    {
        if (peer == null)
            throw new IllegalArgumentException("peer null");
        if (peer.ourPeer != null)
            throw new IllegalArgumentException("peer already has a peer");
        if (peer.isOutEOF() || peer.isInEOF())
            throw new EOFException("peer EOF at constructor");
        in = peer.out;
        out = peer.in;
        in_reachedEOF = false;
        out_setEOF = false;
        accepted = false;
        data = null;
        ourServer = null;
        peer.ourPeer = this;
        this.ourPeer = peer;
        error = null;
    }

    /**
     * TODO docu
     * Synchronized on in-buffer.
     * Also sets data.
     * 
     * @return Next string in the in-buffer
     * @throws EOFException Our input buffer has reached EOF
     * @throws IllegalStateException Server has not yet accepted our connection
     * 
     * @see #getData()
     */
    public String readNext() throws EOFException, IllegalStateException
    {
        if (! accepted)
        {
            error = new IllegalStateException("Not accepted by server yet");
            throw (IllegalStateException) error;
        }
        if (in_reachedEOF)
        {
            error = new EOFException();
            throw (EOFException) error;
        }

        Object obj;
        
        synchronized (in)
        {
            while (in.isEmpty())
            {
                if (in_reachedEOF)
                {
                    error = new EOFException();
                    throw (EOFException) error;
                }
                else
                {
                    try
                    {
                        in.wait();
                    }
                    catch (InterruptedException e) {}
                }
            }
            obj = in.elementAt(0);
            in.removeElementAt(0);
            
            if (obj == EOF_MARKER)
            {
                in_reachedEOF = true;
                if (ourServer != null)
                    ourServer.removeConnection(this);
                error = new EOFException();
                throw (EOFException) error;
            }
        }
        return (String) obj;
    }
    
    /**
     * Send to other end.
     * Ignored if out EOF is set. 
     */
    public void put(String dat) throws IllegalStateException
    {
        if (! accepted)
        {
            error = new IllegalStateException("Not accepted by server yet");
            throw (IllegalStateException) error;
        }
        if (out_setEOF)
            return;
        synchronized (out)
        {
            out.addElement(dat);
            out.notifyAll();  // Another thread may have been waiting for input
        }
    }

    /** close the socket, set EOF */
    public void disconnect()
    {
        D.ebugPrintln("DISCONNECTING " + data);
        accepted = false;
        out.clear();
        in.clear();
        in_reachedEOF = true;
        setEOF();
        // connected = false;  JM TODO
    }

    public void connect(String serverSocketName) throws ConnectException, IllegalStateException
    {
        if (accepted)
            throw new IllegalStateException("Already accepted by a server");

        LocalStringConnection p = null;
        p = LocalStringServerSocket.connectTo(serverSocketName, this);
        // will set ourPeer and use our in/out if it works.

        // ** connectTo will Thread.wait until accepted by server.
        
        accepted = true;
        
        // TODO write an isConnected (what does it mean? (vs EOFs))
        // TODO should we be throwing away connectTo's return?
        //     Is it returning the right kind of thing?
    }
    
    /**
     * Remember, the peer's in is our out, and vice versa. 
     * 
     * @return Returns our peer, or null if not yet connected.
     */
    public LocalStringConnection getPeer()
    {
        return ourPeer;
    }

    /**
     * Is or was accepted by a server
     * 
     * @return Returns the isAccepted.
     */
    public boolean isAccepted()
    {
        return accepted;
    }

    /**
     * Intended for server to call: Set our accepted flag.
     * Peer must be non-null to set accepted.
     * 
     * @throws IllegalStateException If we can't be, or already are, accepted
     */
    public void setAccepted() throws IllegalStateException
    {
        if (ourPeer == null)
            throw new IllegalStateException("No peer, can't be accepted");
        if (accepted)
            throw new IllegalStateException("Already accepted");
        accepted = true;
    }
    
    /**
     * Signal the end of outbound data.
     * Not the same as closing, because we don't terminate the inbound side.
     * 
     * Synchronizes on out-buffer.
     */
    public void setEOF()
    {
        // TODO close method, let the remote-end know we're closing
        
        synchronized (out)
        {
            out.addElement(EOF_MARKER);
            out_setEOF = true;
            out.notifyAll();
        }
    }

    public boolean isInEOF()
    {
        synchronized (in)
        {
            return in_reachedEOF;
        }
    }

    public boolean isOutEOF()
    {
        synchronized (out)
        {
            return out_setEOF;            
        }
    }

    /**
     * @return The app-specific data for this generic connection
     */
    public Object getData()
    {
        return data;
    }
    
    /**
     * Set the data for this connection
     * 
     * @param dat The new data, or null
     */
    public void setData(Object dat)
    {
        data = dat;
    }

    /**
     * @return The generic server (optional) for this connection
     */
    public Server getServer()
    {
        return ourServer;
    }
    
    /**
     * Server-side: Set the generic server for this connection.
     * If a server is set, its removeConnection method is called if our input reaches EOF.
     * Call this before calling run().
     * 
     * @param dat The new server, or null
     */
    public void setServer(Server srv)
    {
        ourServer = srv;
    }

    public Exception getError()
    {
        return error;
    }
    
    /**
     * Always returns localhost; StringConnection interface.
     */
    public String host()
    {
        return "localhost";
    }
    
    /**
     * Local version; nothing special to do to start reading messages.
     */
    public boolean connect()
    {
        return accepted;
    }

    /* TODO
     * @see soc.server.genericServer.StringConnection#isConnected()
     */
    public boolean isConnected()
    {
        return accepted && ! out_setEOF;
    }

    /** For server-side (ourServ != null); continuously read and treat input */
    public void run()
    {
        Thread.currentThread().setName("connection-srv-localstring");
        
        if (ourServer == null)
            return;
        
        ourServer.addConnection(this);

        try
        {
            while (! in_reachedEOF)
            {
                ourServer.treat(readNext(), this);
            }
        }
        catch (IOException e)
        {
            D.ebugPrintln("IOException in LocalStringConnection.run - " + e);

            if (D.ebugOn)
            {
                e.printStackTrace(System.out);
            }

            if (in_reachedEOF)
            {
                return;
            }

            error = e;
            ourServer.removeConnection(this);
        }
    }
    
}
