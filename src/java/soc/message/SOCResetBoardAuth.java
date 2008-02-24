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
 * This message from server informs the client that a game they're playing
 * has been "reset" to a new game (with same name and players, new layout),
 * and they should join at the given position.
 *<P>
 * For robots, they must discard game state and ask to re-join.
 * Treat as a {@link SOCJoinGameRequest}: ask server for us to join the new game.
 *<P>
 * For human players, this message replaces the {@link SOCJoinGameAuth} seen when joining a brand-new game; the reset message will be followed
 * with others which will fill in the game state.
 *<P>
 * For details of messages sent, see 
 * {@link soc.server.SOCServer#resetBoardAndNotify(String, String)}.
 *
 * @see SOCResetBoardRequest
 * @author Jeremy D. Monin <jeremy@nand.net>
 *
 */
public class SOCResetBoardAuth extends SOCMessage
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
     * Player number requesting the reset
     */
    private int requesterNumber;

    /**
     * Create a ResetBoardAuth message.
     *
     * @param ga  the name of the game
     * @param joinpn  the player position number at which to join
     * @param reqpn  player number who requested the reset
     */
    public SOCResetBoardAuth(String ga, int joinpn, int reqpn)
    {
        messageType = RESETBOARDAUTH;
        game = ga;
        playerNumber = joinpn;
        requesterNumber = reqpn;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the player position number at which to rejoin
     */
    public int getRejoinPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * @return the number of the player who requested the board reset
     */
    public int getRequestingPlayerNumber()
    {
        return requesterNumber;
    }

    /**
     * RESETBOARDAUTH sep game sep2 playernumber sep2 requester
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, playerNumber, requesterNumber);
    }

    /**
     * RESETBOARDAUTH sep game sep2 playernumber sep2 requester
     *
     * @param ga  the name of the game
     * @param joinpn  the player position number at which to join
     * @param reqpn  player number who requested the reset
     * @return the command string
     */
    public static String toCmd(String ga, int joinpn, int reqpn)
    {
        return RESETBOARDAUTH + sep + ga + sep2 + joinpn + sep2 + reqpn;
    }

    /**
     * Parse the command String into a SOCResetBoardAuth message
     *
     * @param s   the String to parse
     * @return    a SOCResetBoardAuth message, or null if the data is garbled
     */
    public static SOCResetBoardAuth parseDataStr(String s)
    {
        String ga;   // the game name
        int joinpn;  // the player number to join at
        int reqpn;   // the requester player number

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            joinpn = Integer.parseInt(st.nextToken());
            reqpn = Integer.parseInt(st.nextToken());
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCResetBoardAuth(ga, joinpn, reqpn);
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCResetBoardAuth:game=" + game + "|joinPlayerNumber=" + playerNumber + "|requester=" + requesterNumber;
    }
}
