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
 * This bi-directional message gives the client's vote on a "board reset",
 * which was requested by another player in that game.
 *<P>
 * When sent from client to server, it gives that client's vote.
 * This won't come from robots: robots are assumed to vote yes and go along.
 *<P>
 * When sent from server to (other) clients, it informs the players of the
 * other player's vote.
 *
 * @see SOCResetBoardRequest
 * @author Jeremy D. Monin <jeremy@nand.net>
 */
public class SOCResetBoardVote extends SOCMessage
{
    /**
     * Name of game
     */
    private String game;

    /**
     * the player number of who voted (used when sending to other clients)
     */
    private int playerNumber;

    /**
     * Did they vote yes?
     */
    private boolean votedYes;

    /**
     * Create a SOCResetBoardVoteRequest message.
     *
     * @param ga  the name of the game
     * @param pn  the player position who voted (used when sending to other clients)
     * @param pyes  did they vote yes
     */
    public SOCResetBoardVote(String ga, int pn, boolean pyes)
    {
        messageType = RESETBOARDVOTE;
        game = ga;
        playerNumber = pn;
        votedYes = pyes;
    }

    /**
     * @return the name of the game
     */
    public String getGame()
    {
        return game;
    }

    /**
     * @return the voter's player number
     */
    public int getPlayerNumber()
    {
        return playerNumber;
    }

    /**
     * @return if true, the vote is Yes
     */
    public boolean getPlayerVote()
    {
        return votedYes;
    }

    /**
     * RESETBOARDVOTE sep game sep2 playernumber sep2 yesno
     *<P>
     * Yes is Y, No is N
     *
     * @return the command string
     */
    public String toCmd()
    {
        return toCmd(game, playerNumber, votedYes);
    }

    /**
     * RESETBOARDVOTE sep game sep2 playernumber sep2 yesno
     *<P>
     * Yes is Y, No is N
     *
     * @param ga  the name of the game
     * @param pn  the voter's player number
     * @param pyes if the vote was yes
     * @return the command string
     */
    public static String toCmd(String ga, int pn, boolean pyes)
    {
        return RESETBOARDVOTE + sep + ga + sep2 + pn + sep2
            + (pyes ? "Y" : "N");
    }

    /**
     * Parse the command String into a SOCResetBoardVote message
     *
     * @param s   the String to parse
     * @return    a SOCResetBoardVote message, or null if the data is garbled
     */
    public static SOCResetBoardVote parseDataStr(String s)
    {
        String ga; // the game name
        int pn;    // the voter's player number
        String vy; // vote, "Y" or "N"

        StringTokenizer st = new StringTokenizer(s, sep2);

        try
        {
            ga = st.nextToken();
            pn = Integer.parseInt(st.nextToken());
            vy = st.nextToken();
        }
        catch (Exception e)
        {
            return null;
        }

        return new SOCResetBoardVote(ga, pn, vy.equals("Y"));
    }

    /**
     * @return a human readable form of the message
     */
    public String toString()
    {
        return "SOCResetBoardVote:game=" + game + "|pn=" + playerNumber
            + "|vote=" + (votedYes ? "Y" : "N");
    }
}
