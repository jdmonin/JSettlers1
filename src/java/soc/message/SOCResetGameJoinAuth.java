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
 * This message from server informs the client that a game they're playing
 * has been "reset" to a new board (and new game with same name),
 * and they should join at the given position.
 * This message replaces the {@link SOCJoinGameAuth} seen when joining a brand-new game.
 *
 * For details of messages sent, see 
 * {@link soc.server.SOCServer#resetBoardAndNotify(String, String)}.
 *
 * @see SOCResetBoardRequest
 * @author Jeremy D. Monin <jeremy@nand.net>
 *
 */
public class SOCResetGameJoinAuth extends SOCMessage
{
    /**
     * Name of game
     */
    private String game;

    /**
     * The player position at which client should re-join;
     * same as their existing position on the old board.
     */
    private int playerNumber;

    /**
     * Name of player requesting the reset
     */
    private String requesterName;

    /**
     * Create a ResetBoardJoin message.
     *
     * @param ga  the name of the game
     * @param pn  the player position number at which to join
     * @param rn  name of the player who requested the reset
     */
    public SOCResetGameJoinAuth(String ga, int pn, String rn)
    {
        messageType = RESETGAMEJOINAUTH;
        game = ga;
        playerNumber = pn;
        requesterName = rn;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the player position number
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * @return the name of the player who requested the board reset
     */
    public String getRequestingPlayer()
    {
        return requesterName;
    }

    /**
     * RESETGAMEJOINAUTH sep game sep2 playernumber
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, playerNumber, requesterName);
    }

    /**
     * RESETGAMEJOINAUTH sep game sep2 playernumber
     *
     * @param ga  the name of the game
     * @param pn  the client's player position number
     * @param rn  the requesting player's name
     * @return the command string
     */
    public static String toCmd(String ga, int pn, String rn)
    {
        return RESETGAMEJOINAUTH + sep + ga + sep2 + pn + sep2 + rn;
    }

    /**
     * Parse the command String into a SOCResetGameJoinAuth message
     *
     * @param s   the String to parse
     * @return    a SOCResetGameJoinAuth message, or null if the data is garbled
     */
    public static SOCResetGameJoinAuth parseDataStr(String s)
    {
        String ga; // the game name
        int pn;    // the player number
        String rn; // the requester player name

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
            rn = st.nextToken();
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCResetGameJoinAuth(ga, pn, rn);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCResetGameJoinAuth:game=" + game + "|playerNumber=" + playerNumber + "|requester=" + requesterName;
    }
}
