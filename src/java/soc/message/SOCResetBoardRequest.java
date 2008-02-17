/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2007 Jeremy D. Monin <jeremy@nand.net>
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
package soc.message;

import java.util.StringTokenizer;

import soc.server.genericServer.StringConnection;


/**
 * This message from client to server requests a "board reset" of a game
 * being played.
 *
 * If reset is allowed, server will respond with {@link soc.message.SOCResetGameJoinAuth}
 * and subsequent messages. For details, see 
 * {@link soc.server.SOCServer#resetBoardAndNotify(String, String)}.
 *
 * @author Jeremy D. Monin <jeremy@nand.net>
 */
public class SOCResetBoardRequest extends SOCMessage
{
    /**
     * Name of game
     */
    private String game;

    /**
     * Create a ResetBoardRequest message.
     *
     * @param ga  the name of the game
     */
    public SOCResetBoardRequest(String ga)
    {
        messageType = RESETBOARDREQUEST;
        game = ga;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * RESETBOARDREQUEST sep game
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game);
    }

    /**
     * RESETBOARDREQUEST sep game
     *
     * @param ga  the name of the game
     * @return the command string
     */
    public static String toCmd(String ga)
    {
        return RESETBOARDREQUEST + sep + ga;
    }

    /**
     * Parse the command String into a ResetBoardRequest message
     *
     * @param s   the String to parse
     * @return    a ResetBoardRequest message
     */
    public static SOCResetBoardRequest parseDataStr(String s)
    {
        return new SOCResetBoardRequest(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCResetBoardRequest:game=" + game;
    }
}
