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
package soc.server;

import java.util.Enumeration;
import java.util.Vector;

import soc.debug.D;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.server.genericServer.StringConnection;

/**
 * This class holds data the server needs, related to a
 * "board reset" of a game being played.
 *
 * @see soc.server.SOCGameList#resetGame(String)
 * @see soc.game.SOCGame#resetAsCopy()
 *
 * @author Jeremy D. Monin <jeremy@nand.net>
 */
public class SOCGameReset
{
    /** The new game, created from an old game {@link soc.game.SOCGame#resetAsCopy()} */ 
    public SOCGame newGame;
    /** Were there robots in the old game? */
    public boolean hadRobots;
    /** Human and robot connections; both arrays null at vacant seats. Indexed 0 to SOCGame.MAXPLAYERS-1 */
    public StringConnection[] humanConns, robotConns;
    /** Was this player position a robot? Indexed 0 to SOCGame.MAXPLAYERS-1 */
    public boolean[] wasRobot;

    /** Create a SOCGameReset: Extract data, reset the old game, and gather new data.
     *  Adjust game member list to remove robots.
     *
     * @param oldGame Game to reset - {@link soc.game.SOCGame#resetAsCopy()} will be called.
     *   The old game's state will be changed to RESET_OLD.
     * @param memberConns Game members (StringConnections),
     *   as retrieved by {@link SOCGameList#getMembers(String)}.
     *   Contents of this vector will be changed to remove any robot members.
     */
    public SOCGameReset (SOCGame oldGame, Vector memberConns)
    {
        hadRobots = false;
        wasRobot = new boolean[SOCGame.MAXPLAYERS];
        for (int i = 0; i < SOCGame.MAXPLAYERS; ++i)
        {
            SOCPlayer pl = oldGame.getPlayer(i);
            boolean isRobot = pl.isRobot();
            wasRobot[i] = isRobot;
            if (isRobot)
                hadRobots = true;
        }

        /**
         * Reset the game
         */
        newGame = oldGame.resetAsCopy(); 

        /**
         * Gather connection information, cleanup member list
         */
        humanConns = new StringConnection[SOCGame.MAXPLAYERS];
        robotConns = new StringConnection[SOCGame.MAXPLAYERS];
        if (memberConns != null)
        {
            // Grab connection information for humans and robots.
            // This enum is easier than enumerating all connected clients;
            // there is no server-wide mapping of clientname -> connection.
            // memberConns is from _old_ game, so robots are included.
            // Robots aren't copied to the new game, and must re-join.
            Enumeration playersEnum = memberConns.elements();
            while (playersEnum.hasMoreElements())
            {
                StringConnection pCon = (StringConnection) playersEnum.nextElement();
                String pname = (String) pCon.getData();
                SOCPlayer p = newGame.getPlayer(pname);
                if (p != null)
                {
                    int pn = p.getPlayerNumber();                    
                    humanConns[pn] = pCon;
                }
                else
                {
                    // No such player in new game.
                    // Assume is robot player in old game.
                    p = oldGame.getPlayer(pname);
                    if (p != null)
                    {
                        int pn = p.getPlayerNumber();
                        if (p.isRobot())
                            robotConns[pn] = pCon;
                        else
                            // should not happen
                            D.ebugPrintln("SOCGameReset assert failed: human player not copied: " + pn);
                    }
                }
            }

            // Check all player positions after enum
            for (int pn = 0; pn < SOCGame.MAXPLAYERS; ++pn)
            {
                if ((humanConns[pn] == null) && (robotConns[pn] == null) && ! newGame.isSeatVacant(pn))
                    D.ebugPrintln("SOCGameReset assert failed: did not find player " + pn);
            }

            // Remove robots from list of game members
            for (int pn = 0; pn < SOCGame.MAXPLAYERS; ++pn)
            {
                if (wasRobot[pn])
                    memberConns.remove(robotConns[pn]);
            }
        }
    }

}  // public class SOCGameReset
