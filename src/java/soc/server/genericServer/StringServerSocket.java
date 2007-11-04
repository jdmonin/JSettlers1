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

/**
 * StringServerSocket allows server applications to communicate with clients,
 * with no difference between local and actual networked traffic.
 * 
 * @author Jeremy D Monin <jeremy@nand.net>
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
