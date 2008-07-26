/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
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

import soc.debug.D;  // JM

import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCDevCardConstants;
import soc.game.SOCDevCardSet;
import soc.game.SOCGame;
import soc.game.SOCMoveRobberResult;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;
import soc.game.SOCTradeOffer;

import soc.message.*;

import soc.server.database.SOCDBHelper;

import soc.server.genericServer.Server;
import soc.server.genericServer.StringConnection;

import soc.util.IntPair;
import soc.util.SOCRobotParameters;
import soc.util.Version;

import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * A server for Settlers of Catan
 *
 * @author  Robert S. Thomas
 *
 * Note: This is an attempt at being more modular. 5/13/99 RST
 * Note: Hopfully fixed all of the deadlock problems. 12/27/01 RST
 */
public class SOCServer extends Server
{
    /**
     * Name used when sending messages from the server.
     */
    public static final String SERVERNAME = "Server";

    /**
     * If game will expire in this or fewer minutes, warn the players. Default 10.
     * Must be at least twice the sleep-time in SOCGameTimeoutChecker.run().
     * The game expiry time is set at game creation in SOCGameList.CreateGame.
     *
     * @see #checkForExpiredGames()
     * @see SOCGameTimeoutChecker#run()
     * @see SOCGameList#createGame(String)
     */
    public static int GAME_EXPIRE_WARN_MINUTES = 10;

    /**
     * For local practice games (pipes, not TCP), the name of the pipe.
     * Used to distinguish practice vs "real" games.
     * 
     * @see soc.util.LocalStringConnection
     */
    public static String PRACTICE_STRINGPORT = "SOCPRACTICE"; 

    /**
     * So we can get random numbers.
     */
    private Random rand = new Random();

    /**
     * The TCP port we listen on.
     */
    public int port;

    /**
     * Maximum number of connections allowed
     */
    protected int maxConnections;

    /**
     * A list of robots connected to this server
     */
    protected Vector robots = new Vector();

    /**
     * Robot default parameters; copied for each newly connecting robot.
     * Changing this will not change parameters of any robots already connected.
     *
     * @see #handleIMAROBOT(StringConnection, SOCImARobot)
     * @see soc.robot.SOCRobotDM
     */
    public static SOCRobotParameters ROBOT_PARAMS_DEFAULT
        = new SOCRobotParameters(120, 35, 0.13f, 1.0f, 1.0f, 3.0f, 1.0f, 1, 1);
        // Formerly a literal in handleIMAROBOT.
        // Strategy type 1 == SOCRobotDM.FAST_STRATEGY.
        // If you change values here, see SOCPlayerClient.startPracticeGame
        // for assumptions which may also need to be changed.

    /**
     * Smarter robot default parameters. (For practice games; not referenced by server)
     * Same as ROBOT_PARAMS_DEFAULT but with SMART_STRATEGY, not FAST_STRATEGY.
     *
     * @see #ROBOT_PARAMS_DEFAULT
     * @see soc.robot.SOCRobotDM
     */
    public static SOCRobotParameters ROBOT_PARAMS_SMARTER
        = new SOCRobotParameters(120, 35, 0.13f, 1.0f, 1.0f, 3.0f, 1.0f, 0, 1);
    
    /** Status Message to send, nickname already logged into the system */
    public static final String MSG_NICKNAME_ALREADY_IN_USE
        = "Someone with that nickname is already logged into the system.";

    /**
     * list of chat channels
     */
    protected SOCChannelList channelList = new SOCChannelList();

    /**
     * list of soc games
     */
    protected SOCGameList gameList = new SOCGameList();

    /**
     * table of requests for robots to join games
     */
    protected Hashtable robotJoinRequests = new Hashtable();

    /**
     * table of requestst for robots to leave games
     */
    protected Hashtable robotDismissRequests = new Hashtable();

    /**
     * table of game data files
     */
    protected Hashtable gameDataFiles = new Hashtable();

    /**
     * the current game event record
     */

    //protected SOCGameEventRecord currentGameEventRecord;

    /**
     * the time that this server was started
     */
    protected long startTime;

    /**
     * the total number of games that have been started
     */
    protected int numberOfGamesStarted;

    /**
     * the total number of games finished
     */
    protected int numberOfGamesFinished;

    /**
     * total number of users
     */
    protected int numberOfUsers;

    /**
     * server robot pinger
     */
    SOCServerRobotPinger serverRobotPinger;

    /**
     * game timeout checker
     */
    SOCGameTimeoutChecker gameTimeoutChecker;
    String databaseUserName;
    String databasePassword;

    /**
     * Create a Settlers of Catan server listening on port p.
     * You must start its thread yourself.
     *
     * @param p    the port that the server listens on
     * @param mc   the maximum number of connections allowed
     * @param databaseUserName  the user name for accessing the database
     * @param databasePassword  the password for the user
     */
    public SOCServer(int p, int mc, String databaseUserName, String databasePassword)
    {
        super(p);
        port = p;
        maxConnections = mc;
        initSocServer(databaseUserName, databasePassword);
    }

    /**
     * Create a Settlers of Catan server listening on local stringport s.
     * You must start its thread yourself.
     *
     * @param s    the stringport that the server listens on
     * @param mc   the maximum number of connections allowed
     * @param databaseUserName  the user name for accessing the database
     * @param databasePassword  the password for the user
     */
    public SOCServer(String s, int mc, String databaseUserName, String databasePassword)
    {
        super(s);
        maxConnections = mc;
        initSocServer(databaseUserName, databasePassword);
    }    

    /**
     * Common init for both constructors.
     * 
     * @param databaseUserName Used for DB connect - not retained
     * @param databasePassword Used for DB connect - not retained
     */
    private void initSocServer(String databaseUserName, String databasePassword)
    {
        System.err.println("Java Settlers Server " + Version.version() +
                           ", build " + Version.buildnum() + ", " + Version.copyright());
        System.err.println("Network layer based on code by Cristian Bogdan; local network by Jeremy Monin.");
        
        /* Check for problems during super setup (such as port already in use) */
        if (error != null)
        {
            System.err.println("* Exiting due to network setup problem: " + error.toString());
            System.exit (1);
        }

        try
        {
            SOCDBHelper.initialize(databaseUserName, databasePassword);
            System.err.println("User database initialized.");
        }
        catch (SQLException x) // just a warning
        {
            System.err.println("No user database available: " +
                               x.getMessage());
            System.err.println("Users will not be authenticated.");
        }

        startTime = System.currentTimeMillis();
        numberOfGamesStarted = 0;
        numberOfGamesFinished = 0;
        numberOfUsers = 0;
        serverRobotPinger = new SOCServerRobotPinger(robots);
        serverRobotPinger.start();
        gameTimeoutChecker = new SOCGameTimeoutChecker(this);
        gameTimeoutChecker.start();
        this.databaseUserName = databaseUserName;
        this.databasePassword = databasePassword;
    }

    /**
     * Adds a connection to a chat channel.
     *
     * WARNING: MUST HAVE THE channelList.takeMonitorForChannel(ch)
     * before calling this method
     *
     * @param c    the Connection to be added
     * @param ch   the name of the channel
     *
     */
    public void connectToChannel(StringConnection c, String ch)
    {
        if (c != null)
        {
            if (channelList.isChannel(ch))
            {
                if (!channelList.isMember(c, ch))
                {
                    c.put(SOCMembers.toCmd(ch, channelList.getMembers(ch)));
                    D.ebugPrintln("*** " + c.getData() + " joined the channel " + ch);
                    channelList.addMember(c, ch);
                }
            }
        }
    }

    /**
     * the connection c leaves the channel ch
     *
     * WARNING: MUST HAVE THE channelList.takeMonitorForChannel(ch) before
     * calling this method
     *
     * @param c  the connection
     * @param ch the channel
     * @param channelListLock  true if we have the channelList monitor
     * @return true if we destroyed the channel
     */
    public boolean leaveChannel(StringConnection c, String ch, boolean channelListLock)
    {
        D.ebugPrintln("leaveChannel: " + c.getData() + " " + ch + " " + channelListLock);

        boolean result = false;

        if (c != null)
        {
            if (channelList.isMember(c, ch))
            {
                channelList.removeMember(c, ch);

                SOCLeave leaveMessage = new SOCLeave((String) c.getData(), c.host(), ch);
                messageToChannelWithMon(ch, leaveMessage);
                D.ebugPrintln("*** " + (String) c.getData() + " left the channel " + ch);
            }

            if (channelList.isChannelEmpty(ch))
            {
                if (channelListLock)
                {
                    channelList.deleteChannel(ch);
                }
                else
                {
                    channelList.takeMonitor();

                    try
                    {
                        channelList.deleteChannel(ch);
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintln("Exception in leaveChannel - " + e);
                    }

                    channelList.releaseMonitor();
                }

                result = true;
            }
        }

        return result;
    }

    /**
     * Adds a connection to a game.
     *
     * @param c    the Connection to be added
     * @param ga   the name of the game
     *
     * @return     true if c was not a member of ch before
     */
    public boolean connectToGame(StringConnection c, String ga)
    {
        boolean result = false;

        if (c != null)
        {
            boolean gameExists = false;
            gameList.takeMonitor();

            try
            {
                gameExists = gameList.isGame(ga);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Excepetion in connectToGame - " + e);
            }

            gameList.releaseMonitor();

            if (gameExists)
            {
                gameList.takeMonitorForGame(ga);

                try
                {
                    if (gameList.isMember(c, ga))
                    {
                        result = false;
                    }
                    else
                    {
                        gameList.addMember(c, ga);
                        result = true;
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Excepetion in connectToGame (isMember) - " + e);
                }

                gameList.releaseMonitorForGame(ga);
            }
            else
            {
                /**
                 * the game did not exist, create it
                 */
                gameList.takeMonitor();

                boolean monitorReleased = false;

                try
                {
                    gameList.createGame(ga);  // Create new game, expiring in SOCGameList.GAME_EXPIRE_MINUTES .
                    gameList.addMember(c, ga);

                    // must release monitor before we broadcast
                    gameList.releaseMonitor();
                    monitorReleased = true;
                    broadcast(SOCNewGame.toCmd(ga));
                    result = true;
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Excepetion in connectToGame - " + e);
                }

                if (!monitorReleased)
                {
                    gameList.releaseMonitor();
                }
            }

            return result;
        }
        else
        {
            return false;
        }
    }

    /**
     * the connection c leaves the game gm
     *
     * WARNING: MUST HAVE THE gameList.takeMonitorForGame(gm) before
     * calling this method
     *
     * @param c  the connection
     * @param gm the game
     * @param gameListLock  true if we have the gameList.takeMonitor() lock
     * @return true if the game was destroyed
     */
    public boolean leaveGame(StringConnection c, String gm, boolean gameListLock)
    {
        boolean gameDestroyed = false;

        if (c != null)
        {
            gameList.removeMember(c, gm);

            boolean isPlayer = false;
            int playerNumber = 0;
            SOCGame cg = gameList.getGameData(gm);

            boolean gameHasHumanPlayer = false;
            boolean gameHasObserver = false;

            if (cg != null)
            {
                for (playerNumber = 0; playerNumber < SOCGame.MAXPLAYERS;
                        playerNumber++)
                {
                    SOCPlayer player = cg.getPlayer(playerNumber);

                    if ((player != null) && (player.getName() != null))
                    {
                        if (player.getName().equals((String) c.getData()))
                        {
                            isPlayer = true;
                            cg.removePlayer((String) c.getData());

                            //broadcastGameStats(cg);
                            break;
                        }
                    }
                }

                SOCLeaveGame leaveMessage = new SOCLeaveGame((String) c.getData(), c.host(), gm);
                messageToGameWithMon(gm, leaveMessage);
                recordGameEvent(gm, leaveMessage.toCmd());

                D.ebugPrintln("*** " + (String) c.getData() + " left the game " + gm);
                messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, (String) c.getData() + " left the game"));

                /**
                 * check if there is at least one person playing the game
                 */
                for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++)
                {
                    if (cg != null)
                    {
                        SOCPlayer player = cg.getPlayer(pn);

                        if ((player != null) && (player.getName() != null) && (!cg.isSeatVacant(pn)) && (!player.isRobot()))
                        {
                            gameHasHumanPlayer = true;

                            break;
                        }
                    }
                }

                //D.ebugPrintln("*** gameHasHumanPlayer = "+gameHasHumanPlayer+" for "+gm);

                /**
                 * check if there is at least one person watching the game
                 */
                if ((cg != null) && !gameHasHumanPlayer && !gameList.isGameEmpty(gm))
                {
                    Enumeration membersEnum = gameList.getMembers(gm).elements();

                    while (membersEnum.hasMoreElements())
                    {
                        StringConnection member = (StringConnection) membersEnum.nextElement();

                        //D.ebugPrintln("*** "+member.data+" is a member of "+gm);
                        boolean nameMatch = false;

                        for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++)
                        {
                            SOCPlayer player = cg.getPlayer(pn);

                            if ((player != null) && (player.getName() != null) && (player.getName().equals((String) member.getData())))
                            {
                                nameMatch = true;

                                break;
                            }
                        }

                        if (!nameMatch)
                        {
                            gameHasObserver = true;

                            break;
                        }
                    }
                }

                //D.ebugPrintln("*** gameHasObserver = "+gameHasObserver+" for "+gm);

                /**
                 * if the leaving member was playing the game, and
                 * it wasn't a robot, and the game isn't over, then...
                 */
                if (isPlayer && (gameHasHumanPlayer || gameHasObserver) && (cg != null) && (!cg.getPlayer(playerNumber).isRobot()) && (cg.getGameState() < SOCGame.OVER) && !(cg.getGameState() < SOCGame.START1A))
                {
                    /**
                     * get a robot to replace this player
                     */
                    boolean foundNoRobots = false;

                    messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, "Fetching a robot player..."));

                    if (robots.isEmpty())
                    {
                        messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, "Sorry, no robots on this server."));
                        foundNoRobots = true;
                    }
                    else
                    {
                        /**
                         * request a robot that isn't already playing this game or
                         * is not already requested to play in this game
                         */
                        boolean nameMatch = false;
                        StringConnection robotConn = null;

                        ///
                        /// shuffle the indexes to distribute load
                        ///
                        int[] robotIndexes = new int[robots.size()];

                        for (int i = 0; i < robots.size(); i++)
                        {
                            robotIndexes[i] = i;
                        }

                        for (int j = 0; j < 3; j++)
                        {
                            for (int i = 0; i < robotIndexes.length; i++) //
                            {
                                // Swap a random card below the ith robot with the ith robot
                                int idx = Math.abs(rand.nextInt() % (robotIndexes.length - i));
                                int tmp = robotIndexes[idx];
                                robotIndexes[idx] = robotIndexes[i];
                                robotIndexes[i] = tmp;
                            }
                        }

                        if (D.ebugOn)
                        {
                            for (int i = 0; i < robots.size(); i++)
                            {
                                D.ebugPrintln("^^^ robotIndexes[" + i + "]=" + robotIndexes[i]);
                            }
                        }

                        Vector requests = (Vector) robotJoinRequests.get(gm);

                        for (int idx = 0; idx < robots.size(); idx++)
                        {
                            robotConn = (StringConnection) robots.get(robotIndexes[idx]);
                            nameMatch = false;

                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                if (cg != null)
                                {
                                    SOCPlayer pl = cg.getPlayer(i);

                                    if (pl != null)
                                    {
                                        String pname = pl.getName();

                                        D.ebugPrintln("CHECKING " + (String) robotConn.getData() + " == " + pname);

                                        if ((pname != null) && (pname.equals((String) robotConn.getData())))
                                        {
                                            nameMatch = true;

                                            break;
                                        }
                                    }
                                }
                            }

                            if ((!nameMatch) && (requests != null))
                            {
                                Enumeration requestsEnum = requests.elements();

                                while (requestsEnum.hasMoreElements())
                                {
                                    StringConnection tempCon = (StringConnection) requestsEnum.nextElement();

                                    D.ebugPrintln("CHECKING " + robotConn + " == " + tempCon);

                                    if (tempCon == robotConn)
                                    {
                                        nameMatch = true;
                                    }

                                    break;
                                }
                            }

                            if (!nameMatch)
                            {
                                break;
                            }
                        }

                        if (!nameMatch && (cg != null))
                        {
                            /**
                             * make the request
                             */
                            D.ebugPrintln("@@@ JOIN GAME REQUEST for " + (String) robotConn.getData());

                            robotConn.put(SOCJoinGameRequest.toCmd(gm, playerNumber));

                            /**
                             * record the request
                             */
                            if (requests == null)
                            {
                                requests = new Vector();
                                requests.addElement(robotConn);
                                robotJoinRequests.put(gm, requests);
                            }
                            else
                            {
                                requests.addElement(robotConn);
                            }
                        }
                        else
                        {
                            messageToGameWithMon(gm, new SOCGameTextMsg(gm, SERVERNAME, "*** Can't find a robot! ***"));
                            foundNoRobots = true;
                        }
                    }
                    
                    if (foundNoRobots && (playerNumber == cg.getCurrentPlayerNumber()))
                    {
                        // Rare condition:
                        // No robot was found, but it was this player's turn.
                        // End their turn just to keep the game limping along.
                        // To prevent deadlock, we must release gamelist's monitor for
                        // this game before calling endGameTurn.
                        if (cg.canEndTurn(playerNumber))
                        {
                            gameList.releaseMonitorForGame(gm);
                            cg.takeMonitor();
                            endGameTurn(cg);
                            cg.releaseMonitor();
                            gameList.takeMonitorForGame(gm);
                        } else {
                            // Cannot end turn.
                            // TODO... error message? Force?
                        }
                    }
                }
            }

            /**
             * if the game has no players, or if they're all
             * robots, then end the game and write the data
             * to disk.
             */
            boolean emptyGame = false;
            emptyGame = gameList.isGameEmpty(gm);

            if (emptyGame || (!gameHasHumanPlayer && !gameHasObserver))
            {
                if (gameListLock)
                {
                    destroyGame(gm);
                }
                else
                {
                    gameList.takeMonitor();

                    try
                    {
                        destroyGame(gm);
                    }
                    catch (Exception e)
                    {
                        D.ebugPrintln("Exception in leaveGame (destroyGame) - " + e);
                    }

                    gameList.releaseMonitor();
                }

                gameDestroyed = true;
            }
        }

        //D.ebugPrintln("*** gameDestroyed = "+gameDestroyed+" for "+gm);
        return gameDestroyed;
    }

    /**
     * destroy the game
     *
     * WARNING: MUST HAVE THE gameList.takeMonitor() before
     * calling this method
     *
     * @param gm  the name of the game
     */
    public void destroyGame(String gm)
    {
        //D.ebugPrintln("***** destroyGame("+gm+")");
        SOCGame cg = null;

        cg = gameList.getGameData(gm);

        if (cg != null)
        {
            if (cg.getGameState() == SOCGame.OVER)
            {
                numberOfGamesFinished++;
            }

            ///
            /// write out game data
            ///

            /*
               currentGameEventRecord.setSnapshot(cg);
               saveCurrentGameEventRecord(gm);
               SOCGameRecord gr = (SOCGameRecord)gameRecords.get(gm);
               writeGameRecord(gm, gr);
             */

            //storeGameScores(cg);
            ///
            /// tell all robots to leave
            ///
            Vector members = null;
            members = gameList.getMembers(gm);

            if (members != null)
            {
                Enumeration conEnum = members.elements();

                while (conEnum.hasMoreElements())
                {
                    StringConnection con = (StringConnection) conEnum.nextElement();
                    con.put(SOCRobotDismiss.toCmd(gm));
                }
            }

            gameList.deleteGame(gm);
        }
    }

    /**
     * @return The names (Strings) of games on this server
     */
    public Enumeration getGameNames()
    {
        return gameList.getGames();
    }

    /**
     * Given a game name on this server, return its state.
     *
     * @param gm Game name
     * @return Game's state, or -1 if no game with that name on this server
     */
    public int getGameState(String gm)
    {
        SOCGame g = gameList.getGameData(gm);
        if (g != null)
            return g.getGameState();
        else
            return -1;
    }

    /**
     * the connection c leaves all channels it was in
     *
     * @param c  the connection
     * @return   the channels it was in
     */
    public Vector leaveAllChannels(StringConnection c)
    {
        if (c != null)
        {
            Vector ret = new Vector();
            Vector destroyed = new Vector();

            channelList.takeMonitor();

            try
            {
                for (Enumeration k = channelList.getChannels();
                        k.hasMoreElements();)
                {
                    String ch = (String) k.nextElement();

                    if (channelList.isMember(c, ch))
                    {
                        boolean thisChannelDestroyed = false;
                        channelList.takeMonitorForChannel(ch);

                        try
                        {
                            thisChannelDestroyed = leaveChannel(c, ch, true);
                        }
                        catch (Exception e)
                        {
                            D.ebugPrintln("Exception in leaveAllChannels (leaveChannel) - " + e);
                        }

                        channelList.releaseMonitorForChannel(ch);

                        if (thisChannelDestroyed)
                        {
                            destroyed.addElement(ch);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in leaveAllChannels - " + e);
            }

            channelList.releaseMonitor();

            /**
             * let everyone know about the destroyed channels
             */
            for (Enumeration de = destroyed.elements(); de.hasMoreElements();)
            {
                String ga = (String) de.nextElement();
                broadcast(SOCDeleteChannel.toCmd(ga));
            }

            return ret;
        }
        else
        {
            return null;
        }
    }

    /**
     * the connection c leaves all games it was in
     *
     * @param c  the connection
     * @return   the games it was in
     */
    public Vector leaveAllGames(StringConnection c)
    {
        if (c != null)
        {
            Vector ret = new Vector();
            Vector destroyed = new Vector();

            gameList.takeMonitor();

            try
            {
                for (Enumeration k = gameList.getGames(); k.hasMoreElements();)
                {
                    String ga = (String) k.nextElement();
                    Vector v = (Vector) gameList.getMembers(ga);

                    if (v.contains(c))
                    {
                        boolean thisGameDestroyed = false;
                        gameList.takeMonitorForGame(ga);

                        try
                        {
                            thisGameDestroyed = leaveGame(c, ga, true);
                        }
                        catch (Exception e)
                        {
                            D.ebugPrintln("Exception in leaveAllGames (leaveGame) - " + e);
                        }

                        gameList.releaseMonitorForGame(ga);

                        if (thisGameDestroyed)
                        {
                            destroyed.addElement(ga);
                        }

                        ret.addElement(ga);
                    }
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in leaveAllGames - " + e);
            }

            gameList.releaseMonitor();

            /**
             * let everyone know about the destroyed games
             */
            for (Enumeration de = destroyed.elements(); de.hasMoreElements();)
            {
                String ga = (String) de.nextElement();
                D.ebugPrintln("** Broadcasting SOCDeleteGame " + ga);
                broadcast(SOCDeleteGame.toCmd(ga));
            }

            return ret;
        }
        else
        {
            return null;
        }
    }

    /**
     * Send a message to the given channel
     *
     * @param ch  the name of the channel
     * @param mes the message to send
     */
    public void messageToChannel(String ch, SOCMessage mes)
    {
        channelList.takeMonitorForChannel(ch);

        try
        {
            Vector v = channelList.getMembers(ch);

            if (v != null)
            {
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection c = (StringConnection) menum.nextElement();

                    if (c != null)
                    {
                        c.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in messageToChannel - " + e);
        }

        channelList.releaseMonitorForChannel(ch);
    }

    /**
     * Send a message to the given channel
     *
     * WARNING: MUST HAVE THE gameList.takeMonitorForChannel(ch) before
     * calling this method
     *
     * @param ch  the name of the channel
     * @param mes the message to send
     */
    public void messageToChannelWithMon(String ch, SOCMessage mes)
    {
        Vector v = channelList.getMembers(ch);

        if (v != null)
        {
            Enumeration menum = v.elements();

            while (menum.hasMoreElements())
            {
                StringConnection c = (StringConnection) menum.nextElement();

                if (c != null)
                {
                    c.put(mes.toCmd());
                }
            }
        }
    }

    /**
     * Send a message to a player and record it
     *
     * @param c   the player connection
     * @param mes the message to send
     */
    public void messageToPlayer(StringConnection c, SOCMessage mes)
    {
        if ((c != null) && (mes != null))
        {
            //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", c.getData()));
            c.put(mes.toCmd());
        }
    }

    /**
     * Send a message to the given game
     *
     * @param ga  the name of the game
     * @param mes the message to send. If mes is a SOCGameTextMsg whose
     *            text begins with ">>>", the client should consider this
     *            an urgent message, and draw the user's attention in some way.
     *            (See {@link #messageToGameUrgent(String, String)})
     */
    public void messageToGame(String ga, SOCMessage mes)
    {
        gameList.takeMonitorForGame(ga);

        try
        {
            Vector v = gameList.getMembers(ga);

            if (v != null)
            {
                //D.ebugPrintln("M2G - "+mes);
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection c = (StringConnection) menum.nextElement();

                    if (c != null)
                    {
                        //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", c.getData()));
                        c.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in messageToGame - " + e);
        }

        gameList.releaseMonitorForGame(ga);
    }

    /**
     * Send a message to the given game
     *
     * WARNING: MUST HAVE THE gameList.takeMonitorForGame(ga) before
     * calling this method
     *
     * @param ga  the name of the game
     * @param mes the message to send
     */
    public void messageToGameWithMon(String ga, SOCMessage mes)
    {
        Vector v = gameList.getMembers(ga);

        if (v != null)
        {
            //D.ebugPrintln("M2G - "+mes);
            Enumeration menum = v.elements();

            while (menum.hasMoreElements())
            {
                StringConnection c = (StringConnection) menum.nextElement();

                if (c != null)
                {
                    //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", c.getData()));
                    c.put(mes.toCmd());
                }
            }
        }
    }

    /**
     * Send a message to all the connections in a game
     * excluding some.
     *
     * @param gn  the name of the game
     * @param ex  the list of exceptions
     * @param mes the message
     */
    public void messageToGameExcept(String gn, Vector ex, SOCMessage mes)
    {
        gameList.takeMonitorForGame(gn);

        try
        {
            Vector v = gameList.getMembers(gn);

            if (v != null)
            {
                //D.ebugPrintln("M2GE - "+mes);
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection con = (StringConnection) menum.nextElement();

                    if ((con != null) && (!ex.contains(con)))
                    {
                        //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", con.getData()));
                        con.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in messageToGameExcept - " + e);
        }

        gameList.releaseMonitorForGame(gn);
    }

    /**
     * Send a message to all the connections in a game
     * excluding one.
     *
     * @param gn  the name of the game
     * @param ex  the exception
     * @param mes the message
     */
    public void messageToGameExcept(String gn, StringConnection ex, SOCMessage mes)
    {
        gameList.takeMonitorForGame(gn);

        try
        {
            Vector v = gameList.getMembers(gn);

            if (v != null)
            {
                //D.ebugPrintln("M2GE - "+mes);
                Enumeration menum = v.elements();

                while (menum.hasMoreElements())
                {
                    StringConnection con = (StringConnection) menum.nextElement();
                    if ((con != null) && (con != ex))
                    {
                        //currentGameEventRecord.addMessageOut(new SOCMessageRecord(mes, "SERVER", con.getData()));
                        con.put(mes.toCmd());
                    }
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in messageToGameExcept - " + e);
        }

        gameList.releaseMonitorForGame(gn);
    }

    /**
     * Send an urgent SOCGameTextMsg to the given game.
     * An "urgent" message is a SOCGameTextMsg whose text
     * begins with ">>>"; the client should draw the user's
     * attention in some way.
     *<P>
     * Like messageToGame, will take and release the game's monitor.
     *
     * @param ga  the name of the game
     * @param mes the message to send. If mes does not begin with ">>>",
     *            will prepend ">>> " before sending mes.
     */
    public void messageToGameUrgent(String ga, String mes)
    {
        if (! mes.startsWith(">>>"))
            mes = ">>> " + mes;
        messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, mes));
    }

    /**
     * things to do when the connection c leaves
     *
     * @param c  the connection
     */
    public void leaveConnection(StringConnection c)
    {
        if (c != null)
        {
            leaveAllChannels(c);
            leaveAllGames(c);

            /**
             * if it is a robot, remove it from the list
             */
            robots.removeElement(c);
        }
    }

    /**
     * Things to do when a new connection comes.
     * If the connection is accepted, it's added to {@link #unnamedConns} until the
     * player "names" it by joining or creating a game under their player name.
     *  SYNCHRONIZATION NOTE: During the call to newConnection1, the monitor lock of
     *  {@link #unnamedConns} is held.  Thus, defer as much as possible until
     *  {@link #newConnection2(StringConnection)} (after the connection is accepted).
     *
     * @param c  the new Connection
     * @return true to accept and continue, false if you have rejected this connection
     *
     * @see #addConnection(StringConnection)
     * @see #newConnection2(StringConnection)
     * @see #nameConnection(StringConnection)
     */
    public boolean newConnection1(StringConnection c)
    {
        if (c != null)
        {
            /**
             * see if we are under the connection limit
             */
            try
            {
                if (this.connectionCount() >= maxConnections)
                {
                    SOCRejectConnection rcCommand = new SOCRejectConnection("Too many connections, please try another server.");
                    c.put(rcCommand.toCmd());
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("Caught exception in SOCServer.newConnection(Connection) - " + e);
                e.printStackTrace(System.out);
            }

            try
            {
                /**
                 * prevent someone from connecting twice from
                 * the same machine
                 * (Commented out: This is a bad idea due to proxies, NAT, etc.)
                 */
                boolean hostMatch = false;
                /*
                Enumeration allConnections = this.getConnections();

                   while(allConnections.hasMoreElements()) {
                   StringConnection tempCon = (StringConnection)allConnections.nextElement();
                   if (!(c.host().equals("pippen")) && (tempCon.host().equals(c.host()))) {
                   hostMatch = true;
                   break;
                   }
                   }
                 */
                if (hostMatch)
                {
                    SOCRejectConnection rcCommand = new SOCRejectConnection("Can't connect to the server more than once from one machine.");
                    c.put(rcCommand.toCmd());
                }
                else
                {
                    /**
                     * Accept this connection.
                     * Once it's added to the list, send the list of channels and games
                     * from {@link #newConnection2(StringConnection)}.
                     */
                    return true;
                }
            }
            catch (Exception e)
            {
                D.ebugPrintln("Caught exception in SOCServer.newConnection(Connection) - " + e);
                e.printStackTrace(System.out);
            }
        }

        return false;  // Not accepted
    }

    /** Send welcome messages (channel list, game list) when a new connection comes, part 2 -
     *  c has been accepted and added to a connection list.
     *  Unlike {@link #newConnection1(StringConnection)},
     *  no connection-list locks are held when this method is called.
     */
    protected void newConnection2(StringConnection c)
    {
        Vector cl = new Vector();
        channelList.takeMonitor();

        try
        {
            Enumeration clEnum = channelList.getChannels();

            while (clEnum.hasMoreElements())
            {
                cl.addElement(clEnum.nextElement());
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in newConnection (channelList) - " + e);
        }

        channelList.releaseMonitor();

        c.put(SOCChannels.toCmd(cl));

        Vector gl = new Vector();
        gameList.takeMonitor();

        try
        {
            Enumeration gaEnum = gameList.getGames();

            while (gaEnum.hasMoreElements())
            {
                gl.addElement(gaEnum.nextElement());
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in newConnection (gameList) - " + e);
        }

        gameList.releaseMonitor();
        c.put(SOCGames.toCmd(gl));

        /*
           gaEnum = gameList.getGames();
           int scores[] = new int[SOCGame.MAXPLAYERS];
           boolean robots[] = new boolean[SOCGame.MAXPLAYERS];
           while (gaEnum.hasMoreElements()) {
           String gameName = (String)gaEnum.nextElement();
           SOCGame theGame = gameList.getGameData(gameName);
           for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
           SOCPlayer player = theGame.getPlayer(i);
           if (player != null) {
           if (theGame.isSeatVacant(i)) {
           scores[i] = -1;
           robots[i] = false;
           } else {
           scores[i] = player.getPublicVP();
           robots[i] = player.isRobot();
           }
           } else {
           scores[i] = 0;
           }
           }
           c.put(SOCGameStats.toCmd(gameName, scores, robots));
           }
         */        
    }

    /**
     * check if a name is ok
     * a name is ok if it hasn't been used yet
     *
     * @param n  the name
     * @return   true if the name is ok
     */
    private boolean checkNickname(String n)
    {
        if (n.equals(SERVERNAME))
        {
            return false;
        }

        Enumeration connsEnum = getConnections();

        while (connsEnum.hasMoreElements())
        {
            StringConnection con = (StringConnection) connsEnum.nextElement();

            if ((con != null) && (n.equals((String) con.getData())))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Treat the incomming messages
     *
     * Note: When there is a choice, always use local information
     *       over information from the message.  For example, use
     *       the nickname from the connection to get the player
     *       information rather than the player information from
     *       the message.  This makes it harder to send false
     *       messages making players do things they didn't want
     *       to do.
     *
     * @param s    String containing the message
     * @param c    the Connection that sent the Message
     */
    public void processCommand(String s, StringConnection c)
    {
        try
        {
            SOCMessage mes = (SOCMessage) SOCMessage.toMsg(s);

            // D.ebugPrintln(c.getData()+" - "+mes);
            if (mes != null)
            {
                SOCGame ga;

                switch (mes.getType())
                {
                /**
                 * "join a channel" message
                 */
                case SOCMessage.JOIN:
                    handleJOIN(c, (SOCJoin) mes);

                    break;

                /**
                 * "leave a channel" message
                 */
                case SOCMessage.LEAVE:
                    handleLEAVE(c, (SOCLeave) mes);

                    break;

                /**
                 * "leave all channels" message
                 */
                case SOCMessage.LEAVEALL:
                    removeConnection(c);
                    removeConnectionCleanup(c);

                    break;

                /**
                 * text message
                 */
                case SOCMessage.TEXTMSG:

                    SOCTextMsg textMsgMes = (SOCTextMsg) mes;

                    if (c.getData().equals("debug"))
                    {
                        if (textMsgMes.getText().startsWith("*KILLCHANNEL*"))
                        {
                            messageToChannel(textMsgMes.getChannel(), new SOCTextMsg(textMsgMes.getChannel(), SERVERNAME, "********** " + (String) c.getData() + " KILLED THE CHANNEL **********"));
                            channelList.takeMonitor();

                            try
                            {
                                channelList.deleteChannel(textMsgMes.getChannel());
                            }
                            catch (Exception e)
                            {
                                D.ebugPrintln("Exception in KILLCHANNEL - " + e);
                            }

                            channelList.releaseMonitor();
                            broadcast(SOCDeleteChannel.toCmd(textMsgMes.getChannel()));
                        }
                        else
                        {
                            /**
                             * Send the message to the members of the channel
                             */
                            messageToChannel(textMsgMes.getChannel(), mes);
                        }
                    }
                    else
                    {
                        /**
                         * Send the message to the members of the channel
                         */
                        messageToChannel(textMsgMes.getChannel(), mes);
                    }

                    break;

                /**
                 * a robot has connected to this server
                 */
                case SOCMessage.IMAROBOT:
                    handleIMAROBOT(c, (SOCImARobot) mes);

                    break;

                /**
                 * text message from a game
                 */
                case SOCMessage.GAMETEXTMSG:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    SOCGameTextMsg gameTextMsgMes = (SOCGameTextMsg) mes;
                    recordGameEvent(gameTextMsgMes.getGame(), gameTextMsgMes.toCmd());

                    ga = gameList.getGameData(gameTextMsgMes.getGame());

                    //currentGameEventRecord.setSnapshot(ga);
                    ///
                    /// command to add time to a game
                    /// If the command text changes from '*ADDTIME*' to something else,
                    /// please update the warning text sent in checkForExpiredGames().
                    ///
                    if ((gameTextMsgMes.getText().startsWith("*ADDTIME*")) || (gameTextMsgMes.getText().startsWith("*addtime*")) || (gameTextMsgMes.getText().startsWith("ADDTIME")) || (gameTextMsgMes.getText().startsWith("addtime")))
                    {
                        SOCGame gameData = gameList.getGameData(gameTextMsgMes.getGame());

                        if (gameData != null)
                        {
                            // add 30 min. to the expiration time.  If this
                            // changes to another timespan, please update the
                            // warning text sent in checkForExpiredGames().
                            // Use ">>>" in messageToGame to mark as urgent.
                            gameData.setExpiration(gameData.getExpiration() + (30 * 60 * 1000));
                            messageToGameUrgent(gameTextMsgMes.getGame(), ">>> This game will expire in " + ((gameData.getExpiration() - System.currentTimeMillis()) / 60000) + " minutes.");
                        }
                    }

                    ///
                    /// Check the time remaining for this game
                    ///
                    if (gameTextMsgMes.getText().startsWith("*CHECKTIME*"))
                    {
                        SOCGame gameData = gameList.getGameData(gameTextMsgMes.getGame());
                        messageToGameUrgent(gameTextMsgMes.getGame(), ">>> This game will expire in " + ((gameData.getExpiration() - System.currentTimeMillis()) / 60000) + " minutes.");
                    }
                    else if (gameTextMsgMes.getText().startsWith("*WHO*"))
                    {
                        Vector gameMembers = null;
                        gameList.takeMonitorForGame(gameTextMsgMes.getGame());

                        try
                        {
                            gameMembers = gameList.getMembers(gameTextMsgMes.getGame());
                        }
                        catch (Exception e)
                        {
                            D.ebugPrintln("Exception in *WHO* (gameMembers) - " + e);
                        }

                        gameList.releaseMonitorForGame(gameTextMsgMes.getGame());

                        if (gameMembers != null)
                        {
                            Enumeration membersEnum = gameMembers.elements();

                            while (membersEnum.hasMoreElements())
                            {
                                StringConnection conn = (StringConnection) membersEnum.nextElement();
                                messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), SERVERNAME, "> " + conn.getData()));
                            }
                        }
                    }

                    //
                    // useful for debuging 
                    //
                    if (c.getData().equals("debug"))
                    {
                        final String msgText = gameTextMsgMes.getText();
                        if (gameTextMsgMes.getText().startsWith("rsrcs:"))
                        {
                            giveResources(gameTextMsgMes.getText(), ga);
                        }
                        else if (gameTextMsgMes.getText().startsWith("dev:"))
                        {
                            giveDevCard(gameTextMsgMes.getText(), ga);
                        }
                        else if (gameTextMsgMes.getText().charAt(0) == '*')
                        {
                            processDebugCommand(c, ga.getName(), msgText);
                        }
                        else
                        {
                            //
                            // Send the message to the members of the game
                            //
                            messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), (String) c.getData(), gameTextMsgMes.getText()));
                        }
                    }
                    else
                    {
                        //
                        // Send the message to the members of the game
                        //
                        messageToGame(gameTextMsgMes.getGame(), new SOCGameTextMsg(gameTextMsgMes.getGame(), (String) c.getData(), gameTextMsgMes.getText()));
                    }

                    //saveCurrentGameEventRecord(gameTextMsgMes.getGame());
                    break;

                /**
                 * "join a game" message
                 */
                case SOCMessage.JOINGAME:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleJOINGAME(c, (SOCJoinGame) mes);

                    //ga = (SOCGame)gamesData.get(((SOCJoinGame)mes).getGame());
                    //if (ga != null) {
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCJoinGame)mes).getGame());
                    //}
                    break;

                /**
                 * "leave a game" message
                 */
                case SOCMessage.LEAVEGAME:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleLEAVEGAME(c, (SOCLeaveGame) mes);

                    //ga = (SOCGame)gamesData.get(((SOCLeaveGame)mes).getGame());
                    //if (ga != null) {
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCLeaveGame)mes).getGame());
                    //}
                    break;

                /**
                 * someone wants to sit down
                 */
                case SOCMessage.SITDOWN:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleSITDOWN(c, (SOCSitDown) mes);

                    //ga = (SOCGame)gamesData.get(((SOCSitDown)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCSitDown)mes).getGame());
                    break;

                /**
                 * someone put a piece on the board
                 */
                case SOCMessage.PUTPIECE:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handlePUTPIECE(c, (SOCPutPiece) mes);

                    //ga = (SOCGame)gamesData.get(((SOCPutPiece)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCPutPiece)mes).getGame());
                    break;

                /**
                 * a player is moving the robber
                 */
                case SOCMessage.MOVEROBBER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleMOVEROBBER(c, (SOCMoveRobber) mes);

                    //ga = (SOCGame)gamesData.get(((SOCMoveRobber)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCMoveRobber)mes).getGame());
                    break;

                /**
                 * someone is starting a game
                 */
                case SOCMessage.STARTGAME:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleSTARTGAME(c, (SOCStartGame) mes);

                    //ga = (SOCGame)gamesData.get(((SOCStartGame)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCStartGame)mes).getGame());
                    break;

                case SOCMessage.ROLLDICE:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleROLLDICE(c, (SOCRollDice) mes);

                    //ga = (SOCGame)gamesData.get(((SOCRollDice)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCRollDice)mes).getGame());
                    break;

                case SOCMessage.DISCARD:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleDISCARD(c, (SOCDiscard) mes);

                    //ga = (SOCGame)gamesData.get(((SOCDiscard)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCDiscard)mes).getGame());
                    break;

                case SOCMessage.ENDTURN:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleENDTURN(c, (SOCEndTurn) mes);

                    //ga = (SOCGame)gamesData.get(((SOCEndTurn)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCEndTurn)mes).getGame());
                    break;

                case SOCMessage.CHOOSEPLAYER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleCHOOSEPLAYER(c, (SOCChoosePlayer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCChoosePlayer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCChoosePlayer)mes).getGame());
                    break;

                case SOCMessage.MAKEOFFER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleMAKEOFFER(c, (SOCMakeOffer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCMakeOffer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCMakeOffer)mes).getGame());
                    break;

                case SOCMessage.CLEAROFFER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleCLEAROFFER(c, (SOCClearOffer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCClearOffer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCClearOffer)mes).getGame());
                    break;

                case SOCMessage.REJECTOFFER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleREJECTOFFER(c, (SOCRejectOffer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCRejectOffer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCRejectOffer)mes).getGame());
                    break;

                case SOCMessage.ACCEPTOFFER:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleACCEPTOFFER(c, (SOCAcceptOffer) mes);

                    //ga = (SOCGame)gamesData.get(((SOCAcceptOffer)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCAcceptOffer)mes).getGame());
                    break;

                case SOCMessage.BANKTRADE:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleBANKTRADE(c, (SOCBankTrade) mes);

                    //ga = (SOCGame)gamesData.get(((SOCBankTrade)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCBankTrade)mes).getGame());
                    break;

                case SOCMessage.BUILDREQUEST:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleBUILDREQUEST(c, (SOCBuildRequest) mes);

                    //ga = (SOCGame)gamesData.get(((SOCBuildRequest)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCBuildRequest)mes).getGame());
                    break;

                case SOCMessage.CANCELBUILDREQUEST:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleCANCELBUILDREQUEST(c, (SOCCancelBuildRequest) mes);

                    //ga = (SOCGame)gamesData.get(((SOCCancelBuildRequest)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCCancelBuildRequest)mes).getGame());
                    break;

                case SOCMessage.BUYCARDREQUEST:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleBUYCARDREQUEST(c, (SOCBuyCardRequest) mes);

                    //ga = (SOCGame)gamesData.get(((SOCBuyCardRequest)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCBuyCardRequest)mes).getGame());
                    break;

                case SOCMessage.PLAYDEVCARDREQUEST:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handlePLAYDEVCARDREQUEST(c, (SOCPlayDevCardRequest) mes);

                    //ga = (SOCGame)gamesData.get(((SOCPlayDevCardRequest)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCPlayDevCardRequest)mes).getGame());
                    break;

                case SOCMessage.DISCOVERYPICK:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleDISCOVERYPICK(c, (SOCDiscoveryPick) mes);

                    //ga = (SOCGame)gamesData.get(((SOCDiscoveryPick)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCDiscoveryPick)mes).getGame());
                    break;

                case SOCMessage.MONOPOLYPICK:

                    //createNewGameEventRecord();
                    //currentGameEventRecord.setMessageIn(new SOCMessageRecord(mes, c.getData(), "SERVER"));
                    handleMONOPOLYPICK(c, (SOCMonopolyPick) mes);

                    //ga = (SOCGame)gamesData.get(((SOCMonopolyPick)mes).getGame());
                    //currentGameEventRecord.setSnapshot(ga);
                    //saveCurrentGameEventRecord(((SOCMonopolyPick)mes).getGame());
                    break;

                case SOCMessage.CHANGEFACE:
                    handleCHANGEFACE(c, (SOCChangeFace) mes);

                    break;

                case SOCMessage.SETSEATLOCK:
                    handleSETSEATLOCK(c, (SOCSetSeatLock) mes);

                    break;

                case SOCMessage.RESETBOARDREQUEST:
                    handleRESETBOARDREQUEST(c, (SOCResetBoardRequest) mes);

                    break;

                case SOCMessage.RESETBOARDVOTE:
                    handleRESETBOARDVOTE(c, (SOCResetBoardVote) mes);

                    break;

                case SOCMessage.CREATEACCOUNT:
                    handleCREATEACCOUNT(c, (SOCCreateAccount) mes);

                    break;
                }
            }
        }
        catch (Throwable e)
        {
            D.ebugPrintln("ERROR -> " + e);
            e.printStackTrace();
        }
    }

    /**
     * Process a debug command, sent by the "debug" client/player.
     */
    public void processDebugCommand(StringConnection debugCli, String ga, String dcmd)
    {
        if (dcmd.startsWith("*KILLGAME*"))
        {
            messageToGameUrgent(ga, ">>> ********** " + (String) debugCli.getData() + " KILLED THE GAME!!! ********** <<<");
            gameList.takeMonitor();

            try
            {
                destroyGame(ga);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in KILLGAME - " + e);
            }

            gameList.releaseMonitor();
            broadcast(SOCDeleteGame.toCmd(ga));
        }
        else if (dcmd.startsWith("*STATS*"))
        {
            long diff = System.currentTimeMillis() - startTime;
            long hours = diff / (60 * 60 * 1000);
            long minutes = (diff - (hours * 60 * 60 * 1000)) / (60 * 1000);
            long seconds = (diff - (hours * 60 * 60 * 1000) - (minutes * 60 * 1000)) / 1000;
            Runtime rt = Runtime.getRuntime();
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Uptime: " + hours + ":" + minutes + ":" + seconds));
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Total connections: " + numberOfConnections));
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Current connections: " + connectionCount()));
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Total Users: " + numberOfUsers));
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Games started: " + numberOfGamesStarted));
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Games finished: " + numberOfGamesFinished));
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Total Memory: " + rt.totalMemory()));
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Free Memory: " + rt.freeMemory()));
        }
        else if (dcmd.startsWith("*GC*"))
        {
            Runtime rt = Runtime.getRuntime();
            rt.gc();
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> GARBAGE COLLECTING DONE"));
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Free Memory: " + rt.freeMemory()));
        }
        else if (dcmd.startsWith("*STOP*"))
        {
            String stopMsg = ">>> ********** " + (String) debugCli.getData() + " KILLED THE SERVER!!! ********** <<<";
            stopServer(stopMsg);
            System.exit(0);
        }
        else if (dcmd.startsWith("*BCAST* "))
        {
            ///
            /// broadcast to all chat channels and games
            ///
            broadcast(SOCBCastTextMsg.toCmd(dcmd.substring(8)));
        }
        else if (dcmd.startsWith("*BOTLIST*"))
        {
            Enumeration robotsEnum = robots.elements();

            while (robotsEnum.hasMoreElements())
            {
                StringConnection robotConn = (StringConnection) robotsEnum.nextElement();
                messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> Robot: " + robotConn.getData()));
                robotConn.put(SOCAdminPing.toCmd((ga)));
            }
        }
        else if (dcmd.startsWith("*RESETBOT* "))
        {
            String botName = dcmd.substring(11).trim();
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> botName = '" + botName + "'"));

            Enumeration robotsEnum = robots.elements();

            while (robotsEnum.hasMoreElements())
            {
                StringConnection robotConn = (StringConnection) robotsEnum.nextElement();
                D.ebugPrintln("&&& '" + botName + "' == '" + robotConn.getData() + "' is " + (botName.equals((String) robotConn.getData())));

                if (botName.equals((String) robotConn.getData()))
                {
                    messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> SENDING RESET COMMAND TO " + botName));

                    SOCAdminReset resetCmd = new SOCAdminReset();
                    robotConn.put(resetCmd.toCmd());

                    break;
                }
            }
        }
        else if (dcmd.startsWith("*KILLBOT* "))
        {
            String botName = dcmd.substring(10).trim();
            messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> botName = '" + botName + "'"));

            Enumeration robotsEnum = robots.elements();

            while (robotsEnum.hasMoreElements())
            {
                StringConnection robotConn = (StringConnection) robotsEnum.nextElement();
                D.ebugPrintln("&&& '" + botName + "' == '" + robotConn.getData() + "' is " + (botName.equals((String) robotConn.getData())));

                if (botName.equals((String) robotConn.getData()))
                {
                    messageToGame(ga, new SOCGameTextMsg(ga, SERVERNAME, "> DISCONNECTING " + botName));
                    removeConnection(robotConn);
                    removeConnectionCleanup(robotConn);

                    break;
                }
            }
        }
    }

    /**
     * The server is being cleanly stopped.
     * Shut down with a final message "The game server is shutting down".
     */
    public synchronized void stopServer()
    {
        stopServer(">>> The game server is shutting down. <<<");
    }

    /**
     * The server is being cleanly stopped.  Send a final message, disconnect all
     * the connections, disconnect from database if connected.
     * Currently called only by the debug command "*STOP*",
     * and by SOCPlayerClient's locally hosted TCP server.
     *
     * @param stopMsg Final text message to send to all connected clients, or null.
     *         Will be sent as a {@link SOCBcastTextMsg}.
     *         As always, if message starts with ">>" it will be considered urgent.
     */
    public synchronized void stopServer(String stopMsg)
    {
        if (stopMsg != null)
        {
            broadcast(SOCBCastTextMsg.toCmd(stopMsg));
        }

        /// give time for messages to drain (such as urgent text messages
        /// about stopping the server)
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException ie)
        {
            Thread.yield();
        }

        /// now continue with shutdown
        try
        {
            SOCDBHelper.cleanup();
        }
        catch (SQLException x) { }
        
        super.stopServer();
    }

    /**
     * authenticate the user
     * see if the user is in the db, if so then check the password
     * if they're not in the db, but they supplied a password
     * then send a message
     * if they're not in the db, and no password, then ok
     *
     * @param c         the user's connection
     * @param userName  the user's nickname
     * @param password  the user's password
     * @return true if the user has been authenticated
     */
    private boolean authenticateUser(StringConnection c, String userName, String password)
    {
        String userPassword = null;

        try
        {
            userPassword = SOCDBHelper.getUserPassword(userName);
        }
        catch (SQLException sqle)
        {
            // Indicates a db problem: don't authenticate empty password
            c.put(SOCStatusMessage.toCmd("Problem connecting to database, please try again later."));
            return false;
        }

        if (userPassword != null)
        {
            if (!userPassword.equals(password))
            {
                c.put(SOCStatusMessage.toCmd("Incorrect password for '" + userName + "'."));

                return false;
            }
        }
        else if (!password.equals(""))
        {
            c.put(SOCStatusMessage.toCmd("No user with the nickname '" + userName + "' is registered with the system."));

            return false;
        }

        //
        // Update the last login time
        //
        //Date currentTime = new Date();

        //SOCDBHelper.updateLastlogin(userName, currentTime.getTime());
        //
        // Record the login info for this user
        //
        //SOCDBHelper.recordLogin(userName, c.host(), currentTime.getTime());
        return true;
    }

    /**
     * Handle the "join a channel" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleJOIN(StringConnection c, SOCJoin mes)
    {
        if (c != null)
        {
            D.ebugPrintln("handleJOIN: " + mes);

            /**
             * Check that the nickname is ok
             */
            if ((c.getData() == null) && (!checkNickname(mes.getNickname())))
            {
                c.put(SOCStatusMessage.toCmd(MSG_NICKNAME_ALREADY_IN_USE));

                return;
            }

            if ((c.getData() == null) && (!authenticateUser(c, mes.getNickname(), mes.getPassword())))
            {
                return;
            }

            /**
             * Check that the channel name is ok
             */

            /*
               if (!checkChannelName(mes.getChannel())) {
               return;
               }
             */
            if (c.getData() == null)
            {
                c.setData(mes.getNickname());
                nameConnection(c);
                numberOfUsers++;
            }

            /**
             * Tell the client that everything is good to go
             */
            c.put(SOCJoinAuth.toCmd(mes.getNickname(), mes.getChannel()));
            c.put(SOCStatusMessage.toCmd("Welcome to Java Settlers of Catan!"));

            /**
             * Add the StringConnection to the channel
             */
            String ch = mes.getChannel();

            if (channelList.takeMonitorForChannel(ch))
            {
                try
                {
                    connectToChannel(c, ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleJOIN (connectToChannel) - " + e);
                }

                channelList.releaseMonitorForChannel(ch);
            }
            else
            {
                /**
                 * the channel did not exist, create it
                 */
                channelList.takeMonitor();

                try
                {
                    channelList.createChannel(ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleJOIN (createChannel) - " + e);
                }

                channelList.releaseMonitor();
                broadcast(SOCNewChannel.toCmd(ch));
                c.put(SOCMembers.toCmd(ch, channelList.getMembers(ch)));
                D.ebugPrintln("*** " + c.getData() + " joined the channel " + ch);
                channelList.takeMonitorForChannel(ch);

                try
                {
                    channelList.addMember(c, ch);
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleJOIN (addMember) - " + e);
                }

                channelList.releaseMonitorForChannel(ch);
            }

            /**
             * let everyone know about the change
             */
            messageToChannel(ch, new SOCJoin(mes.getNickname(), "", "dummyhost", ch));
        }
    }

    /**
     * Handle the "leave a channel" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleLEAVE(StringConnection c, SOCLeave mes)
    {
        D.ebugPrintln("handleLEAVE: " + mes);

        if (c != null)
        {
            boolean destroyedChannel = false;
            channelList.takeMonitorForChannel(mes.getChannel());

            try
            {
                destroyedChannel = leaveChannel(c, mes.getChannel(), false);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in handleLEAVE - " + e);
            }

            channelList.releaseMonitorForChannel(mes.getChannel());

            if (destroyedChannel)
            {
                broadcast(SOCDeleteChannel.toCmd(mes.getChannel()));
            }
        }
    }

    /**
     * Handle the "I'm a robot" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleIMAROBOT(StringConnection c, SOCImARobot mes)
    {
        if (c != null)
        {
            /**
             * Check that the nickname is ok
             */
            if ((c.getData() == null) && (!checkNickname(mes.getNickname())))
            {
                c.put(SOCStatusMessage.toCmd(MSG_NICKNAME_ALREADY_IN_USE));
                SOCRejectConnection rcCommand = new SOCRejectConnection(MSG_NICKNAME_ALREADY_IN_USE);
                c.put(rcCommand.toCmd());
                System.err.println("Robot login attempt, name already in use: " + mes.getNickname());
                // c.disconnect();

                return;
            }

            SOCRobotParameters params = null;
            //
            // send the current robot parameters
            //
            try
            {
                params = SOCDBHelper.retrieveRobotParams(mes.getNickname());
                D.ebugPrintln("*** Robot Parameters for " + mes.getNickname() + " = " + params);
            }
            catch (SQLException sqle)
            {
                System.err.println("Error retrieving robot parameters from db: Using defaults.");
            }

            if (params == null)
            {
                params = new SOCRobotParameters(ROBOT_PARAMS_DEFAULT);
            }

            c.put(SOCUpdateRobotParams.toCmd(params));

            //
            // add this connection to the robot list
            //
            c.setData(mes.getNickname());
            robots.addElement(c);
            nameConnection(c);
        }
    }

    /**
     * Handle the "join a game" message.
     * Will join the game, or return a STATUSMESSAGE if nickname is not OK.
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleJOINGAME(StringConnection c, SOCJoinGame mes)
    {
        if (c != null)
        {
            D.ebugPrintln("handleJOINGAME: " + mes);

            /**
             * Check that the nickname is ok
             */
            if ((c.getData() == null) && (!checkNickname(mes.getNickname())))
            {
                c.put(SOCStatusMessage.toCmd(MSG_NICKNAME_ALREADY_IN_USE));

                return;
            }

            if ((c.getData() == null) && (!authenticateUser(c, mes.getNickname(), mes.getPassword())))
            {
                return;
            }

            if (c != null)
            {
                if (c.getData() == null)
                {
                    c.setData(mes.getNickname());
                    nameConnection(c);
                    numberOfUsers++;
                }
            }

            /**
             * Check that the game name is ok
             */

            /*
               if (!checkGameName(mes.getGame())) {
               return;
               }
             */

            /**
             * Tell the client that everything is good to go
             */
            if (connectToGame(c, mes.getGame()))
            {
                String gameName = mes.getGame();

                /**
                 * send the entire state of the game to client,
                 * send client join event to other players
                 */
                SOCGame gameData = gameList.getGameData(gameName);

                if (gameData != null)
                {
                    joinGame(gameData, c, false);
                }
            }
        }
    }

    /**
     * Handle the "leave game" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleLEAVEGAME(StringConnection c, SOCLeaveGame mes)
    {
        if (c != null)
        {
            boolean isMember = false;
            final String gaName = mes.getGame();
            gameList.takeMonitorForGame(gaName);

            try
            {
                isMember = gameList.isMember(c, gaName);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in handleLEAVEGAME (isMember) - " + e);
            }

            gameList.releaseMonitorForGame(gaName);

            if (isMember)
            {
                boolean gameDestroyed = false;
                gameList.takeMonitorForGame(gaName);

                try
                {
                    gameDestroyed = leaveGame(c, gaName, false);
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleLEAVEGAME (leaveGame) - " + e);
                }

                gameList.releaseMonitorForGame(gaName);

                if (gameDestroyed)
                {
                    broadcast(SOCDeleteGame.toCmd(gaName));
                }
                else
                {
                    /*
                       SOCLeaveGame leaveMessage = new SOCLeaveGame((String)c.getData(), c.host(), mes.getGame());
                       messageToGame(mes.getGame(), leaveMessage);
                       recordGameEvent(mes.getGame(), leaveMessage.toCmd());
                     */
                }

                /**
                 * if it's a robot, remove it from the request list
                 */
                Vector requests = (Vector) robotDismissRequests.get(gaName);

                if (requests != null)
                {
                    Enumeration reqEnum = requests.elements();
                    SOCReplaceRequest req = null;

                    while (reqEnum.hasMoreElements())
                    {
                        SOCReplaceRequest tempReq = (SOCReplaceRequest) reqEnum.nextElement();

                        if (tempReq.getLeaving() == c)
                        {
                            req = tempReq;

                            break;
                        }
                    }

                    if (req != null)
                    {
                        requests.removeElement(req);

                        /**
                         * let the person replacing the robot sit down
                         */
                        SOCGame ga = gameList.getGameData(gaName);
                        sitDown(ga, req.getArriving(), req.getSitDownMessage().getPlayerNumber(), req.getSitDownMessage().isRobot(), false);
                    }
                }
            }
        }
    }

    /**
     * handle "sit down" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleSITDOWN(StringConnection c, SOCSitDown mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                /**
                 * make sure this player isn't already sitting
                 */
                boolean canSit = true;

                /*
                   for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
                   if (ga.getPlayer(i).getName() == (String)c.getData()) {
                   canSit = false;
                   break;
                   }
                   }
                 */
                //D.ebugPrintln("ga.isSeatVacant(mes.getPlayerNumber()) = "+ga.isSeatVacant(mes.getPlayerNumber()));
                /**
                 * make sure a person isn't sitting here already
                 */
                ga.takeMonitor();

                try
                {
                    if (!ga.isSeatVacant(mes.getPlayerNumber()))
                    {
                        SOCPlayer seatedPlayer = ga.getPlayer(mes.getPlayerNumber());

                        if (seatedPlayer.isRobot() && (!ga.isSeatLocked(mes.getPlayerNumber())) && (ga.getCurrentPlayerNumber() != mes.getPlayerNumber()))
                        {
                            /**
                             * boot the robot out of the game
                             */
                            StringConnection robotCon = null;
                            Enumeration conEnum = conns.elements();

                            while (conEnum.hasMoreElements())
                            {
                                StringConnection con = (StringConnection) conEnum.nextElement();

                                if (seatedPlayer.getName().equals((String) con.getData()))
                                {
                                    robotCon = con;

                                    break;
                                }
                            }

                            robotCon.put(SOCRobotDismiss.toCmd(mes.getGame()));

                            /**
                             * this connection has to wait for the robot to leave
                             * and then it can sit down
                             */
                            Vector disRequests = (Vector) robotDismissRequests.get(mes.getGame());
                            SOCReplaceRequest req = new SOCReplaceRequest(c, robotCon, mes);

                            if (disRequests == null)
                            {
                                disRequests = new Vector();
                                disRequests.addElement(req);
                                robotDismissRequests.put(mes.getGame(), disRequests);
                            }
                            else
                            {
                                disRequests.addElement(req);
                            }
                        }

                        canSit = false;
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleSITDOWN - " + e);
                }

                ga.releaseMonitor();

                /**
                 * if this is a robot, remove it from the request list
                 */
                Vector joinRequests = (Vector) robotJoinRequests.get(mes.getGame());

                if (joinRequests != null)
                {
                    joinRequests.removeElement(c);
                }

                //D.ebugPrintln("canSit 2 = "+canSit);
                if (canSit)
                {
                    sitDown(ga, c, mes.getPlayerNumber(), mes.isRobot(), false);
                }
                else
                {
                    /**
                     * if the robot can't sit, tell it to go away
                     */
                    if (mes.isRobot())
                    {
                        c.put(SOCRobotDismiss.toCmd(mes.getGame()));
                    }
                }
            }
        }
    }

    /**
     * handle "put piece" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handlePUTPIECE(StringConnection c, SOCPutPiece mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCPlayer player = ga.getPlayer((String) c.getData());

                    /**
                     * make sure the player can do it
                     */
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        boolean sendDenyReply = false;
                        /*
                           if (D.ebugOn) {
                           D.ebugPrintln("BEFORE");
                           for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                           SOCPlayer tmpPlayer = ga.getPlayer(pn);
                           D.ebugPrintln("Player # "+pn);
                           for (int i = 0x22; i < 0xCC; i++) {
                           if (tmpPlayer.isPotentialRoad(i))
                           D.ebugPrintln("### POTENTIAL ROAD AT "+Integer.toHexString(i));
                           }
                           }
                           }
                         */
                        switch (mes.getPieceType())
                        {
                        case SOCPlayingPiece.ROAD:

                            SOCRoad rd = new SOCRoad(player, mes.getCoordinates());

                            if ((ga.getGameState() == SOCGame.START1B) || (ga.getGameState() == SOCGame.START2B) || (ga.getGameState() == SOCGame.PLACING_ROAD) || (ga.getGameState() == SOCGame.PLACING_FREE_ROAD1) || (ga.getGameState() == SOCGame.PLACING_FREE_ROAD2))
                            {
                                if (player.isPotentialRoad(mes.getCoordinates()))
                                {
                                    ga.putPiece(rd);  // Changes state and sometimes player

                                    /*
                                       if (D.ebugOn) {
                                       D.ebugPrintln("AFTER");
                                       for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++) {
                                       SOCPlayer tmpPlayer = ga.getPlayer(pn);
                                       D.ebugPrintln("Player # "+pn);
                                       for (int i = 0x22; i < 0xCC; i++) {
                                       if (tmpPlayer.isPotentialRoad(i))
                                       D.ebugPrintln("### POTENTIAL ROAD AT "+Integer.toHexString(i));
                                       }
                                       }
                                       }
                                     */
                                    gameList.takeMonitorForGame(gaName);
                                    messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, (String) c.getData() + " built a road."));
                                    messageToGameWithMon(gaName, new SOCPutPiece(mes.getGame(), player.getPlayerNumber(), SOCPlayingPiece.ROAD, mes.getCoordinates()));
                                    gameList.releaseMonitorForGame(gaName);
                                    boolean toldRoll = sendGameState(ga, false);
                                    broadcastGameStats(ga);

                                    if (!checkTurn(c, ga))
                                    {                                        
                                        // Player changed (or play started), announce new player.
                                        sendTurn(ga, true);
                                    }
                                    else if (toldRoll)
                                    {
                                        // When play starts, or after placing 2nd free road,
                                        // announce even though player unchanged,
                                        // to trigger auto-roll for the player.
                                        messageToGame(gaName, new SOCRollDicePrompt (gaName, player.getPlayerNumber()));
                                    }
                                }
                                else
                                {
                                    D.ebugPrintln("ILLEGAL ROAD");
                                    c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a road there."));
                                    sendDenyReply = true;                                   
                                }
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a road right now."));
                            }

                            break;

                        case SOCPlayingPiece.SETTLEMENT:

                            SOCSettlement se = new SOCSettlement(player, mes.getCoordinates());

                            if ((ga.getGameState() == SOCGame.START1A) || (ga.getGameState() == SOCGame.START2A) || (ga.getGameState() == SOCGame.PLACING_SETTLEMENT))
                            {
                                if (player.isPotentialSettlement(mes.getCoordinates()))
                                {
                                    ga.putPiece(se);   // Changes game state and (if game start) player
                                    gameList.takeMonitorForGame(gaName);
                                    messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, (String) c.getData() + " built a settlement."));
                                    messageToGameWithMon(gaName, new SOCPutPiece(mes.getGame(), player.getPlayerNumber(), SOCPlayingPiece.SETTLEMENT, mes.getCoordinates()));
                                    gameList.releaseMonitorForGame(gaName);
                                    broadcastGameStats(ga);
                                    sendGameState(ga);

                                    if (!checkTurn(c, ga))
                                    {
                                        sendTurn(ga, false);  // Announce new current player.
                                    }
                                }
                                else
                                {
                                    D.ebugPrintln("ILLEGAL SETTLEMENT");
                                    c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a settlement there."));
                                    sendDenyReply = true;
                                }
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a settlement right now."));
                            }

                            break;

                        case SOCPlayingPiece.CITY:

                            SOCCity ci = new SOCCity(player, mes.getCoordinates());

                            if (ga.getGameState() == SOCGame.PLACING_CITY)
                            {
                                if (player.isPotentialCity(mes.getCoordinates()))
                                {
                                    ga.putPiece(ci);  // changes game state and maybe player
                                    gameList.takeMonitorForGame(gaName);
                                    messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, (String) c.getData() + " built a city."));
                                    messageToGameWithMon(gaName, new SOCPutPiece(mes.getGame(), player.getPlayerNumber(), SOCPlayingPiece.CITY, mes.getCoordinates()));
                                    gameList.releaseMonitorForGame(gaName);
                                    broadcastGameStats(ga);
                                    sendGameState(ga);

                                    if (!checkTurn(c, ga))
                                    {
                                        sendTurn(ga, false);  // announce new current player
                                    }
                                }
                                else
                                {
                                    D.ebugPrintln("ILLEGAL CITY");
                                    c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a city there."));
                                    sendDenyReply = true;
                                }
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a city right now."));
                            }

                            break;
                        
                        }  // switch (mes.getPieceType())
                        
                        if (sendDenyReply)
                        {
                            messageToPlayer(c, new SOCCancelBuildRequest(gaName, mes.getPieceType()));
                        }                       
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "move robber" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleMOVEROBBER(StringConnection c, SOCMoveRobber mes)
    {
        if (c != null)
        {
            String gn = mes.getGame();
            SOCGame ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCPlayer player = ga.getPlayer((String) c.getData());

                    /**
                     * make sure the player can do it
                     */
                    final String gaName = ga.getName();
                    if (ga.canMoveRobber(player.getPlayerNumber(), mes.getCoordinates()))
                    {
                        SOCMoveRobberResult result = ga.moveRobber(player.getPlayerNumber(), mes.getCoordinates());
                        messageToGame(gaName, new SOCMoveRobber(gaName, player.getPlayerNumber(), mes.getCoordinates()));

                        Vector victims = result.getVictims();

                        /** only one possible victim */
                        if (victims.size() == 1)
                        {
                            /**
                             * report what was stolen
                             */
                            SOCPlayer victim = (SOCPlayer) victims.firstElement();
                            reportRobbery(ga, player, victim, result.getLoot());
                        }
                        /** no victim */
                        else if (victims.size() == 0)
                        {
                            /**
                             * just say it was moved; nothing is stolen
                             */
                            messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, (String) c.getData() + " moved the robber."));
                        }
                        else
                        {
                            /**
                             * else, the player needs to choose a victim
                             */
                            messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, (String) c.getData() + " moved the robber, must choose a victim."));                            
                        }

                        sendGameState(ga);
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't move the robber."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "start game" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleSTARTGAME(StringConnection c, SOCStartGame mes)
    {
        if (c != null)
        {
            String gn = mes.getGame();
            SOCGame ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (ga.getGameState() == SOCGame.NEW)
                    {
                        boolean seatsFull = true;
                        boolean anyLocked = false;
                        int numEmpty = 0;
                        int numPlayers = 0;

                        //
                        // count the number of empty seats
                        //
                        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                        {
                            if (ga.isSeatVacant(i))
                            {
                                if (ga.isSeatLocked(i))
                                {
                                    anyLocked = true;
                                }
                                else
                                {
                                    seatsFull = false;
                                    ++numEmpty;
                                }
                            }
                            else
                            {
                                ++numPlayers;
                            }
                        }
                        
                        if (seatsFull && (numPlayers < 2))
                        {
                            seatsFull = false;
                            numEmpty = 3;
                            String m = "Sorry, the only player cannot lock all seats.";
                            messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, m));
                        }
                        else if (!seatsFull)
                        {
                            if (robots.isEmpty()) 
                            {                                
                                if (numPlayers < SOCGame.MINPLAYERS)
                                {
                                    messageToGame(gn, new SOCGameTextMsg
                                        (gn, SERVERNAME, "No robots on this server, please fill at least "
                                         + SOCGame.MINPLAYERS + " seats before starting." ));
                                }
                                else
                                {
                                    seatsFull = true;  // Enough players to start game.
                                }
                            }
                            else
                            {
                                //
                                // make sure there are enough robots connected
                                //
                                if (numEmpty > robots.size())
                                {
                                    String m;
                                    if (anyLocked)
                                        m = "Sorry, not enough robots to fill all the seats.  Only " + robots.size() + " robots are available.";
                                    else
                                        m = "Sorry, not enough robots to fill all the seats.  Lock some seats.  Only " + robots.size() + " robots are available.";
                                    messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, m));
                                }
                                else
                                {
                                    ga.setGameState(SOCGame.READY);

                                    /**
                                     * Fill all the unlocked empty seats with robots.
                                     * Build a Vector of StringConnections of robots asked
                                     * to join, and add it to the robotJoinRequests table.
                                     */
                                    readyGameAskRobotsJoin(ga, null);
                                }
                            }
                        }

                        /**
                         * If this doesn't need robots, then start the game.
                         * Otherwise wait for them to sit before starting the game.
                         */
                        if (seatsFull)
                        {
                            startGame(ga);
                        }
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * Fill all the unlocked empty seats with robots.
     * Builds a Vector of StringConnections of robots asked to join,
     * and adds it to the robotJoinRequests table.
     * Game state should be READY.
     * Called by handleSTARTGAME, resetBoardAndNotify.
     * MUST be called ONLY from the one thread handling all inbound messages,
     * or (race condition) the robots may ask to join before we've
     * set up robotJoinRequests. 
     *
     * @param ga  Game to ask robots to join
     * @param robotSeats If robotSeats is null, robots are randomly selected.
     *                   If non-null, a MAXPLAYERS-sized array of StringConnections.
     *                   Any vacant non-locked seat, with index i,
     *                   is filled with the robot whose connection is robotSeats[i].
     *                   Other indexes should be null, and won't be used.
     *
     * @throws IllegalStateException if ga.gamestate is not READY
     * @throws IllegalArgumentException if robotSeats is not null but wrong length,
     *           or if a robotSeat element is null but that seat wants a robot (vacant non-locked).
     */
    private void readyGameAskRobotsJoin(SOCGame ga, StringConnection[] robotSeats)
        throws IllegalStateException, IllegalArgumentException
    {
        if (ga.getGameState() != SOCGame.READY)
            throw new IllegalStateException("SOCGame state not READY: " + ga.getGameState());

        Vector robotRequests = null;

        int[] robotIndexes = null;
        if (robotSeats == null)
        {
            ///
            /// shuffle the indexes to distribute load
            ///
            robotIndexes = new int[robots.size()];
    
            for (int i = 0; i < robots.size(); i++)
            {
                robotIndexes[i] = i;
            }
    
            for (int j = 0; j < 3; j++)
            {
                for (int i = 0; i < robotIndexes.length; i++)
                {
                    int idx = Math.abs(rand.nextInt() % (robotIndexes.length - i));
                    int tmp = robotIndexes[idx];
                    robotIndexes[idx] = robotIndexes[i];
                    robotIndexes[i] = tmp;
                }
            }
    
            if (D.ebugOn)
            {
                for (int i = 0; i < robots.size();
                        i++)
                {
                    D.ebugPrintln("^^^ robotIndexes[" + i + "]=" + robotIndexes[i]);
                }
            }
        }
        else
        {
            // robotSeats not null: check length
            if (robotSeats.length != SOCGame.MAXPLAYERS)
                throw new IllegalArgumentException("robotSeats Length must be MAXPLAYERS");
        }

        String gname = ga.getName();
        int idx = 0;

        for (int i = 0; i < SOCGame.MAXPLAYERS;
                i++)
        {
            if (ga.isSeatVacant(i) && ! ga.isSeatLocked(i))
            {
                /**
                 * fetch a robot player
                 */
                if (idx < robots.size())
                {
                    messageToGame(gname, new SOCGameTextMsg(gname, SERVERNAME, "Fetching a robot player..."));

                    StringConnection robotConn;
                    if (robotSeats != null)
                    {
                        robotConn = robotSeats[i];
                        if (robotConn == null)
                            throw new IllegalArgumentException("robotSeats[" + i + "] was needed but null");
                    }
                    else
                    {
                        robotConn = (StringConnection) robots.get(robotIndexes[idx]);
                    }
                    idx++;

                    /**
                     * make the request
                     */
                    robotConn.put(SOCJoinGameRequest.toCmd(gname, i));

                    /**
                     * record the request
                     */
                    D.ebugPrintln("@@@ JOIN GAME REQUEST for " + (String) robotConn.getData());
                    if (robotRequests == null)
                        robotRequests = new Vector();
                    robotRequests.addElement(robotConn);
                }
            }
        }

        if (robotRequests != null)
        {
            // we know it isn't empty,
            // so add to the request table
            robotJoinRequests.put(gname, robotRequests);
        }
    }

    /**
     * handle "roll dice" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleROLLDICE(StringConnection c, SOCRollDice mes)
    {
        if (c != null)
        {
            final String gn = mes.getGame();
            SOCGame ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (ga.canRollDice(ga.getPlayer((String) c.getData()).getPlayerNumber()))
                    {
                        IntPair dice = ga.rollDice();
                        /**
                         * Send roll results and then text to client.
                         * Client expects to see DiceResult first, then text message;
                         * to reduce visual clutter, SOCPlayerInterface.print
                         * expects text message to follow a certain format.
                         * If a 7 is rolled, sendGameState will also say who must discard
                         * (in a GAMETEXTMSG).
                         */
                        messageToGame(gn, new SOCDiceResult(gn, ga.getCurrentDice()));
                        messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, (String) c.getData() + " rolled a " + dice.getA() + " and a " + dice.getB() + "."));
                        sendGameState(ga);  // For 7, give visual feedback before sending discard request

                        /**
                         * if the roll is not 7, tell players what they got
                         */
                        if (ga.getCurrentDice() != 7)
                        {
                            boolean noPlayersGained = true;
                            StringBuffer gainsText = new StringBuffer();

                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                if (! ga.isSeatVacant(i))
                                {
                                    SOCResourceSet rsrcs = ga.getResourcesGainedFromRoll(ga.getPlayer(i), ga.getCurrentDice());
    
                                    if (rsrcs.getTotal() != 0)
                                    {
                                        if (noPlayersGained)
                                        {
                                            noPlayersGained = false;
                                        }
                                        else
                                        {
                                            gainsText.append(" ");
                                        }

                                        gainsText.append(ga.getPlayer(i).getName());
                                        gainsText.append(" gets ");
                                        // Send SOCPlayerElement messages,
                                        // build resource-text in gainsText.
                                        reportRolledResources(gn, rsrcs, i, gainsText);
                                        gainsText.append(".");
                                    }

                                    //
                                    //  send all resource info for accuracy
                                    //
                                    StringConnection playerCon = null;
                                    Enumeration conEnum = conns.elements();

                                    while (conEnum.hasMoreElements())
                                    {
                                        StringConnection con = (StringConnection) conEnum.nextElement();

                                        if (ga.getPlayer(i).getName().equals((String) con.getData()))
                                        {
                                            playerCon = con;

                                            break;
                                        }
                                    }

                                    if (playerCon != null)
                                    {
                                        SOCResourceSet resources = ga.getPlayer(i).getResources();
                                        messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.CLAY, resources.getAmount(SOCPlayerElement.CLAY)));
                                        messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.ORE, resources.getAmount(SOCPlayerElement.ORE)));
                                        messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.SHEEP, resources.getAmount(SOCPlayerElement.SHEEP)));
                                        messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.WHEAT, resources.getAmount(SOCPlayerElement.WHEAT)));
                                        messageToPlayer(playerCon, new SOCPlayerElement(ga.getName(), i, SOCPlayerElement.SET, SOCPlayerElement.WOOD, resources.getAmount(SOCPlayerElement.WOOD)));
                                        messageToGame(ga.getName(), new SOCResourceCount(ga.getName(), i, resources.getTotal()));
                                    }
                                }  // if (! ga.isSeatVacant(i))
                            }  // for (i)

                            String message;
                            if (noPlayersGained)
                            {
                                message = "No player gets anything.";
                            }
                            else
                            {
                                message = gainsText.toString();
                            }
                            messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, message));

                            /*
                               if (D.ebugOn) {
                               for (int i=0; i < SOCGame.MAXPLAYERS; i++) {
                               SOCResourceSet rsrcs = ga.getPlayer(i).getResources();
                               String resourceMessage = "PLAYER "+i+" RESOURCES: ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.CLAY)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.ORE)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.SHEEP)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.WHEAT)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.WOOD)+" ";
                               resourceMessage += rsrcs.getAmount(SOCResourceConstants.UNKNOWN)+" ";
                               messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, resourceMessage));
                               }
                               }
                             */
                        }
                        else
                        {
                            /**
                             * player rolled 7
                             */
                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                if (( ! ga.isSeatVacant(i))
                                    && (ga.getPlayer(i).getResources().getTotal() > 7))
                                {
                                    Enumeration coEnum = getConnections();

                                    while (coEnum.hasMoreElements())
                                    {
                                        StringConnection con = (StringConnection) coEnum.nextElement();

                                        if (ga.getPlayer(i).getName().equals((String) con.getData()))
                                        {
                                            // Request discard half (round down)
                                            con.put(SOCDiscardRequest.toCmd(ga.getName(), ga.getPlayer(i).getResources().getTotal() / 2));

                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gn, SERVERNAME, "You can't roll right now."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception in handleROLLDICE - " + e);
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "discard" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleDISCARD(StringConnection c, SOCDiscard mes)
    {
        if (c != null)
        {
            final String gn = mes.getGame();
            SOCGame ga = gameList.getGameData(gn);

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCPlayer player = ga.getPlayer((String) c.getData());

                    if (ga.canDiscard(player.getPlayerNumber(), mes.getResources()))
                    {
                        ga.discard(player.getPlayerNumber(), mes.getResources());

                        /**
                         * tell the player client that the player discarded the resources
                         */
                        int cl, or, sh, wh, wo;
                        cl = mes.getResources().getAmount(SOCResourceConstants.CLAY);
                        or = mes.getResources().getAmount(SOCResourceConstants.ORE);
                        sh = mes.getResources().getAmount(SOCResourceConstants.SHEEP);
                        wh = mes.getResources().getAmount(SOCResourceConstants.WHEAT);
                        wo = mes.getResources().getAmount(SOCResourceConstants.WOOD);

                        if (cl > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, cl));
                        }

                        if (or > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, or));
                        }

                        if (sh > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, sh));
                        }

                        if (wh > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, wh));
                        }

                        if (wo > 0)
                        {
                            messageToPlayer(c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, wo));
                        }

                        /**
                         * tell everyone else that the player discarded unknown resources
                         */
                        messageToGameExcept(gn, c, new SOCPlayerElement(gn, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.UNKNOWN, mes.getResources().getTotal()));
                        messageToGame(gn, new SOCGameTextMsg(gn, SERVERNAME, (String) c.getData() + " discarded " + mes.getResources().getTotal() + " resources."));

                        /**
                         * send the new state, or end turn if forced
                         */
                        if ((ga.getGameState() != SOCGame.PLAY1) || ! ga.isForcingEndTurn())
                        {
                            sendGameState(ga);
                        } else {
                            endGameTurn(ga);  // already did ga.takeMonitor()
                        }
                    }
                    else
                    {
                        /**
                         * there could be a better feedback message here
                         */
                        c.put(SOCGameTextMsg.toCmd(gn, SERVERNAME, "You can't discard that many cards."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "end turn" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleENDTURN(StringConnection c, SOCEndTurn mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                final String gname = ga.getName();               
                ga.takeMonitor();

                try
                {
                    if (ga.getGameState() == SOCGame.OVER)
                    {
                        // Should not happen; is here just in case.
                        SOCPlayer pl = ga.getPlayer((String) c.getData());
                        String msg = ga.gameOverMessageToPlayer(pl);
                            // msg = "The game is over; you are the winner!";
                            // msg = "The game is over; <someone> won.";
                            // msg = "The game is over; no one won.";
                        c.put(SOCGameTextMsg.toCmd(gname, SERVERNAME, msg));
                    }
                    else if (checkTurn(c, ga))
                    {
                        if (ga.canEndTurn(ga.getPlayer((String) c.getData()).getPlayerNumber()))
                        {
                            endGameTurn(ga);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(gname, SERVERNAME, "You can't end your turn yet."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gname, SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * Pre-checking already done, end the current player's turn in this game.
     *<P>
     * Assumes:
     * <UL>
     * <LI> ga.canEndTurn already called, to validate player
     * <LI> ga.takeMonitor already called (not the same as {@link SOCGameList#takeMonitorForGame(String)})
     * <LI> gamelist.takeMonitorForGame is NOT called, we do NOT have that monitor
     * </UL> 
     * @param ga Game to end turn
     * @param hasListGameMon Do we have this game's monitor in SOCGameList?
     */
    private void endGameTurn(SOCGame ga)
    {
        final String gname = ga.getName();

        boolean hadBoardResetRequest = (-1 != ga.getResetVoteRequester());
        ga.endTurn();  // May set state to OVER, if new player has enough points to win
        if (hadBoardResetRequest)
        {
            // Cancel voting at end of turn
            messageToGame(gname, new SOCResetBoardReject(gname));
        }

        /**
         * send new state number; if game is now OVER,
         * also send end-of-game messages.
         */
        boolean wantsRollPrompt = sendGameState(ga, false);

        /**
         * clear any trade offers
         */
        gameList.takeMonitorForGame(gname);
        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
        {
            messageToGameWithMon(gname, new SOCClearOffer(gname, i));
        }
        gameList.releaseMonitorForGame(gname);

        /**
         * send whose turn it is
         */
        sendTurn(ga, wantsRollPrompt);
    }

    /**
     * handle "choose player" message during robbery
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleCHOOSEPLAYER(StringConnection c, SOCChoosePlayer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.canChoosePlayer(mes.getChoice()))
                        {
                            int rsrc = ga.stealFromPlayer(mes.getChoice());
                            reportRobbery(ga, ga.getPlayer((String) c.getData()), ga.getPlayer(mes.getChoice()), rsrc);
                            sendGameState(ga);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't steal from that player."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "make offer" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleMAKEOFFER(StringConnection c, SOCMakeOffer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCTradeOffer offer = mes.getOffer();

                    /**
                     * remake the offer with data that we know is accurate,
                     * namely the 'from' datum
                     */
                    SOCPlayer player = ga.getPlayer((String) c.getData());

                    if (player != null)
                    {
                        final String gaName = ga.getName();
                        SOCTradeOffer remadeOffer = new SOCTradeOffer(gaName, player.getPlayerNumber(), offer.getTo(), offer.getGiveSet(), offer.getGetSet());
                        player.setCurrentOffer(remadeOffer);
                        messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, (String) c.getData() + " made an offer to trade."));

                        SOCMakeOffer makeOfferMessage = new SOCMakeOffer(gaName, remadeOffer);
                        messageToGame(gaName, makeOfferMessage);

                        recordGameEvent(gaName, makeOfferMessage.toCmd());

                        /**
                         * clear all the trade messages because a new offer has been made
                         */
                        gameList.takeMonitorForGame(gaName);
                        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                        {
                            messageToGameWithMon(gaName, new SOCClearTradeMsg(gaName, i));
                        }
                        gameList.releaseMonitorForGame(gaName);
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "clear offer" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleCLEAROFFER(StringConnection c, SOCClearOffer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    ga.getPlayer((String) c.getData()).setCurrentOffer(null);
                    messageToGame(gaName, new SOCClearOffer(gaName, ga.getPlayer((String) c.getData()).getPlayerNumber()));
                    recordGameEvent(mes.getGame(), mes.toCmd());

                    /**
                     * clear all the trade messages
                     */
                    for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                    {
                        messageToGame(gaName, new SOCClearTradeMsg(gaName, i));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "reject offer" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleREJECTOFFER(StringConnection c, SOCRejectOffer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                SOCPlayer player = ga.getPlayer((String) c.getData());

                if (player != null)
                {
                    final String gaName = ga.getName();
                    SOCRejectOffer rejectMessage = new SOCRejectOffer(gaName, player.getPlayerNumber());
                    messageToGame(gaName, rejectMessage);

                    recordGameEvent(gaName, rejectMessage.toCmd());
                }
            }
        }
    }

    /**
     * handle "accept offer" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleACCEPTOFFER(StringConnection c, SOCAcceptOffer mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    SOCPlayer player = ga.getPlayer((String) c.getData());

                    if (player != null)
                    {
                        int acceptingNumber = player.getPlayerNumber();

                        if (ga.canMakeTrade(mes.getOfferingNumber(), acceptingNumber))
                        {
                            ga.makeTrade(mes.getOfferingNumber(), acceptingNumber);
                            reportTrade(ga, mes.getOfferingNumber(), acceptingNumber);

                            recordGameEvent(mes.getGame(), mes.toCmd());

                            /**
                             * clear all offers
                             */
                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                ga.getPlayer(i).setCurrentOffer(null);
                                messageToGame(ga.getName(), new SOCClearOffer(ga.getName(), i));
                            }

                            /**
                             * send a message to the bots that the offer was accepted
                             */
                            messageToGame(ga.getName(), mes);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't make that trade."));
                        }
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "bank trade" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleBANKTRADE(StringConnection c, SOCBankTrade mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.canMakeBankTrade(mes.getGiveSet(), mes.getGetSet()))
                        {
                            ga.makeBankTrade(mes.getGiveSet(), mes.getGetSet());
                            reportBankTrade(ga, mes.getGiveSet(), mes.getGetSet());
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "You can't make that trade."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(ga.getName(), SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "build request" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleBUILDREQUEST(StringConnection c, SOCBuildRequest mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                final String gaName = ga.getName();
                ga.takeMonitor();

                try
                {
                    if (checkTurn(c, ga))
                    {
                        if (ga.getGameState() == SOCGame.PLAY1)
                        {
                            SOCPlayer player = ga.getPlayer((String) c.getData());

                            switch (mes.getPieceType())
                            {
                            case SOCPlayingPiece.ROAD:

                                if (ga.couldBuildRoad(player.getPlayerNumber()))
                                {
                                    ga.buyRoad(player.getPlayerNumber());
                                    messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, 1));
                                    messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, 1));
                                    sendGameState(ga);
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a road."));
                                }

                                break;

                            case SOCPlayingPiece.SETTLEMENT:

                                if (ga.couldBuildSettlement(player.getPlayerNumber()))
                                {
                                    ga.buySettlement(player.getPlayerNumber());
                                    gameList.takeMonitorForGame(gaName);
                                    messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, 1));
                                    messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, 1));
                                    messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, 1));
                                    messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, 1));
                                    gameList.releaseMonitorForGame(gaName);
                                    sendGameState(ga);
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a settlement."));
                                }

                                break;

                            case SOCPlayingPiece.CITY:

                                if (ga.couldBuildCity(player.getPlayerNumber()))
                                {
                                    ga.buyCity(player.getPlayerNumber());
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, 3));
                                    messageToGame(ga.getName(), new SOCPlayerElement(ga.getName(), player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, 2));
                                    sendGameState(ga);
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build a city."));
                                }

                                break;
                            }
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't build now."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "cancel build request" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleCANCELBUILDREQUEST(StringConnection c, SOCCancelBuildRequest mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        SOCPlayer player = ga.getPlayer((String) c.getData());

                        switch (mes.getPieceType())
                        {
                        case SOCPlayingPiece.ROAD:

                            if (ga.getGameState() == SOCGame.PLACING_ROAD)
                            {
                                ga.cancelBuildRoad(player.getPlayerNumber());
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, 1));
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, 1));
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You didn't buy a road."));
                            }

                            break;

                        case SOCPlayingPiece.SETTLEMENT:

                            if (ga.getGameState() == SOCGame.PLACING_SETTLEMENT)
                            {
                                ga.cancelBuildSettlement(player.getPlayerNumber());
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, 1));
                                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, 1));
                                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, 1));
                                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, 1));
                                gameList.releaseMonitorForGame(gaName);
                                sendGameState(ga);
                            }
                            else if ((ga.getGameState() == SOCGame.START1B) || (ga.getGameState() == SOCGame.START2B))
                            {
                                SOCSettlement pp = new SOCSettlement(player, player.getLastSettlementCoord());
                                ga.undoPutInitSettlement(pp);
                                messageToGame(gaName, mes);  // Re-send to all clients to announce it
                                    // (Safe since we've validated all message parameters)
                                messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, player.getName() + " cancelled their settlement placement."));
                                sendGameState(ga);  // This send is redundant, if client reaction changes game state
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You didn't buy a settlement."));
                            }

                            break;

                        case SOCPlayingPiece.CITY:

                            if (ga.getGameState() == SOCGame.PLACING_CITY)
                            {
                                ga.cancelBuildCity(player.getPlayerNumber());
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.ORE, 3));
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, 2));
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You didn't buy a city."));
                            }

                            break;
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "buy card request" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleBUYCARDREQUEST(StringConnection c, SOCBuyCardRequest mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        SOCPlayer player = ga.getPlayer((String) c.getData());

                        if ((ga.getGameState() == SOCGame.PLAY1) && (ga.couldBuyDevCard(player.getPlayerNumber())))
                        {
                            int card = ga.buyDevCard();
                            gameList.takeMonitorForGame(gaName);
                            messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, 1));
                            messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, 1));
                            messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, 1));
                            messageToGameWithMon(gaName, new SOCDevCardCount(gaName, ga.getNumDevCards()));
                            gameList.releaseMonitorForGame(gaName);
                            messageToPlayer(c, new SOCDevCard(gaName, player.getPlayerNumber(), SOCDevCard.DRAW, card));

                            messageToGameExcept(gaName, c, new SOCDevCard(gaName, player.getPlayerNumber(), SOCDevCard.DRAW, SOCDevCardConstants.UNKNOWN));
                            messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, (String) c.getData() + " bought a development card."));

                            if (ga.getNumDevCards() > 1)
                            {
                                messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, "There are " + ga.getNumDevCards() + " cards left."));
                            }
                            else if (ga.getNumDevCards() == 1)
                            {
                                messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, "There is 1 card left."));
                            }
                            else
                            {
                                messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, "There are no more Development cards."));
                            }

                            sendGameState(ga);
                        }
                        else
                        {
                            if (ga.getNumDevCards() == 0)
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "There are no more Development cards."));
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't buy a development card now."));
                            }
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "play development card request" message
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handlePLAYDEVCARDREQUEST(StringConnection c, SOCPlayDevCardRequest mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        SOCPlayer player = ga.getPlayer((String) c.getData());

                        switch (mes.getDevCard())
                        {
                        case SOCDevCardConstants.KNIGHT:

                            if (ga.canPlayKnight(player.getPlayerNumber()))
                            {
                                ga.playKnight();
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, player.getName() + " played a Soldier card."));
                                messageToGameWithMon(gaName, new SOCDevCard(gaName, player.getPlayerNumber(), SOCDevCard.PLAY, SOCDevCardConstants.KNIGHT));
                                messageToGameWithMon(gaName, new SOCSetPlayedDevCard(gaName, player.getPlayerNumber(), true));
                                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.NUMKNIGHTS, 1));
                                gameList.releaseMonitorForGame(gaName);
                                broadcastGameStats(ga);
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't play a Soldier card now."));
                            }

                            break;

                        case SOCDevCardConstants.ROADS:

                            if (ga.canPlayRoadBuilding(player.getPlayerNumber()))
                            {
                                ga.playRoadBuilding();
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new SOCDevCard(gaName, player.getPlayerNumber(), SOCDevCard.PLAY, SOCDevCardConstants.ROADS));
                                messageToGameWithMon(gaName, new SOCSetPlayedDevCard(gaName, player.getPlayerNumber(), true));
                                messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, player.getName() + " played a Road Building card."));
                                gameList.releaseMonitorForGame(gaName);
                                sendGameState(ga);
                                if (ga.getGameState() == SOCGame.PLACING_FREE_ROAD1)
                                {
                                    c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You may place 2 roads."));
                                }
                                else
                                {
                                    c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You may place your 1 remaining road."));
                                }
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't play a Road Building card now."));
                            }

                            break;

                        case SOCDevCardConstants.DISC:

                            if (ga.canPlayDiscovery(player.getPlayerNumber()))
                            {
                                ga.playDiscovery();
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new SOCDevCard(gaName, player.getPlayerNumber(), SOCDevCard.PLAY, SOCDevCardConstants.DISC));
                                messageToGameWithMon(gaName, new SOCSetPlayedDevCard(gaName, player.getPlayerNumber(), true));
                                messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, player.getName() + " played a Year of Plenty card."));
                                gameList.releaseMonitorForGame(gaName);
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't play a Year of Plenty card now."));
                            }

                            break;

                        case SOCDevCardConstants.MONO:

                            if (ga.canPlayMonopoly(player.getPlayerNumber()))
                            {
                                ga.playMonopoly();
                                gameList.takeMonitorForGame(gaName);
                                messageToGameWithMon(gaName, new SOCDevCard(gaName, player.getPlayerNumber(), SOCDevCard.PLAY, SOCDevCardConstants.MONO));
                                messageToGameWithMon(gaName, new SOCSetPlayedDevCard(gaName, player.getPlayerNumber(), true));
                                messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, player.getName() + " played a Monopoly card."));
                                gameList.releaseMonitorForGame(gaName);
                                sendGameState(ga);
                            }
                            else
                            {
                                c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't play a Monopoly card now."));
                            }

                            break;

                        // VP cards are secretly played when bought.
                        // (case SOCDevCardConstants.CAP, LIB, UNIV, TEMP, TOW):
                        // If player clicks "Play Card" the message is handled at the
                        // client, in SOCHandPanel.actionPerformed case CARD.
                        //  "You secretly played this VP card when you bought it."
                        //  break;

                        default:
                            D.ebugPrintln("* SOCServer.handlePLAYDEVCARDREQUEST: asked to play unhandled type " + mes.getDevCard());

                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "discovery pick" message (while playing Discovery card)
     *
     * @param c  the connection that sent the message
     * @param mes  the messsage
     */
    private void handleDISCOVERYPICK(StringConnection c, SOCDiscoveryPick mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        SOCPlayer player = ga.getPlayer((String) c.getData());

                        if (ga.canDoDiscoveryAction(mes.getResources()))
                        {
                            ga.doDiscoveryAction(mes.getResources());

                            String message = (String) c.getData() + " received ";
                            int cl, or, sh, wh, wo;
                            cl = mes.getResources().getAmount(SOCResourceConstants.CLAY);
                            or = mes.getResources().getAmount(SOCResourceConstants.ORE);
                            sh = mes.getResources().getAmount(SOCResourceConstants.SHEEP);
                            wh = mes.getResources().getAmount(SOCResourceConstants.WHEAT);
                            wo = mes.getResources().getAmount(SOCResourceConstants.WOOD);

                            if (cl > 0)
                            {
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, cl));
                                message += (cl + " clay");

                                if ((or + sh + wh + wo) > 0)
                                {
                                    message += " and ";
                                }
                            }

                            if (or > 0)
                            {
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.ORE, or));
                                message += (or + " ore");

                                if ((sh + wh + wo) > 0)
                                {
                                    message += " and ";
                                }
                            }

                            if (sh > 0)
                            {
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, sh));
                                message += (sh + " sheep");

                                if ((wh + wo) > 0)
                                {
                                    message += " and ";
                                }
                            }

                            if (wh > 0)
                            {
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, wh));
                                message += (wh + " wheat");

                                if (wo > 0)
                                {
                                    message += " and ";
                                }
                            }

                            if (wo > 0)
                            {
                                messageToGame(gaName, new SOCPlayerElement(gaName, player.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, wo));
                                message += (wo + " wood");
                            }

                            message += " from the bank.";
                            messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, message));
                            sendGameState(ga);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "That is not a legal Year of Plenty pick."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "monopoly pick" message
     *
     * @param c     the connection that sent the message
     * @param mes   the messsage
     */
    private void handleMONOPOLYPICK(StringConnection c, SOCMonopolyPick mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                ga.takeMonitor();

                try
                {
                    final String gaName = ga.getName();
                    if (checkTurn(c, ga))
                    {
                        if (ga.canDoMonopolyAction())
                        {
                            ga.doMonopolyAction(mes.getResource());

                            String message = (String) c.getData() + " monopolized ";

                            switch (mes.getResource())
                            {
                            case SOCResourceConstants.CLAY:
                                message += "clay.";

                                break;

                            case SOCResourceConstants.ORE:
                                message += "ore.";

                                break;

                            case SOCResourceConstants.SHEEP:
                                message += "sheep.";

                                break;

                            case SOCResourceConstants.WHEAT:
                                message += "wheat.";

                                break;

                            case SOCResourceConstants.WOOD:
                                message += "wood.";

                                break;
                            }

                            gameList.takeMonitorForGame(gaName);
                            messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, message));

                            /**
                             * just send all the player's resource counts for the
                             * monopolized resource
                             */
                            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                            {
                                /**
                                 * Note: This only works if SOCPlayerElement.CLAY == SOCResourceConstants.CLAY
                                 */
                                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, i, SOCPlayerElement.SET, mes.getResource(), ga.getPlayer(i).getResources().getAmount(mes.getResource())));
                            }
                            gameList.releaseMonitorForGame(gaName);

                            sendGameState(ga);
                        }
                        else
                        {
                            c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "You can't do a Monopoly pick now."));
                        }
                    }
                    else
                    {
                        c.put(SOCGameTextMsg.toCmd(gaName, SERVERNAME, "It's not your turn."));
                    }
                }
                catch (Exception e)
                {
                    D.ebugPrintln("Exception caught - " + e);
                    e.printStackTrace();
                }

                ga.releaseMonitor();
            }
        }
    }

    /**
     * handle "change face" message
     *
     * @param c  the connection
     * @param mes  the message
     */
    private void handleCHANGEFACE(StringConnection c, SOCChangeFace mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                SOCPlayer player = ga.getPlayer((String) c.getData());

                if (player != null)
                {
                    final String gaName = mes.getGame();
                    player.setFaceId(mes.getFaceId());
                    messageToGame(gaName, new SOCChangeFace(gaName, player.getPlayerNumber(), mes.getFaceId()));
                }
            }
        }
    }

    /**
     * handle "set seat lock" message
     *
     * @param c  the connection
     * @param mes  the message
     */
    private void handleSETSEATLOCK(StringConnection c, SOCSetSeatLock mes)
    {
        if (c != null)
        {
            SOCGame ga = gameList.getGameData(mes.getGame());

            if (ga != null)
            {
                SOCPlayer player = ga.getPlayer((String) c.getData());

                if (player != null)
                {
                    if (mes.getLockState() == true)
                    {
                        ga.lockSeat(mes.getPlayerNumber());
                    }
                    else
                    {
                        ga.unlockSeat(mes.getPlayerNumber());
                    }

                    messageToGame(mes.getGame(), mes);
                }
            }
        }
    }

    /**
     * handle "reset-board request" message.
     * If multiple human players, start a vote.
     * Otherwise, reset the game to a copy with
     * same name and (copy of) same players, new layout.
     *<P>
     * The requesting player doesn't vote, but server still
     * sends the vote-request-message, to tell that client their
     * request was accepted and voting has begun.
     *<P>
     * If only one player remains (all other humans have left at end),
     * ask them to start a new game instead. This is a rare occurrence
     * and we shouldn't bring in new robots and all,
     * since we already have an interface to set up a game.
     *
     * @see #resetBoardAndNotify(String, String)
     *
     * @param c  the connection
     * @param mes  the message
     */
    private void handleRESETBOARDREQUEST(StringConnection c, SOCResetBoardRequest mes)
    {
        if (c == null)
            return;
        String gaName = mes.getGame();
        SOCGame ga = gameList.getGameData(gaName);
        if (ga == null)
            return;        
        SOCPlayer reqPlayer = ga.getPlayer((String) c.getData());
        if (reqPlayer == null)
        {
            return;  // Not playing in that game (Security)
        }
        
        /**
         * Is voting already active from another player?
         * Or, has this player already asked for voting this turn?
         */
        if (ga.getResetVoteActive() || reqPlayer.hasAskedBoardReset())
        {
            // Ignore this second request. Can't send REJECT because
            // that would end the already-active round of voting.
            return;
        }
        
        /**
         * Is there more than one human player?
         * Grab connection information for humans and robots.
         */
        StringConnection[] humanConns = new StringConnection[SOCGame.MAXPLAYERS];
        StringConnection[] robotConns = new StringConnection[SOCGame.MAXPLAYERS];
        int numHuman = SOCGameBoardReset.sortPlayerConnections(ga, null, gameList.getMembers(gaName), humanConns, robotConns);

        int reqPN = reqPlayer.getPlayerNumber();
        if (numHuman < 2)
        {
            // Are there robots? Go ahead and reset if so.
            int numRobot = 0;
            for (int i = robotConns.length-1; i>=0; --i)
                if (robotConns[i] != null)
                    ++numRobot;
            if (numRobot > 0)
            {
                resetBoardAndNotify(gaName, reqPN);
            } else {
                messageToGameUrgent(gaName, "Everyone has left this game. Please start a new game with players or bots.");
            }
        }
        else
        {
            // Put it to a vote
            gameList.takeMonitorForGame(gaName);
            messageToGameWithMon(gaName, new SOCGameTextMsg
                (gaName, SERVERNAME, (String) c.getData() + " requests a board reset - other players please vote."));
            String vrCmd = SOCResetBoardVoteRequest.toCmd(gaName, reqPN);
            ga.resetVoteBegin(reqPN);
            gameList.releaseMonitorForGame(gaName);
            for (int i = 0; i < SOCGame.MAXPLAYERS; ++i)
                if (humanConns[i] != null)
                    humanConns[i].put(vrCmd);
        }
    }

    /**
     * handle message of player's vote for a "reset-board" request.
     * Register the player's vote.
     * If all votes have now arrived, and the vote is unanimous,
     * reset the game to a copy with same name and players, new layout.
     *
     * @see #resetBoardAndNotify(String, String)
     *
     * @param c  the connection
     * @param mes  the message
     */
    private void handleRESETBOARDVOTE(StringConnection c, SOCResetBoardVote mes)
    {
        if (c == null)
            return;
        String gaName = mes.getGame();
        SOCGame ga = gameList.getGameData(gaName);
        if (ga == null)
            return;        
        SOCPlayer reqPlayer = ga.getPlayer((String) c.getData());
        if (reqPlayer == null)
        {
            return;  // Not playing in that game (Security)
        }
        
        boolean votingComplete = false;
        try
        {
            int pn = reqPlayer.getPlayerNumber();
            boolean vyes = mes.getPlayerVote();

            // Register in game
            votingComplete = ga.resetVoteRegister(pn, vyes);
            // Tell other players
            messageToGame (gaName, new SOCResetBoardVote(gaName, pn, vyes));
        }
        catch (IllegalArgumentException e)
        {
            D.ebugPrintln("*Error in player voting: game " + ga.getName() + ": " + e);
            return;
        }
        catch (IllegalStateException e)
        {
            D.ebugPrintln("*Voting not active: game " + ga.getName());
            return;
        }

        if (! votingComplete)
        {
            return;
        }
        
        if (ga.getResetVoteResult())
        {
            // Vote succeeded - Go ahead and reset.
            resetBoardAndNotify(gaName, ga.getResetVoteRequester());
        }
        else
        {
            // Vote rejected - Let everyone know.
            messageToGame(gaName, new SOCResetBoardReject(gaName));
        }                
    }

    /**
     * handle "create account" message
     *
     * @param c  the connection
     * @param mes  the message
     */
    private void handleCREATEACCOUNT(StringConnection c, SOCCreateAccount mes)
    {
        //
        // check to see if there is an account with
        // the requested nickname
        //
        String userPassword = null;

        try
        {
            userPassword = SOCDBHelper.getUserPassword(mes.getNickname());
        }
        catch (SQLException sqle)
        {
            // Indicates a db problem: don't continue
            c.put(SOCStatusMessage.toCmd("Problem connecting to database, please try again later."));
            return;
        }

        if (userPassword != null)
        {
            c.put(SOCStatusMessage.toCmd("The nickname '" + mes.getNickname() + "' is already in use."));

            return;
        }

        //
        // create the account
        //
        Date currentTime = new Date();

        boolean success = false;

        try
        {
            success = SOCDBHelper.createAccount(mes.getNickname(), c.host(), mes.getPassword(), mes.getEmail(), currentTime.getTime());
        }
        catch (SQLException sqle)
        {
            System.err.println("Error creating account in db.");
        }

        if (success)
        {
            c.put(SOCStatusMessage.toCmd("Account created for '" + mes.getNickname() + "'."));
        }
        else
        {
            c.put(SOCStatusMessage.toCmd("Account not created due to error."));
        }
    }

    /**
     * Client has been approved to join game; send the entire state of the game to client,
     * send client join event to other players.
     * First message sent to connecting client is JOINGAMEAUTH, unless isReset.
     *
     * @param gameData Game to join
     * @param c        The connection of joining client
     * @param isReset  Game is a board-reset of an existing game
     */
    private void joinGame(SOCGame gameData, StringConnection c, boolean isReset)
    {
        String gameName = gameData.getName();
        if (! isReset)
        {
            c.put(SOCJoinGameAuth.toCmd(gameName));
            c.put(SOCStatusMessage.toCmd("Welcome to Java Settlers of Catan!"));
        }

        //c.put(SOCGameState.toCmd(gameName, gameData.getGameState()));
        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
        {
            /**
             * send the already-seated player information;
             * if isReset, don't send, because sitDown will
             * be sent from resetBoardAndNotify.
             */
            if (! isReset)
            {
                SOCPlayer pl = gameData.getPlayer(i);
                String plName = pl.getName(); 
                if ((plName != null) && (!gameData.isSeatVacant(i)))
                {
                    c.put(SOCSitDown.toCmd(gameName, plName, i, pl.isRobot()));
                }
            }

            /**
             * send the seat lock information
             */
            messageToPlayer(c, new SOCSetSeatLock(gameName, i, gameData.isSeatLocked(i)));
        }

        SOCBoardLayout bl = getBoardLayoutMessage(gameData);
        c.put(bl.toCmd());

        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
        {
            SOCPlayer pl = gameData.getPlayer(i);

            // Send piece info even if player has left the game (pl.getName() == null).
            // This lets them see "their" pieces before sitDown(), if they rejoin at same position.

            Enumeration piecesEnum = pl.getPieces().elements();

            while (piecesEnum.hasMoreElements())
            {
                SOCPlayingPiece piece = (SOCPlayingPiece) piecesEnum.nextElement();

                if (piece.getType() == SOCPlayingPiece.CITY)
                {
                    c.put(SOCPutPiece.toCmd(gameName, i, SOCPlayingPiece.SETTLEMENT, piece.getCoordinates()));
                }

                c.put(SOCPutPiece.toCmd(gameName, i, piece.getType(), piece.getCoordinates()));
            }

            /**
             * send potential settlement list
             */
            Vector psList = new Vector();

            for (int j = 0x23; j <= 0xDC; j++)
            {
                if (pl.isPotentialSettlement(j))
                {
                    psList.addElement(new Integer(j));
                }
            }

            c.put(SOCPotentialSettlements.toCmd(gameName, i, psList));

            /**
             * send coords of the last settlement
             */
            c.put(SOCLastSettlement.toCmd(gameName, i, pl.getLastSettlementCoord()));

            /**
             * send number of playing pieces in hand
             */
            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.ROADS, pl.getNumPieces(SOCPlayingPiece.ROAD)));
            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.SETTLEMENTS, pl.getNumPieces(SOCPlayingPiece.SETTLEMENT)));
            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.CITIES, pl.getNumPieces(SOCPlayingPiece.CITY)));

            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.UNKNOWN, pl.getResources().getTotal()));

            c.put(SOCPlayerElement.toCmd(gameName, i, SOCPlayerElement.SET, SOCPlayerElement.NUMKNIGHTS, pl.getNumKnights()));

            int numDevCards = pl.getDevCards().getTotal();

            for (int j = 0; j < numDevCards; j++)
            {
                c.put(SOCDevCard.toCmd(gameName, i, SOCDevCard.ADDOLD, SOCDevCardConstants.UNKNOWN));
            }

            c.put(SOCFirstPlayer.toCmd(gameName, gameData.getFirstPlayer()));

            c.put(SOCDevCardCount.toCmd(gameName, gameData.getNumDevCards()));

            c.put(SOCChangeFace.toCmd(gameName, i, pl.getFaceId()));

            c.put(SOCDiceResult.toCmd(gameName, gameData.getCurrentDice()));
        }

        /// 
        /// send who has longest road
        ///
        SOCPlayer lrPlayer = gameData.getPlayerWithLongestRoad();
        int lrPlayerNum = -1;

        if (lrPlayer != null)
        {
            lrPlayerNum = lrPlayer.getPlayerNumber();
        }

        c.put(SOCLongestRoad.toCmd(gameName, lrPlayerNum));

        ///
        /// send who has largest army
        ///
        final SOCPlayer laPlayer = gameData.getPlayerWithLargestArmy();
        final int laPlayerNum;
        if (laPlayer != null)
        {
            laPlayerNum = laPlayer.getPlayerNumber();
        }
        else
        {
            laPlayerNum = -1;
        }

        c.put(SOCLargestArmy.toCmd(gameName, laPlayerNum));

        String membersCommand = null;
        gameList.takeMonitorForGame(gameName);

        try
        {
            Vector gameMembers = gameList.getMembers(gameName);
            membersCommand = SOCGameMembers.toCmd(gameName, gameMembers);
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in handleJOINGAME (gameMembers) - " + e);
        }

        gameList.releaseMonitorForGame(gameName);
        c.put(membersCommand);
        c.put(SOCSetTurn.toCmd(gameName, gameData.getCurrentPlayerNumber()));
        c.put(SOCGameState.toCmd(gameName, gameData.getGameState()));
        D.ebugPrintln("*** " + c.getData() + " joined the game " + gameName);

        //messageToGame(gameName, new SOCGameTextMsg(gameName, SERVERNAME, n+" joined the game"));
        /**
         * Let everyone else know about the change
         */
        messageToGame(gameName, new SOCJoinGame
            ((String)c.getData(), "", "dummyhost", gameName));
    }

    /**
     * This player is sitting down at the game
     *
     * @param ga     the game
     * @param c      the connection for the player
     * @param pn     which seat the player is taking
     * @param robot  true if this player is a robot
     * @param isReset Game is a board-reset of an existing game
     */
    private void sitDown(SOCGame ga, StringConnection c, int pn, boolean robot, boolean isReset)
    {
        if ((c != null) && (ga != null))
        {
            ga.takeMonitor();

            try
            {
                if (! isReset)
                {
                    ga.addPlayer((String) c.getData(), pn);
                    ga.getPlayer(pn).setRobotFlag(robot);
                    // If reset, player is already added and knows if robot.
                }

                /**
                 * if the player can sit, then tell the other clients in the game
                 */
                SOCSitDown sitMessage = new SOCSitDown(ga.getName(), (String) c.getData(), pn, robot);
                messageToGame(ga.getName(), sitMessage);

                D.ebugPrintln("*** sent SOCSitDown message to game ***");

                recordGameEvent(ga.getName(), sitMessage.toCmd());

                Vector requests;
                if (! isReset)
                {
                    requests = (Vector) robotJoinRequests.get(ga.getName());
                }
                else
                {
                    requests = null;  // Game already has all players from old game
                }

                if (requests != null)
                {
                    /**
                     * if the request list is empty and the game hasn't started yet,
                     * then start the game
                     */
                    if (requests.isEmpty() && (ga.getGameState() < SOCGame.START1A))
                    {
                        startGame(ga);
                    }

                    /**
                     * if the request list is empty, remove the empty list
                     */
                    if (requests.isEmpty())
                    {
                        robotJoinRequests.remove(ga.getName());
                    }
                }

                broadcastGameStats(ga);

                /**
                 * send all the private information
                 */
                SOCResourceSet resources = ga.getPlayer(pn).getResources();
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.CLAY, resources.getAmount(SOCPlayerElement.CLAY)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.ORE, resources.getAmount(SOCPlayerElement.ORE)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.SHEEP, resources.getAmount(SOCPlayerElement.SHEEP)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.WHEAT, resources.getAmount(SOCPlayerElement.WHEAT)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.WOOD, resources.getAmount(SOCPlayerElement.WOOD)));
                messageToPlayer(c, new SOCPlayerElement(ga.getName(), pn, SOCPlayerElement.SET, SOCPlayerElement.UNKNOWN, resources.getAmount(SOCPlayerElement.UNKNOWN)));

                SOCDevCardSet devCards = ga.getPlayer(pn).getDevCards();

                /**
                 * remove the unknown cards
                 */
                int i;

                for (i = 0; i < devCards.getTotal(); i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.PLAY, SOCDevCardConstants.UNKNOWN));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.KNIGHT);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.KNIGHT));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.ROADS);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.ROADS));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.DISC);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.DISC));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.MONO);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.MONO));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.CAP);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.CAP));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.LIB);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.LIB));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.UNIV);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.UNIV));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.TEMP);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.TEMP));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.NEW, SOCDevCardConstants.TOW);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDNEW, SOCDevCardConstants.TOW));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.KNIGHT);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.KNIGHT));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.ROADS);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.ROADS));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.DISC);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.DISC));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.MONO);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.MONO));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.CAP);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.CAP));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.LIB);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.LIB));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.UNIV);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.UNIV));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.TEMP);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.TEMP));
                }

                for (i = 0;
                        i < devCards.getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.TOW);
                        i++)
                {
                    messageToPlayer(c, new SOCDevCard(ga.getName(), pn, SOCDevCard.ADDOLD, SOCDevCardConstants.TOW));
                }

                /**
                 * send game state info such as requests for discards
                 */
                sendGameState(ga);

                if ((ga.getCurrentDice() == 7) && (ga.getPlayer(pn).getResources().getTotal() > 7))
                {
                    messageToPlayer(c, new SOCDiscardRequest(ga.getName(), ga.getPlayer(pn).getResources().getTotal() / 2));
                }

                /**
                 * send what face this player is using
                 */
                messageToGame(ga.getName(), new SOCChangeFace(ga.getName(), pn, ga.getPlayer(pn).getFaceId()));
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception caught - " + e);
                e.printStackTrace();
            }

            ga.releaseMonitor();
        }
    }

    /**
     * The current player is stealing from another player.
     * Send messages saying what was stolen.
     *
     * @param ga  the game
     * @param pe  the perpetrator
     * @param vi  the the victim
     * @param rsrc  type of resource stolen, as in SOCResourceConstants
     */
    protected void reportRobbery(SOCGame ga, SOCPlayer pe, SOCPlayer vi, int rsrc)
    {
        if (ga != null)
        {
            String mes1 = "You stole ";
            String mes2 = pe.getName() + " stole ";
            SOCPlayerElement gainRsrc = null;
            SOCPlayerElement loseRsrc = null;
            SOCPlayerElement gainUnknown;
            SOCPlayerElement loseUnknown;

            switch (rsrc)
            {
            case SOCResourceConstants.CLAY:
                mes1 += "a clay ";
                mes2 += "a clay ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.CLAY, 1);

                break;

            case SOCResourceConstants.ORE:
                mes1 += "an ore ";
                mes2 += "an ore ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.ORE, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.ORE, 1);

                break;

            case SOCResourceConstants.SHEEP:
                mes1 += "a sheep ";
                mes2 += "a sheep ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.SHEEP, 1);

                break;

            case SOCResourceConstants.WHEAT:
                mes1 += "a wheat ";
                mes2 += "a wheat ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WHEAT, 1);

                break;

            case SOCResourceConstants.WOOD:
                mes1 += "a wood ";
                mes2 += "a wood ";
                gainRsrc = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, 1);
                loseRsrc = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.WOOD, 1);

                break;
            }

            mes1 += ("resource from " + vi.getName() + ".");
            mes2 += "resource from you.";

            StringConnection peCon = null;
            StringConnection viCon = null;
            Enumeration conEnum = conns.elements();

            while (conEnum.hasMoreElements())
            {
                StringConnection con = (StringConnection) conEnum.nextElement();

                if (pe.getName().equals((String) con.getData()))
                {
                    peCon = con;
                }
                else if (vi.getName().equals((String) con.getData()))
                {
                    viCon = con;
                }
            }

            Vector exceptions = new Vector(2);
            exceptions.addElement(peCon);
            exceptions.addElement(viCon);

            /**
             * send the game messages
             */
            messageToPlayer(peCon, gainRsrc);
            messageToPlayer(peCon, loseRsrc);
            messageToPlayer(viCon, gainRsrc);
            messageToPlayer(viCon, loseRsrc);
            gainUnknown = new SOCPlayerElement(ga.getName(), pe.getPlayerNumber(), SOCPlayerElement.GAIN, SOCPlayerElement.UNKNOWN, 1);
            loseUnknown = new SOCPlayerElement(ga.getName(), vi.getPlayerNumber(), SOCPlayerElement.LOSE, SOCPlayerElement.UNKNOWN, 1);
            messageToGameExcept(ga.getName(), exceptions, gainUnknown);
            messageToGameExcept(ga.getName(), exceptions, loseUnknown);

            /**
             * send the text messages
             */
            messageToPlayer(peCon, new SOCGameTextMsg(ga.getName(), SERVERNAME, mes1));
            messageToPlayer(viCon, new SOCGameTextMsg(ga.getName(), SERVERNAME, mes2));
            messageToGameExcept(ga.getName(), exceptions, new SOCGameTextMsg(ga.getName(), SERVERNAME, pe.getName() + " stole a resource from " + vi.getName()));
        }
    }

    /**
     * send the current state of the game with a message.
     * Assumes current player does not change during this state.
     * If we send a text message to prompt the new player to roll,
     * also sends a RollDicePrompt data message.
     *
     * @param ga  the game
     * 
     * @see #sendGameState(SOCGame, boolean)
     */
    protected void sendGameState(SOCGame ga)
    {
        sendGameState(ga, true);
    }
    
    /**
     * send the current state of the game with a message.
     * Note that the current (or new) player number is not sent here.
     * If game is now OVER, send appropriate messages.
     * 
     * @see #sendTurn(SOCGame, boolean)
     * @see #sendGameState(SOCGame)
     * @see #sendGameStateOVER(SOCGame)
     *
     * @param ga  the game
     * @param sendRollPrompt  If true, and if we send a text message to prompt
     *    the player to roll, send a RollDicePrompt data message.
     *    
     * @return    did we send a text message to prompt the player to roll?
     *    If so, sendTurn can also send a RollDicePrompt data message.  
     */
    protected boolean sendGameState(SOCGame ga, boolean sendRollPrompt)
    {
        if (ga == null)
            return false;

        final String gname = ga.getName();
        boolean promptedRoll = false;
        if (ga.getGameState() == SOCGame.OVER)
        {
            /**
             * Before sending state "OVER", enforce current player number.
             * This helps the client's copy of game recognize winning condition.
             */
            messageToGame(gname, new SOCSetTurn(gname, ga.getCurrentPlayerNumber()));
        }
        messageToGame(gname, new SOCGameState(gname, ga.getGameState()));

        SOCPlayer player = null;

        if (ga.getCurrentPlayerNumber() != -1)
        {
            player = ga.getPlayer(ga.getCurrentPlayerNumber());
        }

        switch (ga.getGameState())
        {
        case SOCGame.START1A:
        case SOCGame.START2A:
            messageToGame(gname, new SOCGameTextMsg(gname, SERVERNAME, "It's " + player.getName() + "'s turn to build a settlement."));

            break;

        case SOCGame.START1B:
        case SOCGame.START2B:
            messageToGame(gname, new SOCGameTextMsg(gname, SERVERNAME, "It's " + player.getName() + "'s turn to build a road."));

            break;

        case SOCGame.PLAY:
            messageToGame(gname, new SOCGameTextMsg(gname, SERVERNAME, "It's " + player.getName() + "'s turn to roll the dice."));
            promptedRoll = true;
            if (sendRollPrompt)
                messageToGame(gname, new SOCRollDicePrompt (gname, player.getPlayerNumber()));
                
            break;

        case SOCGame.WAITING_FOR_DISCARDS:

            int count = 0;
            String message = "error at sendGameState()";
            String[] names = new String[SOCGame.MAXPLAYERS];

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                if (ga.getPlayer(i).getNeedToDiscard())
                {
                    names[count] = ga.getPlayer(i).getName();
                    count++;
                }
            }

            if (count == 1)
            {
                message = names[0] + " needs to discard.";
            }
            else if (count == 2)
            {
                message = names[0] + " and " + names[1] + " need to discard.";
            }
            else if (count > 2)
            {
                message = names[0];

                for (int i = 1; i < (count - 1); i++)
                {
                    message += (", " + names[i]);
                }

                message += (" and " + names[count - 1] + " need to discard.");
            }

            messageToGame(gname, new SOCGameTextMsg(gname, SERVERNAME, message));

            break;

        case SOCGame.PLACING_ROBBER:
            messageToGame(gname, new SOCGameTextMsg(gname, SERVERNAME, player.getName() + " will move the robber."));

            break;

        case SOCGame.WAITING_FOR_CHOICE:

            /**
             * get the choices from the game
             */
            boolean[] choices = new boolean[SOCGame.MAXPLAYERS];

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                choices[i] = false;
            }

            Enumeration plEnum = ga.getPossibleVictims().elements();

            while (plEnum.hasMoreElements())
            {
                SOCPlayer pl = (SOCPlayer) plEnum.nextElement();
                choices[pl.getPlayerNumber()] = true;
            }

            /**
             * ask the current player to choose a player to steal from
             */
            String n = ga.getPlayer(ga.getCurrentPlayerNumber()).getName();
            Enumeration connsEnum = getConnections();

            while (connsEnum.hasMoreElements())
            {
                StringConnection con = (StringConnection) connsEnum.nextElement();

                if (n.equals((String) con.getData()))
                {
                    con.put(SOCChoosePlayerRequest.toCmd(gname, choices));

                    break;
                }
            }

            break;

        case SOCGame.OVER:

            sendGameStateOVER(ga);
            
            break;
            
        }  // switch ga.getGameState
        
        return promptedRoll; 
    }
    
    /**
     *  If game is OVER, send messages reporting winner, final score,
     *  and each player's victory-point cards.
     *  Also give stats on game length, and on each player's connect time.
     *
     * @param ga This game is over; state should be OVER
     */
    protected void sendGameStateOVER(SOCGame ga)
    {
        final String gname = ga.getName();

        // Find and announce the winner
        // (the real "found winner" code is in SOCGame.checkForWinner)
        {
            SOCPlayer pl = ga.getPlayer(ga.getCurrentPlayerNumber());

            if (pl.getTotalVP() < SOCGame.VP_WINNER)
            {
                // Should not happen: By rules FAQ, only current player can be winner.
                for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
                {
                    pl = ga.getPlayer(i);        
                    if (pl.getTotalVP() >= SOCGame.VP_WINNER)
                    {
                        break;
                    }
                }
            }
            String msg = ">>> " + pl.getName() + " has won the game with " + pl.getTotalVP() + " points.";
            messageToGameUrgent(gname, msg);
        }
        
        /// send a message with the revealed final scores
        {
            int[] scores = new int[SOCGame.MAXPLAYERS];
            boolean[] isRobot = new boolean[SOCGame.MAXPLAYERS];
            for (int i = 0; i < SOCGame.MAXPLAYERS; ++i)
            {
                scores[i] = ga.getPlayer(i).getTotalVP();
                isRobot[i] = ga.getPlayer(i).isRobot();
            }
            messageToGame(gname, new SOCGameStats(gname, scores, isRobot));
        }
        
        ///
        /// send a message saying what VP cards each player has
        ///
        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
        {
            SOCPlayer pl = ga.getPlayer(i);
            SOCDevCardSet devCards = pl.getDevCards();

            if (devCards.getNumVPCards() > 0)
            {
                String msg = pl.getName() + " has";
                int vpCardCount = 0;

                for (int devCardType = SOCDevCardConstants.CAP;
                        devCardType < SOCDevCardConstants.UNKNOWN;
                        devCardType++)
                {
                    if ((devCards.getAmount(SOCDevCardSet.OLD, devCardType) > 0) || (devCards.getAmount(SOCDevCardSet.NEW, devCardType) > 0))
                    {
                        if (vpCardCount > 0)
                        {
                            if ((devCards.getNumVPCards() - vpCardCount) == 1)
                            {
                                msg += " and";
                            }
                            else if ((devCards.getNumVPCards() - vpCardCount) > 0)
                            {
                                msg += ",";
                            }
                        }

                        vpCardCount++;

                        switch (devCardType)
                        {
                        case SOCDevCardConstants.CAP:
                            msg += " a Gov.House (+1VP)";

                            break;

                        case SOCDevCardConstants.LIB:
                            msg += " a Market (+1VP)";

                            break;

                        case SOCDevCardConstants.UNIV:
                            msg += " a University (+1VP)";

                            break;

                        case SOCDevCardConstants.TEMP:
                            msg += " a Temple (+1VP)";

                            break;

                        case SOCDevCardConstants.TOW:
                            msg += " a Chapel (+1VP)";

                            break;
                        }
                    }
                }  // for each devcard type

                messageToGame(gname, new SOCGameTextMsg(gname, SERVERNAME, msg));

            }  // if devcards
        }  // for each player

        /**
         * send game-length and connect-length messages
         */
        {
            Date now = new Date();
            Date gstart = ga.getStartTime();
            String gLengthMsg;
            if (gstart != null)
            {                
                long gameSeconds = ((now.getTime() - gstart.getTime())+500L) / 1000L;
                long gameMinutes = gameSeconds/60L;
                gameSeconds = gameSeconds % 60L;
                if (gameSeconds == 0)
                    gLengthMsg = "This game took " + gameMinutes + " minutes.";
                else
                    gLengthMsg = "This game took " + gameMinutes + " minutes "
                        + gameSeconds + " seconds.";
                messageToGame(gname, new SOCGameTextMsg(gname, SERVERNAME, gLengthMsg));

                // Ignore possible "1 minutes"; that game is too short to worry about.
            } else {
                gLengthMsg = null;
            }

            /**
             * Tell each player how long they've been connected.
             */
            String connMsg;
            if ((strSocketName != null) && (strSocketName.equals(PRACTICE_STRINGPORT)))
                connMsg = "You have been practicing ";
            else
                connMsg = "You have been connected ";

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                SOCPlayer pl = ga.getPlayer(i);
                if (pl.isRobot() || ga.isSeatVacant(i))
                    continue;  // Don't bother to send timing stats to robots, or to empty seats

                StringConnection plConn = (StringConnection) conns.get(pl.getName()); 
                if (plConn != null)
                {
                    long connTime = plConn.getConnectTime().getTime();
                    long connMinutes = (((now.getTime() - connTime)) + 30000L) / 60000L;                    
                    StringBuffer cLengthMsg = new StringBuffer(connMsg);
                    cLengthMsg.append(connMinutes);
                    if (connMinutes == 1)
                        cLengthMsg.append(" minute.");
                    else
                        cLengthMsg.append(" minutes.");
                    messageToPlayer(plConn, new SOCGameTextMsg(ga.getName(), SERVERNAME, cLengthMsg.toString()));
                }
            }  // for each player

        }  // send game timing stats
    }

    /**
     * report a trade that has taken place between players, using {@link SOCPlayerElement}
     * and {@link SOCGameTextMsg} messages.  Trades are also reported to robots
     * by re-sending the accepting player's {@link SOCAcceptOffer} message.
     *
     * @param ga        the game
     * @param offering  the number of the player making the offer
     * @param accepting the number of the player accepting the offer
     *
     * @see #reportBankTrade(SOCGame, SOCResourceSet, SOCResourceSet)
     */
    protected void reportTrade(SOCGame ga, int offering, int accepting)
    {
        if (ga != null)
        {
            final String gaName = ga.getName();
            final SOCTradeOffer offer = ga.getPlayer(offering).getCurrentOffer();

            StringBuffer message = new StringBuffer(ga.getPlayer(offering).getName());
            message.append(" traded ");
            reportTradeHalf(gaName, offer.getGiveSet(), true, offering, accepting, message);
            message.append(" for ");
            reportTradeHalf(gaName, offer.getGetSet(), false, offering, accepting, message);
            message.append(" from ");
            message.append(ga.getPlayer(accepting).getName());
            message.append('.');
            messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, message.toString()));
        }
    }

    /**
     * report that the current player traded with the bank or a port,
     * using {@link SOCPlayerElement} and {@link SOCGameTextMsg} messages.
     *
     * @param ga        the game
     * @param give      the number of the player making the offer
     * @param get       the number of the player accepting the offer
     *
     * @see #reportTrade(SOCGame, int, int)
     */
    protected void reportBankTrade(SOCGame ga, SOCResourceSet give, SOCResourceSet get)
    {
        if (ga != null)
        {
            final String gaName = ga.getName();
            final int    cpn    = ga.getCurrentPlayerNumber();
            StringBuffer message = new StringBuffer (ga.getPlayer(cpn).getName());
            message.append(" traded ");
            reportTradeHalf(gaName, give, true, cpn, -1, message);
            message.append(" for ");
            reportTradeHalf(gaName, get, false, cpn, -1, message);

            if ((give.getTotal() / get.getTotal()) == 4)
            {
                message.append(" from the bank.");  // 4:1 trade
            }
            else
            {
                message.append(" from a port.");    // 3:1 or 2:1 trade
            }

            messageToGame(gaName, new SOCGameTextMsg(gaName, SERVERNAME, message.toString()));
        }
    }

    /**
     * Report the player's resources gained from a roll, via
     * messageToGame(PLAYERELEMENT) messages,
     * and build the resource-amount string used to report the trade as text.
     *
     * @param gaName  Game name
     * @param rset    Resources set ("give" or "get" side of trade)
     * @param pnA     Player number getting resources
     * @param message Append resource numbers/types to this stringbuffer,
     *                format like "3 clay,3 wood"
     *
     * @see #reportTradeHalf(String, SOCResourceSet, boolean, int, int, StringBuffer)
     */
    private void reportRolledResources
        (String gaName, SOCResourceSet rset, int pn, StringBuffer message)
    {
        reportTradeHalf(gaName, rset, false, pn, -1, message);
    }

    /**
     * Report the "give" or "get" half of a resource trade, via
     * messageToGame(PLAYERELEMENT) messages,
     * and build the resource-amount string used to report the trade as text.
     * Also used to report the resources gained from a roll.
     *
     * @param gaName  Game name
     * @param rset    Resources set ("give" or "get" side of trade)
     * @param isGive  If true, "give" ({@link SOCPlayerElement#LOSE}), otherwise "get" ({@link SOCPlayerElement#GAIN})
     * @param pnA     Player number "giving" if isGive==true, otherwise "getting".
     * @param pnB     Player number on other side of trade, or -1 if bank.    
     * @param message Append resource numbers/types to this stringbuffer,
     *                format like "3 clay,3 wood"
     *
     * @see #reportTrade(SOCGame, int, int)
     * @see #reportBankTrade(SOCGame, SOCResourceSet, SOCResourceSet)
     * @see #reportRolledResources(String, SOCResourceSet, int, StringBuffer)
     */
    private void reportTradeHalf
        (String gaName, SOCResourceSet rset, boolean isGive, int pnA, int pnB, StringBuffer message)
    {
        final int losegain  = isGive ? SOCPlayerElement.LOSE : SOCPlayerElement.GAIN;  // for pnA
        final int gainlose  = isGive ? SOCPlayerElement.GAIN : SOCPlayerElement.LOSE;  // for pnB

        final int cl = rset.getAmount(SOCResourceConstants.CLAY);
        final int or = rset.getAmount(SOCResourceConstants.ORE);
        final int sh = rset.getAmount(SOCResourceConstants.SHEEP);
        final int wh = rset.getAmount(SOCResourceConstants.WHEAT);
        final int wo = rset.getAmount(SOCResourceConstants.WOOD);
        
        boolean needComma = false;  // Has a resource already been appended to message?

        gameList.takeMonitorForGame(gaName);

        if (cl > 0)
        {
            messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnA, losegain, SOCPlayerElement.CLAY, cl));
            if (pnB != -1)
                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnB, gainlose, SOCPlayerElement.CLAY, cl));
            message.append(cl);
            message.append(" clay");
            needComma = true;
        }

        if (or > 0)
        {
            messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnA, losegain, SOCPlayerElement.ORE, or));
            if (pnB != -1)
                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnB, gainlose, SOCPlayerElement.ORE, or));
            if (needComma)
                message.append(',');
            message.append(or);
            message.append(" ore");
            needComma = true;
        }

        if (sh > 0)
        {
            messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnA, losegain, SOCPlayerElement.SHEEP, sh));
            if (pnB != -1)
                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnB, gainlose, SOCPlayerElement.SHEEP, sh));
            if (needComma)
                message.append(',');
            message.append(sh);
            message.append(" sheep");
            needComma = true;
        }

        if (wh > 0)
        {
            messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnA, losegain, SOCPlayerElement.WHEAT, wh));
            if (pnB != -1)
                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnB, gainlose, SOCPlayerElement.WHEAT, wh));
            if (needComma)
                message.append(',');
            message.append(wh);
            message.append(" wheat");
            needComma = true;
        }

        if (wo > 0)
        {
            messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnA, losegain, SOCPlayerElement.WOOD, wo));
            if (pnB != -1)
                messageToGameWithMon(gaName, new SOCPlayerElement(gaName, pnB, gainlose, SOCPlayerElement.WOOD, wo));
            if (needComma)
                message.append(',');
            message.append(wo);
            message.append(" wood");
        }

        gameList.releaseMonitorForGame(gaName);
    }

    /**
     * make sure it's the player's turn
     *
     * @param c  the connection
     * @param ga the game
     *
     * @return true if it is the player's turn;
     *         false if another player's turn, or if this player isn't in the game
     */
    protected boolean checkTurn(StringConnection c, SOCGame ga)
    {
        if ((c != null) && (ga != null))
        {
            try
            {
                if (ga.getCurrentPlayerNumber() != ga.getPlayer((String) c.getData()).getPlayerNumber())
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            catch (Exception e)
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    /**
     * do the stuff you need to do to start a game
     *
     * @param ga  the game
     */
    protected void startGame(SOCGame ga)
    {
        if (ga != null)
        {
            final String gaName = ga.getName();

            numberOfGamesStarted++;
            ga.startGame();
            gameList.takeMonitorForGame(gaName);

            /**
             * send the board layout
             */
            SOCBoardLayout bl = getBoardLayoutMessage(ga);
            messageToGameWithMon(gaName, bl);

            /**
             * send the player info
             */            
            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                if (! ga.isSeatVacant(i))
                {
                    SOCPlayer pl = ga.getPlayer(i);
                    messageToGameWithMon(gaName, new SOCPlayerElement(gaName, i, SOCPlayerElement.SET, SOCPlayerElement.ROADS, pl.getNumPieces(SOCPlayingPiece.ROAD)));
                    messageToGameWithMon(gaName, new SOCPlayerElement(gaName, i, SOCPlayerElement.SET, SOCPlayerElement.SETTLEMENTS, pl.getNumPieces(SOCPlayingPiece.SETTLEMENT)));
                    messageToGameWithMon(gaName, new SOCPlayerElement(gaName, i, SOCPlayerElement.SET, SOCPlayerElement.CITIES, pl.getNumPieces(SOCPlayingPiece.CITY)));
                    messageToGameWithMon(gaName, new SOCSetPlayedDevCard(gaName, i, false));
                }
            }

            /**
             * send the number of dev cards
             */
            messageToGameWithMon(gaName, new SOCDevCardCount(gaName, ga.getNumDevCards()));

            /**
             * ga.startGame() picks who goes first, but feedback is nice
             */
            messageToGameWithMon(gaName, new SOCGameTextMsg(gaName, SERVERNAME, "Randomly picking a starting player..."));

            gameList.releaseMonitorForGame(gaName);

            /**
             * send the game state
             */
            sendGameState(ga, false);

            /**
             * start the game
             */
            messageToGame(gaName, new SOCStartGame(gaName));

            /**
             * send whose turn it is
             */
            sendTurn(ga, false);
        }
    }

    /**
     * Reset the board to a copy with same players but new layout.
     *<OL>
     * <LI value=1> Reset the board, remember player positions.
     * <LI value=2> Send ResetBoardAuth to each client (like sending JoinGameAuth at new game)
     *    Humans will reset their copy of the game.
     *    Robots will leave the game and request to re-join.
     *    (This simplifies the robot client.)
     * <LI value=3> Send messages as if each human player has clicked "join" (except JoinGameAuth)
     * <LI value=4> Send as if each human player has clicked "sit here"
     * <LI value=5a> If no robots, send to game as if someone else has
     *              clicked "start game", and set up state to begin game play.
     * <LI value=5b>  If there are robots, If there are robots, set up wait-request
     *     queue (robotJoinRequests). Game will wait for robots to send
     *     JOINGAME and SITDOWN, as they do when joining a newly created game.
     *     Once all robots have re-joined, the game will begin.
     *</OL>
     * MUST be called ONLY from the one thread handling all inbound messages,
     * or (race condition) the robots may ask to join before we've
     * set up robotJoinRequests. 
     */
    private void resetBoardAndNotify (String gaName, int requestingPlayer)
    {
        /**
         * 1. Reset the board, remember player positions.
         *    Takes the monitorForGame if exists.
         */
        SOCGameBoardReset reBoard = gameList.resetBoard(gaName);
        if (reBoard == null)
            return;
        SOCGame reGame = reBoard.newGame;

        // Must release before calling methods below
        gameList.releaseMonitorForGame(gaName);

        messageToGameUrgent(gaName, ">>> Game " + gaName + " board reset by "
            + reGame.getPlayer(requestingPlayer).getName());

        /**
         * Player connection data:
         * - Humans are copied from old to new game
         * - Robots aren't copied to new game, must re-join
         */
        StringConnection[] huConns = reBoard.humanConns;
        StringConnection[] roConns = reBoard.robotConns;

        /**
         * Notify old game's players. (Humans and robots)
         *
         * 1. Send ResetBoardAuth to each (like sending JoinGameAuth at new game).
         *    Humans will reset their copy of the game.
         *    Robots will leave the game and request to re-join.
         */
        for (int pn = 0; pn < SOCGame.MAXPLAYERS; ++pn)
        {
            SOCResetBoardAuth resetMsg = new SOCResetBoardAuth(gaName, pn, requestingPlayer);
            if (huConns[pn] != null)
                messageToPlayer(huConns[pn], resetMsg);
            else if (roConns[pn] != null)
                messageToPlayer(roConns[pn], resetMsg);
        }

        /**
         * 2. Send messages as if each human player has clicked "join" (except JoinGameAuth)
         */
        for (int pn = 0; pn < SOCGame.MAXPLAYERS; ++pn)
        {
            if (huConns[pn] != null)
                joinGame(reGame, huConns[pn], true);
        }

        /**
         * 3. Send as if each human player has clicked "sit here"
         */
        for (int pn = 0; pn < SOCGame.MAXPLAYERS; ++pn)
        {
            if (huConns[pn] != null)
                sitDown(reGame, huConns[pn], pn, false /* isRobot*/, true /*isReset */ );
        }

        /**
         * 4a. If no robots, send to game as if someone else has
         *     clicked "start game", and set up state to begin game play.
         */
        if (! reBoard.hadRobots)
            startGame (reGame);

        /**
         * 4b. If there are robots, set up wait-request queue
         *     (robotJoinRequests) and ask robots to re-join.
         *     Game will wait for robots to send JOINGAME and SITDOWN,
         *     as they do when joining a newly created game.
         *     Once all robots have re-joined, the game will begin.
         */
        else
        {
            reGame.setGameState(SOCGame.READY);
            readyGameAskRobotsJoin(reGame, reBoard.robotConns);
        }

        // All set.
    }

    /**
     * send whose turn it is. Optionally also send a prompt to roll. 
     *
     * @param ga  the game
     * @param sendRollPrompt  whether to send a RollDicePrompt message afterwards
     */
    private void sendTurn(SOCGame ga, boolean sendRollPrompt)
    {
        if (ga != null)
        {
            String gname = ga.getName();
            int pn = ga.getCurrentPlayerNumber();   

            messageToGame(gname, new SOCSetPlayedDevCard(gname, pn, false));

            SOCTurn turnMessage = new SOCTurn(gname, pn);
            messageToGame(gname, turnMessage);
            recordGameEvent(gname, turnMessage.toCmd());
            
            if (sendRollPrompt)
                messageToGame(gname, new SOCRollDicePrompt(gname, pn));
        }
    }

    /**
     * put together the SOCBoardLayout message
     *
     * @param  ga   the game
     * @return      a board layout message
     */
    private SOCBoardLayout getBoardLayoutMessage(SOCGame ga)
    {
        SOCBoard board;
        int[] hexes;
        int[] numbers;
        int robber;

        board = ga.getBoard();
        hexes = board.getHexLayout();
        numbers = board.getNumberLayout();
        robber = board.getRobberHex();

        return (new SOCBoardLayout(ga.getName(), hexes, numbers, robber));
    }

    /**
     * create a new game event record
     */
    private void createNewGameEventRecord()
    {
        /*
           currentGameEventRecord = new SOCGameEventRecord();
           currentGameEventRecord.setTimestamp(new Date());
         */
    }

    /**
     * save the current game event record in the game record
     *
     * @param gn  the name of the game
     */
    private void saveCurrentGameEventRecord(String gn)
    {
        /*
           SOCGameRecord gr = (SOCGameRecord)gameRecords.get(gn);
           SOCGameEventRecord ger = currentGameEventRecord.myClone();
           gr.addEvent(ger);
         */
    }

    /**
     * write a gameRecord out to disk
     *
     * @param na  the name of the record
     * @param gr  the game record
     */

    /*
       private void writeGameRecord(String na, SOCGameRecord gr) {
       FileOutputStream os = null;
       ObjectOutput output = null;
    
       try {
       Date theTime = new Date();
       os = new FileOutputStream("dataFiles/"+na+"."+theTime.getTime());
       output = new ObjectOutputStream(os);
       } catch (Exception e) {
       D.ebugPrintln(e.toString());
       D.ebugPrintln("Unable to open output stream.");
       }
       try{
       output.writeObject(gr);
       // D.ebugPrintln("*** Wrote "+na+" out to disk. ***");
       output.close();
       } catch (Exception e) {
       D.ebugPrintln(e.toString());
       D.ebugPrintln("Unable to write game record to disk.");
       }
       }
     */

    /**
     * if all the players stayed for the whole game,
     * record the scores in the database
     *
     * @param ga  the game
     */
    protected void storeGameScores(SOCGame ga)
    {
        if (ga != null)
        {
            //D.ebugPrintln("allOriginalPlayers for "+ga.getName()+" : "+ga.allOriginalPlayers());
            if ((ga.getGameState() == SOCGame.OVER) && (ga.allOriginalPlayers()))
            {
                //if (ga.allOriginalPlayers()) {				
                try
                {
                    SOCDBHelper.saveGameScores(ga.getName(), ga.getPlayer(0).getName(), ga.getPlayer(1).getName(), ga.getPlayer(2).getName(), ga.getPlayer(3).getName(), (short) ga.getPlayer(0).getTotalVP(), (short) ga.getPlayer(1).getTotalVP(), (short) ga.getPlayer(2).getTotalVP(), (short) ga.getPlayer(3).getTotalVP(), ga.getStartTime());
                }
                catch (SQLException sqle)
                {
                    System.err.println("Error saving game scores in db.");
                }
            }
        }
    }

    /**
     * record events that happen during the game
     *
     * @param gameName   the name of the game
     * @param event      the event
     */
    protected void recordGameEvent(String gameName, String event)
    {
        /*
           FileWriter fw = (FileWriter)gameDataFiles.get(gameName);
           if (fw != null) {
           try {
           fw.write(event+"\n");
           //D.ebugPrintln("WROTE |"+event+"|");
           } catch (Exception e) {
           D.ebugPrintln(e.toString());
           D.ebugPrintln("Unable to write to disk.");
           }
           }
         */
    }

    /**
     * this is a debugging command that gives resources to a player
     */
    protected void giveResources(String mes, SOCGame game)
    {
        StringTokenizer st = new StringTokenizer(mes.substring(6));
        int[] resources = new int[SOCResourceConstants.WOOD + 1];
        int resourceType = SOCResourceConstants.CLAY;
        String name = "";

        while (st.hasMoreTokens())
        {
            String token = st.nextToken();

            if (resourceType <= SOCResourceConstants.WOOD)
            {
                resources[resourceType] = Integer.parseInt(token);
                resourceType++;
            }
            else
            {
                name = token;

                break;
            }
        }

        SOCResourceSet rset = game.getPlayer(name).getResources();
        int pnum = game.getPlayer(name).getPlayerNumber();
        String outMes = "### " + name + " gets";

        for (resourceType = SOCResourceConstants.CLAY;
                resourceType <= SOCResourceConstants.WOOD; resourceType++)
        {
            rset.add(resources[resourceType], resourceType);
            outMes += (" " + resources[resourceType]);

            switch (resourceType)
            {
            case SOCResourceConstants.CLAY:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.CLAY, resources[resourceType]));

                break;

            case SOCResourceConstants.ORE:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.ORE, resources[resourceType]));

                break;

            case SOCResourceConstants.SHEEP:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.SHEEP, resources[resourceType]));

                break;

            case SOCResourceConstants.WHEAT:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.WHEAT, resources[resourceType]));

                break;

            case SOCResourceConstants.WOOD:
                messageToGame(game.getName(), new SOCPlayerElement(game.getName(), pnum, SOCPlayerElement.GAIN, SOCPlayerElement.WOOD, resources[resourceType]));

                break;
            }
        }

        messageToGame(game.getName(), new SOCGameTextMsg(game.getName(), SERVERNAME, outMes));
    }

    /**
     * this broadcasts game information to all people connected
     * used to display the scores on the player client
     */
    protected void broadcastGameStats(SOCGame ga)
    {
        /*
           if (ga != null) {
           int scores[] = new int[SOCGame.MAXPLAYERS];
           boolean robots[] = new boolean[SOCGame.MAXPLAYERS];
           for (int i = 0; i < SOCGame.MAXPLAYERS; i++) {
           SOCPlayer player = ga.getPlayer(i);
           if (player != null) {
           if (ga.isSeatVacant(i)) {
           scores[i] = -1;
           robots[i] = false;
           } else {
           scores[i] = player.getPublicVP();
           robots[i] = player.isRobot();
           }
           } else {
           scores[i] = -1;
           }
           }
        
           broadcast(SOCGameStats.toCmd(ga.getName(), scores, robots));
           }
         */
    }

    /**
     * check for games that have expired and destroy them.
     * If games are about to expire, send a warning.
     *
     * @see #GAME_EXPIRE_WARN_MINUTES
     * @see SOCGameTimeoutChecker#run()
     */
    public void checkForExpiredGames()
    {
        Vector expired = new Vector();

        gameList.takeMonitor();
        
        // Add 2 minutes because of coarse 5-minute granularity in SOCGameTimeoutChecker.run()
        long warn_ms = (2 + GAME_EXPIRE_WARN_MINUTES) * 60L * 1000L; 

        try
        {
            for (Enumeration k = gameList.getGames(); k.hasMoreElements();)
            {
                String gameName = (String) k.nextElement();
                SOCGame gameData = gameList.getGameData(gameName);
                long gameExpir = gameData.getExpiration();

                // Start our text messages with ">>>" to mark as urgent to the client.

                if (gameExpir <= System.currentTimeMillis())
                {
                    expired.addElement(gameName);
                    messageToGameUrgent(gameName, ">>> The time limit on this game has expired and will now be destroyed.");
                }
                else
                //
                //  Give people a few minutes' warning (they may have a few warnings)
                //
                if ((gameExpir - warn_ms) <= System.currentTimeMillis())
                {
                    long minutes = ((gameExpir - System.currentTimeMillis()) / 60000);
                    messageToGameUrgent(gameName, ">>> Less than "
                        + minutes + " minutes remaining.  Type *ADDTIME* to extend this game another 30 minutes.");
                }
            }
        }
        catch (Exception e)
        {
            D.ebugPrintln("Exception in checkForExpiredGames - " + e);
        }

        gameList.releaseMonitor();

        //
        // destroy the expired games
        //
        for (Enumeration ex = expired.elements(); ex.hasMoreElements();)
        {
            String ga = (String) ex.nextElement();
            gameList.takeMonitor();

            try
            {
                destroyGame(ga);
            }
            catch (Exception e)
            {
                D.ebugPrintln("Exception in checkForExpired - " + e);
            }

            gameList.releaseMonitor();
            broadcast(SOCDeleteGame.toCmd(ga));
        }
    }

    /** this is a debugging command that gives a dev card to a player
     */
    protected void giveDevCard(String mes, SOCGame game)
    {
        StringTokenizer st = new StringTokenizer(mes.substring(5));
        String name = "";
        int cardType = -1;

        while (st.hasMoreTokens())
        {
            String token = st.nextToken();

            if (cardType < 0)
            {
                cardType = Integer.parseInt(token);
            }
            else
            {
                name = token;

                break;
            }
        }

        SOCDevCardSet dcSet = game.getPlayer(name).getDevCards();
        dcSet.add(1, SOCDevCardSet.NEW, cardType);

        int pnum = game.getPlayer(name).getPlayerNumber();
        String outMes = "### " + name + " gets a " + cardType + " card.";
        messageToGame(game.getName(), new SOCDevCard(game.getName(), pnum, SOCDevCard.DRAW, cardType));
        messageToGame(game.getName(), new SOCGameTextMsg(game.getName(), SERVERNAME, outMes));
    }

    /**
     * Starting the server from the command line
     *
     * @param args  arguments: port number
     */
    static public void main(String[] args)
    {
        int port;
        int mc;

        if (args.length < 4)
        {
            System.err.println("usage: java soc.server.SOCServer port_number max_connections dbUser dbPass");

            return;
        }

        try
        {
            port = Integer.parseInt(args[0]);
            mc = Integer.parseInt(args[1]);
        }
        catch (Exception e)
        {
            System.err.println("usage: java soc.server.SOCServer port_number max_connections dbUser dbPass");

            return;
        }

        SOCServer server = new SOCServer(port, mc, args[2], args[3]);
        server.setPriority(5);
        server.start();
    }
}
