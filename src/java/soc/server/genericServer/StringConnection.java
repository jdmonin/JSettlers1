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

/**
 * StringConnection allows clients and servers to communicate,
 * with no difference between local and actual networked traffic.
 * 
 * @author Jeremy D Monin <jeremy@nand.net>
 */
public interface StringConnection
{

    /**
     * @return Hostname of the remote end of the connection
     */
    public abstract String host();

    /**
     * Send data over the connection.
     *
     * @param str Data to send
     *
     * @throws IllegalStateException if not yet accepted by server
     */
    public abstract void put(String str)
        throws IllegalStateException;

    /** For server-side thread which reads and treats incoming messages */
    public abstract void run();

    /** Are we currently connected and active? */
    public abstract boolean isConnected();

    /** start ability to read from the net; called only by the server.
     * (In a non-local version, another thread may be started by this method.)
     * 
     * @return true if able to connect, false if an error occurred.
     */    
    public abstract boolean connect(); 

    /** close the socket, set EOF */
    public abstract void disconnect();

    /**
     * @return The app-specific data for this connection.
     */
    public abstract Object getData();

    /**
     * Set tapp-specific data for this connection, or null.
     */
    public abstract void setData(Object data);

    /**
     * @return Any error encountered, or null
     */
    public abstract Exception getError();

}
