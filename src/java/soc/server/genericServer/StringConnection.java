/**
 * 
 */
package soc.server.genericServer;

import java.io.EOFException;

/**
 * @author Jeremy D Monin <jeremy@nand.net>
 *
 */
public interface StringConnection
{

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public abstract String host();

    /**
     * DOCUMENT ME!
     *
     * @param str DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public abstract void put(String str)
        throws IllegalStateException;
    
    /** For server-side thread which reads and treats incoming messages */
    public abstract void run();

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
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

    public abstract Exception getError();

}