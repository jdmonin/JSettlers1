/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2008 Jeremy D. Monin <jeremy@nand.net>
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

/**
 * This message from server informs all clients that the board reset
 * request has been rejected in voting.
 *
 * @see SOCResetBoardRequest
 * @author Jeremy D. Monin <jeremy@nand.net>
 *
 */
public class SOCResetBoardReject extends SOCMessage
{
    /**
     * Name of game
     */
    private String game;

    /**
     * Create a SOCResetBoardReject message.
     *
     * @param ga  the name of the game
     */
    public SOCResetBoardReject(String ga)
    {
        messageType = RESETBOARDREJECT;
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
     * RESETBOARDREJECT sep game
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game);
    }

    /**
     * RESETBOARDREJECT sep game
     *
     * @param ga  the name of the game
     * @return the command string
     */
    public static String toCmd(String ga)
    {
        return RESETBOARDREJECT + sep + ga;
    }

    /**
     * Parse the command String into a SOCResetBoardReject message
     *
     * @param s   the String to parse
     * @return    a SOCResetBoardAuth message, or null if the data is garbled
     */
    public static SOCResetBoardReject parseDataStr(String s)
    {
        // s is just the game name
        return new SOCResetBoardReject(s);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCResetBoardReject:game=" + game;
    }
}
