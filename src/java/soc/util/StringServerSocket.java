/**
 * 
 */
package soc.util;

import java.io.EOFException;
import java.io.IOException;

import soc.server.genericServer.StringConnection;

/**
 * @author jdmonin
 *
 */
public interface StringServerSocket
{

    /**
     * For server to call.  Blocks waiting for next inbound connection.
     * 
     * @return The server-side peer to the inbound client connection
     * @throws IOException  if network has a problem accepting
     * @throws EOFException if our setEOF() has been called, thus
     *    new clients won't receive any data from us
     */
    public abstract StringConnection accept() throws EOFException, IOException;

    public abstract void close() throws IOException;

}
