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
package soc.game;

import soc.disableDebug.D;

import soc.util.IntPair;

import java.io.Serializable;

import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;


/**
 * A class for holding and manipulating game data.
 * 
 * putPiece updates the game state.
 *
 * @author Robert S. Thomas
 */
public class SOCGame implements Serializable, Cloneable
{
    /**
     * game states
     */
    public static final int NEW = 0; // Brand new game
    public static final int READY = 1; // Ready to start playing
    public static final int START1A = 5; // Players place 1st stlmt
    public static final int START1B = 6; // Players place 1st road
    public static final int START2A = 10; // Players place 2nd stlmt
    public static final int START2B = 11; // Players place 2nd road
    public static final int PLAY = 15; // Play continues normally; time to roll or play card
    public static final int PLAY1 = 20; // Done rolling
    public static final int PLACING_ROAD = 30;
    public static final int PLACING_SETTLEMENT = 31;
    public static final int PLACING_CITY = 32;
    public static final int PLACING_ROBBER = 33;
    public static final int PLACING_FREE_ROAD1 = 40; // Player is placing first road
    public static final int PLACING_FREE_ROAD2 = 41; // Player is placing second road
    public static final int WAITING_FOR_DISCARDS = 50; // Waiting for players to discard
    public static final int WAITING_FOR_CHOICE = 51; // Waiting for player to choose a player
    public static final int WAITING_FOR_DISCOVERY = 52; // Waiting for player to choose 2 resources
    public static final int WAITING_FOR_MONOPOLY = 53; // Waiting for player to choose a resource
    public static final int OVER = 1000; // The game is over
    /**
     * This game is an obsolete old copy of a new (reset) game with the same name.
     * To assist logic, constant value is greater than OVER.
     * @see #resetAsCopy()
     * @see #getResetOldGameState()
     */
    public static final int RESET_OLD = 1302;

    /**
     * seat states
     */
    public static final int VACANT = 0;
    public static final int OCCUPIED = 1;
    /**
     * seatLock states
     */
    public static final boolean LOCKED = true;
    public static final boolean UNLOCKED = false;

    /**
     * maximum number of players in a game
     */
    public static final int MAXPLAYERS = 4;

    /**
     * minimum number of players in a game (was assumed ==MAXPLAYERS in standard 1.0.6).
     * Use isSeatVacant(i) to determine if a player is present;
     * players[i] will be non-null although no player is there.
     */
    public static final int MINPLAYERS = 2;

    /**
     * the set of resources a player needs to build a settlement
     */
    public static final SOCResourceSet EMPTY_RESOURCES = new SOCResourceSet();

    /**
     * the set of resources a player needs to build a settlement
     */
    public static final SOCResourceSet SETTLEMENT_SET = new SOCResourceSet(1, 0, 1, 1, 1, 0);

    /**
     * the set of resources a player needs to build a road
     */
    public static final SOCResourceSet ROAD_SET = new SOCResourceSet(1, 0, 0, 0, 1, 0);

    /**
     * the set of resources a player needs to build a city
     */
    public static final SOCResourceSet CITY_SET = new SOCResourceSet(0, 3, 0, 2, 0, 0);

    /**
     * the set of resources a player needs to buy a development card
     */
    public static final SOCResourceSet CARD_SET = new SOCResourceSet(0, 1, 1, 1, 0, 0);

    /**
     * monitor for synchronization
     */
    boolean inUse;

    /**
     * the name of the game
     */
    private String name;

    /**
     * true if this game is ACTIVE
     */
    private boolean active;

    /**
     * true if the game's network is local for practice (used only at client)
     */
    public boolean isLocal;

    /**
     * true if the game came from a board reset
     */
    private boolean isFromBoardReset;

    /**
     * If a board reset vote is active, player number who requested the vote.
     * All human players must vote unanimously, or board reset is rejected.
     * -1 if no vote is active.
     * Synchronize on {@link #boardResetVotes} before reading or writing.
     */
    private int boardResetVoteRequester;

    /**
     * If a board reset vote is active, votes are recorded here.
     * 0 for no vote yet.  1 for yes.  2 for no.
     * Indexed 0 to SOCGame.MAXPLAYERS-1.
     * Synchronize on this object before reading or writing.
     */
    private int boardResetVotes[];

    /**
     * If a board reset vote is active, we're waiting to receive this many more votes.
     * All human players vote, except the vote requester. Robots do not vote.
     * Synchronize on {@link #boardResetVotes} before reading or writing.
     * When the vote is complete, this is 0.
     * Undefined before the first vote; this field is not reset each turn.
     * Set in resetVoteBegin, resetVoteRegister.
     */
    private int boardResetVotesWaiting;

    /**
     * the game board
     */
    private SOCBoard board;

    /**
     * the players
     */
    private SOCPlayer[] players;

    /**
     * the states for the player's seats
     */
    private int[] seats;

    /**
     * the states if the locks for the player's seats
     */
    private boolean[] seatLocks;

    /**
     * the number of the current player
     */
    private int currentPlayerNumber;

    /**
     * the first player to place a settlement
     */
    private int firstPlayerNumber;

    /**
     * the last player to place the first settlement
     */
    private int lastPlayerNumber;

    /**
     * the current dice result
     */
    private int currentDice;

    /**
     * the current game state
     */
    private int gameState;

    /**
     * the old game state
     */
    private int oldGameState;

    /**
     * the player with the largest army, or -1 if none
     */
    private int playerWithLargestArmy;
    private int oldPlayerWithLargestArmy;

    /**
     * the player with the longest road, or -1 if none
     */
    private int playerWithLongestRoad;

    /**
     * the player declared winner, if gamestate == OVER; otherwise -1
     */
    private int playerWithWin;

    /**
     * the number of development cards left
     */
    private int numDevCards;

    /**
     * the development card deck
     */
    private int[] devCardDeck;

    /**
     * used to generate random numbers
     */
    private Random rand = new Random();

    /**
     * used to track if there were any player subs
     */
    boolean allOriginalPlayers;

    /**
     * used to restore the LR player
     */
    Stack oldPlayerWithLongestRoad;

    /**
     * when this game was created
     */
    Date startTime;

    /**
     * expiration time for this game in milliseconds
     */
    long expiration;

    /**
     * create a new game
     *
     * @param n  the name of the game
     */
    public SOCGame(String n)
    {
        active = true;
        inUse = false;
        name = n;
        board = new SOCBoard();
        players = new SOCPlayer[MAXPLAYERS];
        seats = new int[MAXPLAYERS];
        seatLocks = new boolean[MAXPLAYERS];
        boardResetVotes = new int[MAXPLAYERS];

        for (int i = 0; i < MAXPLAYERS; i++)
        {
            players[i] = new SOCPlayer(i, this);
            seats[i] = VACANT;
            seatLocks[i] = UNLOCKED;
        }

        currentPlayerNumber = -1;
        firstPlayerNumber = -1;
        currentDice = -1;
        playerWithLargestArmy = -1;
        playerWithLongestRoad = -1;
        boardResetVoteRequester = -1;
        playerWithWin = -1;
        numDevCards = 25;
        gameState = NEW;
        oldPlayerWithLongestRoad = new Stack();
        startTime = new Date();
    }

    /**
     * create a new game that can be INACTIVE
     *
     * @param n  the name of the game
     * @param a  true if this is an active game, false for inactive
     */
    public SOCGame(String n, boolean a)
    {
        active = a;
        inUse = false;
        name = n;
        board = new SOCBoard();
        players = new SOCPlayer[MAXPLAYERS];
        seats = new int[MAXPLAYERS];
        seatLocks = new boolean[MAXPLAYERS];
        boardResetVotes = new int[MAXPLAYERS];

        for (int i = 0; i < MAXPLAYERS; i++)
        {
            players[i] = new SOCPlayer(i, this);
            seats[i] = VACANT;
            seatLocks[i] = UNLOCKED;
        }

        currentPlayerNumber = -1;
        firstPlayerNumber = -1;
        currentDice = -1;
        playerWithLargestArmy = -1;
        playerWithLongestRoad = -1;
        boardResetVoteRequester = -1;
        numDevCards = 25;
        gameState = NEW;
        oldPlayerWithLongestRoad = new Stack();
        if (active)
            startTime = new Date();
    }

    /**
     * take the monitor for this game
     */
    public synchronized void takeMonitor()
    {
        //D.ebugPrintln("TAKE MONITOR");
        while (inUse)
        {
            try
            {
                wait(1000);
            }
            catch (InterruptedException e)
            {
                System.out.println("EXCEPTION IN takeMonitor() -- " + e);
            }
        }

        inUse = true;
    }

    /**
     * release the monitor for this game
     */
    public synchronized void releaseMonitor()
    {
        //D.ebugPrintln("RELEASE MONITOR");
        inUse = false;
        this.notify();
    }

    /**
     * @return allOriginalPlayers
     */
    public boolean allOriginalPlayers()
    {
        return allOriginalPlayers;
    }

    /**
     * @return the start time for this game, or null if inactive
     */
    public Date getStartTime()
    {
        return startTime;
    }

    /**
     * @return the expiration time
     */
    public long getExpiration()
    {
        return expiration;
    }

    /**
     * set the expiration time
     *
     * @param ex  the expiration time in milliseconds
     */
    public void setExpiration(long ex)
    {
        expiration = ex;
    }

    /**
     * add a new player
     *
     * @param name  the player's name
     * @param pn    the player's number
     */
    public void addPlayer(String name, int pn)
    {
        players[pn].setName(name);
        seats[pn] = OCCUPIED;

        if ((gameState > NEW) && (gameState < OVER))
        {
            allOriginalPlayers = false;
        }
    }

    /**
     * remove a player
     *
     * @param name  the player's name
     */
    public void removePlayer(String name)
    {
        SOCPlayer pl = getPlayer(name);
        pl.setName(null);
        seats[pl.getPlayerNumber()] = VACANT;

        //D.ebugPrintln("seats["+pl.getPlayerNumber()+"] = VACANT");
    }

    /**
     * @return true if the seat is VACANT
     *
     * @param pn the number of the seat
     */
    public boolean isSeatVacant(int pn)
    {
        return (seats[pn] == VACANT);
    }

    /**
     * locks a seat, so no one can take it
     *
     * @param pn the number of the seat
     */
    public void lockSeat(int pn)
    {
        seatLocks[pn] = LOCKED;
    }

    /**
     * unlocks a seat
     *
     * @param pn the number of the seat
     */
    public void unlockSeat(int pn)
    {
        seatLocks[pn] = UNLOCKED;
    }

    /**
     * @return true if this seat is locked
     *
     * @param pn the number of the seat
     */
    public boolean isSeatLocked(int pn)
    {
        return (seatLocks[pn] == LOCKED);
    }

    /**
     * @return the player object for a player id
     *
     * @param pn  the player number
     */
    public SOCPlayer getPlayer(int pn)
    {
        return players[pn];
    }

    /**
     * @return the player object for a player nickname
     * if there is no match, return null
     *
     * @param nn  the nickname
     */
    public SOCPlayer getPlayer(String nn)
    {
        if (nn != null)
        {
            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                if ((nn.equals(players[i].getName())) && ! isSeatVacant(i))
                {
                    return players[i];
                }
            }
        }

        return null;
    }

    /**
     * @return the name of the game
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return whether this game was created by board reset of an earlier game
     */
    public boolean isBoardReset()
    {
        return isFromBoardReset;
    }

    /**
     * @return the game board
     */
    public SOCBoard getBoard()
    {
        return board;
    }

    /**
     * set the game board
     *
     * @param gb  the game board
     */
    protected void setBoard(SOCBoard gb)
    {
        board = gb;
    }

    /**
     * @return the list of players
     */
    public SOCPlayer[] getPlayers()
    {
        return players;
    }

    /**
     * set the data for a player
     *
     * @param pn  the number of the player
     * @param pl  the player data
     */
    protected void setPlayer(int pn, SOCPlayer pl)
    {
        players[pn] = pl;
    }

    /**
     * @return the number of the current player
     */
    public int getCurrentPlayerNumber()
    {
        return currentPlayerNumber;
    }

    /**
     * set the number of the current player
     *
     * @param pn  the player number
     */
    public void setCurrentPlayerNumber(int pn)
    {
        //D.ebugPrintln("SETTING CURRENT PLAYER NUMBER TO "+pn);
        currentPlayerNumber = pn;
    }

    /**
     * @return the current dice result
     */
    public int getCurrentDice()
    {
        return currentDice;
    }

    /**
     * set the current dice result
     *
     * @param dr  the dice result
     */
    public void setCurrentDice(int dr)
    {
        currentDice = dr;
    }

    /**
     * @return the current game state
     */
    public int getGameState()
    {
        return gameState;
    }

    /**
     * set the current game state.
     * If the new state is OVER, and no playerWithWin yet determined, call checkForWinner.
     *
     * @param gs  the game state
     * @see #checkForWinner()
     */
    public void setGameState(int gs)
    {
        gameState = gs;
        if ((gameState == OVER) && (playerWithWin == -1))
            checkForWinner();
    }
   
    /**
     * If the game board was reset, get the old game state.
     *
     * @return the old game state
     * @throws IllegalStateException Game state must be RESET_OLD
     *    when called; during normal game play, oldGameState is private.
     */
    public int getResetOldGameState() throws IllegalStateException
    {
        if (gameState != RESET_OLD)
            throw new IllegalStateException
                ("Current state is not RESET_OLD: " + gameState);

        return oldGameState;
    }

    /**
     * @return the number of dev cards in the deck
     */
    public int getNumDevCards()
    {
        return numDevCards;
    }

    /**
     * set the number of dev cards in the deck
     *
     * @param  nd  the number of dev cards in the deck
     */
    public void setNumDevCards(int nd)
    {
        numDevCards = nd;
    }

    /**
     * @return the player with the largest army
     */
    public SOCPlayer getPlayerWithLargestArmy()
    {
        if (playerWithLargestArmy != -1)
        {
            return players[playerWithLargestArmy];
        }
        else
        {
            return null;
        }
    }

    /**
     * set the player with the largest army
     *
     * @param pl  the player
     */
    public void setPlayerWithLargestArmy(SOCPlayer pl)
    {
        if (pl == null)
        {
            playerWithLargestArmy = -1;
        }
        else
        {
            playerWithLargestArmy = pl.getPlayerNumber();
        }
    }

    /**
     * @return the player with the longest road
     */
    public SOCPlayer getPlayerWithLongestRoad()
    {
        if (playerWithLongestRoad != -1)
        {
            return players[playerWithLongestRoad];
        }
        else
        {
            return null;
        }
    }

    /**
     * set the player with the longest road
     *
     * @param pl  the player
     */
    public void setPlayerWithLongestRoad(SOCPlayer pl)
    {
        if (pl == null)
        {
            playerWithLongestRoad = -1;
        }
        else
        {
            playerWithLongestRoad = pl.getPlayerNumber();
        }
    }

    /**
     * Find the player who was declared winner at end of game.
     * This is determined in checkForWinner; there is no corresponding setter.
     *
     * @return the winning player, or null if none, or if game is not yet over.
     *
     * @see #checkForWinner()
     */
    public SOCPlayer getPlayerWithWin()
    {
        if (playerWithWin != -1)
        {
            return players[playerWithWin];
        }
        else
        {
            return null;
        }
    }

    /**
     * If game is over, formulate a message to tell a player.
     * @param pl Player to tell (may be the winner)
     * @return A message of one of these forms:
     *       "The game is over; you are the winner!"
     *       "The game is over; <someone> won."
     *       "The game is over; no one won."
     * @throws IllegalStateException If the game state is not OVER
     */
    public String gameOverMessageToPlayer(SOCPlayer pl)
        throws IllegalStateException
    {
        if (gameState != OVER)
            throw new IllegalStateException("This game is not over yet");
        String msg;
        SOCPlayer wn = getPlayerWithWin();

        if ((pl != null) && (pl == wn))
        {
            msg = "The game is over; you are the winner!";
        }
        else if (wn != null)
        {
            msg = "The game is over; " + wn.getName() + " won.";
        }
        else
        {
            // Just in case; don't think this can happen
            msg = "The game is over; no one won.";
        }

        return msg;
    }

    /**
     * advance the turn to the previous player,
     * used during initial placement
     */
    protected void advanceTurnBackwards()
    {
        //D.ebugPrintln("ADVANCE TURN BACKWARDS");
        currentPlayerNumber--;

        if (currentPlayerNumber < 0)
        {
            currentPlayerNumber = MAXPLAYERS - 1;
        }
        while (isSeatVacant (currentPlayerNumber))
        {
            --currentPlayerNumber;
            if (currentPlayerNumber < 0)
            {
                currentPlayerNumber = MAXPLAYERS - 1;
            }
        }
    }

    /**
     * advance the turn to the next player
     */
    protected void advanceTurn()
    {
        //D.ebugPrintln("ADVANCE TURN FORWARDS");
        currentPlayerNumber++;

        if (currentPlayerNumber == MAXPLAYERS)
        {
            currentPlayerNumber = 0;
        }
        while (isSeatVacant (currentPlayerNumber))
        {
            ++currentPlayerNumber;
            if (currentPlayerNumber == MAXPLAYERS)
            {
                currentPlayerNumber = 0;
            }
        }
    }

    /**
     * put this piece on the board and update game state
     *
     * @param pp the piece to put on the board
     */
    public void putPiece(SOCPlayingPiece pp)
    {
        /**
         * call putPiece() on every player so that each
         * player's updatePotentials() function gets called
         */
        for (int i = 0; i < MAXPLAYERS; i++)
        {
            players[i].putPiece(pp);
        }

        board.putPiece(pp);

        /**
         * if the piece is a city, remove the settlement there
         */
        if (pp.getType() == SOCPlayingPiece.CITY)
        {
            SOCSettlement se = new SOCSettlement(pp.getPlayer(), pp.getCoordinates());

            for (int i = 0; i < MAXPLAYERS; i++)
            {
                players[i].removePiece(se);
            }

            board.removePiece(se);
        }

        /**
         * if this the second initial settlement, give
         * the player some resources
         */
        if ((gameState == START2A) && (pp.getType() == SOCPlayingPiece.SETTLEMENT))
        {
            SOCResourceSet resources = new SOCResourceSet();
            Enumeration hexes = SOCBoard.getAdjacentHexesToNode(pp.getCoordinates()).elements();

            while (hexes.hasMoreElements())
            {
                Integer hex = (Integer) hexes.nextElement();

                switch (board.getHexTypeFromCoord(hex.intValue()))
                {
                case SOCBoard.CLAY_HEX:
                    resources.add(1, SOCResourceConstants.CLAY);

                    break;

                case SOCBoard.ORE_HEX:
                    resources.add(1, SOCResourceConstants.ORE);

                    break;

                case SOCBoard.SHEEP_HEX:
                    resources.add(1, SOCResourceConstants.SHEEP);

                    break;

                case SOCBoard.WHEAT_HEX:
                    resources.add(1, SOCResourceConstants.WHEAT);

                    break;

                case SOCBoard.WOOD_HEX:
                    resources.add(1, SOCResourceConstants.WOOD);

                    break;
                }
            }

            pp.getPlayer().getResources().add(resources);
        }

        if ((gameState == START2B) && (pp.getType() == SOCPlayingPiece.ROAD))
        {
            pp.getPlayer().clearPotentialSettlements();
        }

        /**
         * update which player has longest road
         */
        if (pp.getType() != SOCPlayingPiece.CITY)
        {
            if (pp.getType() == SOCPlayingPiece.ROAD)
            {
                /**
                 * the affected player is the one who build the road
                 */
                updateLongestRoad(pp.getPlayer().getPlayerNumber());
            }
            else
            {
                /**
                 * this is a settlement, check if it cut anyone elses road
                 */
                int[] roads = new int[MAXPLAYERS];

                for (int i = 0; i < MAXPLAYERS; i++)
                {
                    roads[i] = 0;
                }

                Enumeration adjEdgeEnum = SOCBoard.getAdjacentEdgesToNode(pp.getCoordinates()).elements();

                while (adjEdgeEnum.hasMoreElements())
                {
                    Integer adjEdge = (Integer) adjEdgeEnum.nextElement();

                    /**
                     * look for other player's roads adjacent to this node
                     */
                    Enumeration allRoadsEnum = board.getRoads().elements();

                    while (allRoadsEnum.hasMoreElements())
                    {
                        SOCRoad road = (SOCRoad) allRoadsEnum.nextElement();

                        if (adjEdge.intValue() == road.getCoordinates())
                        {
                            roads[road.getPlayer().getPlayerNumber()]++;
                        }
                    }
                }

                /**
                 * if a player other than the one who put the settlement
                 * down has 2 roads adjacent to it, then we need to recalculate
                 * their longest road
                 */
                for (int i = 0; i < MAXPLAYERS; i++)
                {
                    if ((i != pp.getPlayer().getPlayerNumber()) && (roads[i] == 2))
                    {
                        updateLongestRoad(i);

                        /**
                         * check to see if this created a tie
                         */
                        break;
                    }
                }
            }
        }

        /**
         * check if the game is over
         */
        checkForWinner();

        /**
         * update the state of the game
         */
        if (active)
        {
            //D.ebugPrintln("CHANGING GAME STATE FROM "+gameState);
            switch (gameState)
            {
            case START1A:
                gameState = START1B;

                break;

            case START1B:
            {
                int tmpCPN = currentPlayerNumber + 1;

                if (tmpCPN >= MAXPLAYERS)
                {
                    tmpCPN = 0;
                }
                while (isSeatVacant (tmpCPN))
                {
                    ++tmpCPN;
                    if (tmpCPN >= MAXPLAYERS)
                    {
                        tmpCPN = 0;
                    }
                }

                if (tmpCPN == firstPlayerNumber)
                {
                    gameState = START2A;
                }
                else
                {
                    advanceTurn();
                    gameState = START1A;
                }
            }

            break;

            case START2A:
                gameState = START2B;

                break;

            case START2B:
            {
                int tmpCPN = currentPlayerNumber - 1;

                if (tmpCPN < 0)
                {
                    tmpCPN = MAXPLAYERS - 1;
                }
                while (isSeatVacant (tmpCPN))
                {
                    --tmpCPN;
                    if (tmpCPN < 0)
                    {
                        tmpCPN = MAXPLAYERS - 1;
                    }
                }

                if (tmpCPN == lastPlayerNumber)
                {
                    // player number is unchanged.
                    gameState = PLAY;
                }
                else
                {
                    advanceTurnBackwards();
                    gameState = START2A;
                }
            }

            break;

            case PLACING_ROAD:
            case PLACING_SETTLEMENT:
            case PLACING_CITY:
                gameState = PLAY1;

                break;

            case PLACING_FREE_ROAD1:
                gameState = PLACING_FREE_ROAD2;

                break;

            case PLACING_FREE_ROAD2:
                gameState = oldGameState;

                break;
            }

            //D.ebugPrintln("  TO "+gameState);
        }
    }

    /**
     * a temporary piece has been put on the board
     *
     * @param pp the piece to put on the board
     */
    public void putTempPiece(SOCPlayingPiece pp)
    {
        //D.ebugPrintln("@@@ putTempPiece "+pp);

        /**
         * save who the last lr player was
         */
        oldPlayerWithLongestRoad.push(new SOCOldLRStats(this));

        /**
         * call putPiece() on every player so that each
         * player's updatePotentials() function gets called
         */
        for (int i = 0; i < MAXPLAYERS; i++)
        {
            players[i].putPiece(pp);
        }

        board.putPiece(pp);

        /**
         * if the piece is a city, remove the settlement there
         */
        if (pp.getType() == SOCPlayingPiece.CITY)
        {
            SOCSettlement se = new SOCSettlement(pp.getPlayer(), pp.getCoordinates());

            for (int i = 0; i < MAXPLAYERS; i++)
            {
                players[i].removePiece(se);
            }

            board.removePiece(se);
        }

        /**
         * update which player has longest road
         */
        if (pp.getType() != SOCPlayingPiece.CITY)
        {
            if (pp.getType() == SOCPlayingPiece.ROAD)
            {
                /**
                 * the affected player is the one who build the road
                 */
                updateLongestRoad(pp.getPlayer().getPlayerNumber());
            }
            else
            {
                /**
                 * this is a settlement, check if it cut anyone elses road
                 */
                int[] roads = new int[MAXPLAYERS];

                for (int i = 0; i < MAXPLAYERS; i++)
                {
                    roads[i] = 0;
                }

                Enumeration adjEdgeEnum = SOCBoard.getAdjacentEdgesToNode(pp.getCoordinates()).elements();

                while (adjEdgeEnum.hasMoreElements())
                {
                    Integer adjEdge = (Integer) adjEdgeEnum.nextElement();

                    /**
                     * look for other player's roads adjacent to this node
                     */
                    Enumeration allRoadsEnum = board.getRoads().elements();

                    while (allRoadsEnum.hasMoreElements())
                    {
                        SOCRoad road = (SOCRoad) allRoadsEnum.nextElement();

                        if (adjEdge.intValue() == road.getCoordinates())
                        {
                            roads[road.getPlayer().getPlayerNumber()]++;
                        }
                    }
                }

                /**
                 * if a player other than the one who put the settlement
                 * down has 2 roads adjacent to it, then we need to recalculate
                 * their longest road
                 */
                for (int i = 0; i < MAXPLAYERS; i++)
                {
                    if ((i != pp.getPlayer().getPlayerNumber()) && (roads[i] == 2))
                    {
                        updateLongestRoad(i);

                        /**
                         * check to see if this created a tie
                         */
                        break;
                    }
                }
            }
        }
    }

    /**
     * undo the putting of a temporary or initial piece
     *
     * @param pp the piece to put on the board
     */
    protected void undoPutPieceCommon(SOCPlayingPiece pp)
    {
        //D.ebugPrintln("@@@ undoPutTempPiece "+pp);
        board.removePiece(pp);

        //
        // call undoPutPiece() on every player so that 
        // they can update their potentials
        //
        for (int i = 0; i < MAXPLAYERS; i++)
        {
            players[i].undoPutPiece(pp);   // If state START2B, will also zero resources
        }

        //
        // if the piece is a city, put the settlement back
        //
        if (pp.getType() == SOCPlayingPiece.CITY)
        {
            SOCSettlement se = new SOCSettlement(pp.getPlayer(), pp.getCoordinates());

            for (int i = 0; i < MAXPLAYERS; i++)
            {
                players[i].putPiece(se);
            }

            board.putPiece(se);
        }
    }

    /**
     * undo the putting of a temporary piece
     *
     * @param pp the piece to put on the board
     *
     * @see #undoPutInitSettlement(SOCPlayingPiece)
     */
    public void undoPutTempPiece(SOCPlayingPiece pp)
    {
        undoPutPieceCommon(pp);

        //
        // update which player has longest road
        //
        SOCOldLRStats oldLRStats = (SOCOldLRStats) oldPlayerWithLongestRoad.pop();
        oldLRStats.restoreOldStats(this);
    }

    /**
     * undo the putting of an initial settlement.
     * If state is STATE2B, resources will be returned.
     * Player is unchanged; state will become STATE1A or STATE2A.
     *
     * @param pp the piece to remove from the board
     */
    public void undoPutInitSettlement(SOCPlayingPiece pp)
    {
        if ((gameState != START1B) && (gameState != START2B))
            throw new IllegalStateException("Cannot remove at this game state: " + gameState);
        if (pp.getType() != SOCPlayingPiece.SETTLEMENT)
            throw new IllegalArgumentException("Not a settlement: type " + pp.getType());
        if (pp.getCoordinates() != pp.getPlayer().getLastSettlementCoord())
            throw new IllegalArgumentException("Not coordinate of last settlement");

        undoPutPieceCommon(pp);  // Will also zero resources via player.undoPutPiece

        if (gameState == START1B)
            gameState = START1A;
        else // gameState == START2B
            gameState = START2A;
    }

    /**
     * do the things involved in starting a game
     * shuffle the tiles and cards
     * make a board
     */
    public void startGame()
    {
        board.makeNewBoard();

        /**
         * shuffle the development cards
         */
        devCardDeck = new int[25];

        int i;
        int j;

        for (i = 0; i < 14; i++)
        {
            devCardDeck[i] = SOCDevCardConstants.KNIGHT;
        }

        for (i = 14; i < 16; i++)
        {
            devCardDeck[i] = SOCDevCardConstants.ROADS;
        }

        for (i = 16; i < 18; i++)
        {
            devCardDeck[i] = SOCDevCardConstants.MONO;
        }

        for (i = 18; i < 20; i++)
        {
            devCardDeck[i] = SOCDevCardConstants.DISC;
        }

        devCardDeck[20] = SOCDevCardConstants.CAP;
        devCardDeck[21] = SOCDevCardConstants.LIB;
        devCardDeck[22] = SOCDevCardConstants.UNIV;
        devCardDeck[23] = SOCDevCardConstants.TEMP;
        devCardDeck[24] = SOCDevCardConstants.TOW;

        for (j = 0; j < 10; j++)
        {
            for (i = 1; i < devCardDeck.length; i++) // don't swap 0 with 0!
            {
                // Swap a random card below the ith card with the ith card
                int idx = Math.abs(rand.nextInt() % (devCardDeck.length - 1));
                int tmp = devCardDeck[idx];
                devCardDeck[idx] = devCardDeck[i];
                devCardDeck[i] = tmp;
            }
        }

        allOriginalPlayers = true;
        gameState = START1A;

        /**
         * choose to goes first
         */
        do
        {
            currentPlayerNumber = Math.abs(rand.nextInt() % MAXPLAYERS);
        } while (isSeatVacant(currentPlayerNumber));
        setFirstPlayer(currentPlayerNumber);
    }

    /**
     * sets who the first player is
     *
     * @param pn  the seat number of the first player
     */
    public void setFirstPlayer(int pn)
    {
        firstPlayerNumber = pn;
        lastPlayerNumber = pn - 1;

        if (lastPlayerNumber < 0)
        {
            lastPlayerNumber = MAXPLAYERS - 1;
        }
        while (isSeatVacant (lastPlayerNumber))
        {
            --lastPlayerNumber;
            if (lastPlayerNumber < 0)
            {
                lastPlayerNumber = MAXPLAYERS - 1;
            }
            if (lastPlayerNumber == firstPlayerNumber)
            {
                // Should not happen: All seats blank                    
                D.ebugPrintln("** setFirstPlayer: Should not happen: All seats blank");
                lastPlayerNumber = -1;
                break;
            }
        }
    }

    /**
     * @return the seat number of the first player
     */
    public int getFirstPlayer()
    {
        return firstPlayerNumber;
    }

    /**
     * @return true if its ok for this player end the turn
     *
     * @param pn  player number of the player who wants to end the turn
     */
    public boolean canEndTurn(int pn)
    {
        if (currentPlayerNumber != pn)
        {
            return false;
        }
        else if (gameState != PLAY1)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * end the turn for the current player
     */
    public void endTurn()
    {
        gameState = PLAY;
        currentDice = 0;
        advanceTurn();
        players[currentPlayerNumber].setPlayedDevCard(false);
        players[currentPlayerNumber].getDevCards().newToOld();
        if (boardResetVoteRequester != -1)
            boardResetVoteRequester = -1;
        for (int i = 0; i < MAXPLAYERS; ++i)
            players[i].setAskedBoardReset(false);
    }

    /**
     * @return true if it's ok for this player to roll the dice
     *
     * @param pn  player number of the player who wants to roll
     */
    public boolean canRollDice(int pn)
    {
        if (currentPlayerNumber != pn)
        {
            return false;
        }
        else if (gameState != PLAY)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * roll the dice
     */
    public IntPair rollDice()
    {
        int die1 = Math.abs(rand.nextInt() % 6) + 1;
        int die2 = Math.abs(rand.nextInt() % 6) + 1;

        currentDice = die1 + die2;

        /**
         * handle the seven case
         */
        if (currentDice == 7)
        {
            /**
             * if there are players with too many cards, wait for
             * them to discard
             */
            for (int i = 0; i < MAXPLAYERS; i++)
            {
                if (players[i].getResources().getTotal() > 7)
                {
                    players[i].setNeedToDiscard(true);
                    gameState = WAITING_FOR_DISCARDS;
                }
            }

            /**
             * if no one needs to discard, then wait for
             * the robber to move
             */
            if (gameState != WAITING_FOR_DISCARDS)
            {
                oldGameState = PLAY1;
                gameState = PLACING_ROBBER;
            }
        }
        else
        {
            /**
             * distribute resources
             */
            for (int i = 0; i < MAXPLAYERS; i++)
            {
                if (! isSeatVacant(i))
                {
                    SOCResourceSet newResources = getResourcesGainedFromRoll(players[i], currentDice);
                    players[i].getResources().add(newResources);
                }
            }

            gameState = PLAY1;
        }

        return new IntPair(die1, die2);
    }

    /**
     * figure out what resources a player would get on a given roll
     *
     * @param player   the player
     * @param roll     the roll
     *
     * @return the resource set
     */
    public SOCResourceSet getResourcesGainedFromRoll(SOCPlayer player, int roll)
    {
        SOCResourceSet resources = new SOCResourceSet();

        /**
         * check the hexes touching settlements
         */
        Enumeration sEnum = player.getSettlements().elements();

        while (sEnum.hasMoreElements())
        {
            SOCSettlement se = (SOCSettlement) sEnum.nextElement();
            Enumeration hexes = SOCBoard.getAdjacentHexesToNode(se.getCoordinates()).elements();

            while (hexes.hasMoreElements())
            {
                Integer hex = (Integer) hexes.nextElement();

                if ((board.getNumberOnHexFromCoord(hex.intValue()) == roll) && (hex.intValue() != board.getRobberHex()))
                {
                    switch (board.getHexTypeFromCoord(hex.intValue()))
                    {
                    case SOCBoard.CLAY_HEX:
                        resources.add(1, SOCResourceConstants.CLAY);

                        break;

                    case SOCBoard.ORE_HEX:
                        resources.add(1, SOCResourceConstants.ORE);

                        break;

                    case SOCBoard.SHEEP_HEX:
                        resources.add(1, SOCResourceConstants.SHEEP);

                        break;

                    case SOCBoard.WHEAT_HEX:
                        resources.add(1, SOCResourceConstants.WHEAT);

                        break;

                    case SOCBoard.WOOD_HEX:
                        resources.add(1, SOCResourceConstants.WOOD);

                        break;
                    }
                }
            }
        }

        /**
         * check the settlements touching cities
         */
        Enumeration cEnum = player.getCities().elements();

        while (cEnum.hasMoreElements())
        {
            SOCCity ci = (SOCCity) cEnum.nextElement();
            Enumeration hexes = SOCBoard.getAdjacentHexesToNode(ci.getCoordinates()).elements();

            while (hexes.hasMoreElements())
            {
                Integer hex = (Integer) hexes.nextElement();

                if ((board.getNumberOnHexFromCoord(hex.intValue()) == roll) && (hex.intValue() != board.getRobberHex()))
                {
                    switch (board.getHexTypeFromCoord(hex.intValue()))
                    {
                    case SOCBoard.CLAY_HEX:
                        resources.add(2, SOCResourceConstants.CLAY);

                        break;

                    case SOCBoard.ORE_HEX:
                        resources.add(2, SOCResourceConstants.ORE);

                        break;

                    case SOCBoard.SHEEP_HEX:
                        resources.add(2, SOCResourceConstants.SHEEP);

                        break;

                    case SOCBoard.WHEAT_HEX:
                        resources.add(2, SOCResourceConstants.WHEAT);

                        break;

                    case SOCBoard.WOOD_HEX:
                        resources.add(2, SOCResourceConstants.WOOD);

                        break;
                    }
                }
            }
        }

        return resources;
    }

    /**
     * @return true if the player can discard these resources
     *
     * @param pn  the number of the player that is discarding
     * @param rs  the resources that the player is discarding
     */
    public boolean canDiscard(int pn, SOCResourceSet rs)
    {
        if (gameState != WAITING_FOR_DISCARDS)
        {
            return false;
        }

        SOCResourceSet resources = players[pn].getResources();

        if (!players[pn].getNeedToDiscard())
        {
            return false;
        }

        if (rs.getTotal() != (resources.getTotal() / 2))
        {
            return false;
        }

        if (!resources.contains(rs))
        {
            return false;
        }

        return true;
    }

    /**
     * A player is discarding resources
     *
     * @param pn   the number of the player
     * @param rs   the resources that are being discarded
     */
    public void discard(int pn, SOCResourceSet rs)
    {
        players[pn].getResources().subtract(rs);
        players[pn].setNeedToDiscard(false);

        /**
         * check if we're still waiting for players to discard
         */
        gameState = PLACING_ROBBER;

        for (int i = 0; i < MAXPLAYERS; i++)
        {
            if (players[i].getNeedToDiscard())
            {
                gameState = WAITING_FOR_DISCARDS;

                break;
            }
        }

        /**
         * if no one needs to discard, then wait for
         * the robber to move
         */
        if (gameState != WAITING_FOR_DISCARDS)
        {
            oldGameState = PLAY1;
            gameState = PLACING_ROBBER;
        }
    }

    /**
     * @return true if the player can move the robber to the coordinates
     *
     * @param pn  the number of the player that is moving the robber
     * @param co  the coordinates
     */
    public boolean canMoveRobber(int pn, int co)
    {
        if (gameState != PLACING_ROBBER)
        {
            return false;
        }

        if (currentPlayerNumber != pn)
        {
            return false;
        }

        if (board.getRobberHex() == co)
        {
            return false;
        }

        int hexType = board.getHexTypeFromCoord(co);

        if ((hexType != SOCBoard.CLAY_HEX) && (hexType != SOCBoard.ORE_HEX) && (hexType != SOCBoard.SHEEP_HEX) && (hexType != SOCBoard.WHEAT_HEX) && (hexType != SOCBoard.WOOD_HEX) && (hexType != SOCBoard.DESERT_HEX))
        {
            return false;
        }

        return true;
    }

    /**
     * move the robber.
     *
     * If no victims (players to possibly steal from): State becomes oldGameState.
     * If just one victim: call stealFromPlayer, State becomes oldGameState.
     * If multiple possible victims: Player must choose a victim; State becomes WAITING_FOR_CHOICE.
     *
     * @param pn  the number of the player that is moving the robber
     * @param co  the coordinates
     *
     * @return returns a result that says if a resource was stolen, or
     *         if the player needs to make a choice.  It also returns
     *         what was stolen and who was the victim.
     */
    public SOCMoveRobberResult moveRobber(int pn, int co)
    {
        SOCMoveRobberResult result = new SOCMoveRobberResult();

        board.setRobberHex(co);

        /**
         * do the robbing thing
         */
        Vector victims = getPossibleVictims();

        if (victims.isEmpty())
        {
            gameState = oldGameState;
        }
        else if (victims.size() == 1)
        {
            SOCPlayer victim = (SOCPlayer) victims.firstElement();
            int loot = stealFromPlayer(victim.getPlayerNumber());
            result.setLoot(loot);
        }
        else
        {
            /**
             * the current player needs to make a choice
             */
            gameState = WAITING_FOR_CHOICE;
        }

        result.setVictims(victims);

        return result;
    }

    /**
     * @return true if the current player can choose a player to rob
     *
     * @param pn  the number of the player to rob
     */
    public boolean canChoosePlayer(int pn)
    {
        if (gameState != WAITING_FOR_CHOICE)
        {
            return false;
        }

        Enumeration plEnum = getPossibleVictims().elements();

        while (plEnum.hasMoreElements())
        {
            SOCPlayer pl = (SOCPlayer) plEnum.nextElement();

            if (pl.getPlayerNumber() == pn)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * @return a list of players touching a hex
     *
     * @param hex  the coordinates of the hex
     */
    public Vector getPlayersOnHex(int hex)
    {
        Vector playerList = new Vector(MAXPLAYERS);

        for (int i = 0; i < MAXPLAYERS; i++)
        {
            Vector settlements = players[i].getSettlements();
            Vector cities = players[i].getCities();
            Enumeration seEnum;
            Enumeration ciEnum;
            int node;
            boolean touching = false;

            node = hex + 0x01;
            seEnum = settlements.elements();

            while (seEnum.hasMoreElements())
            {
                SOCSettlement se = (SOCSettlement) seEnum.nextElement();

                if (se.getCoordinates() == node)
                {
                    touching = true;

                    break;
                }
            }

            if (!touching)
            {
                ciEnum = cities.elements();

                while (ciEnum.hasMoreElements())
                {
                    SOCCity ci = (SOCCity) ciEnum.nextElement();

                    if (ci.getCoordinates() == node)
                    {
                        touching = true;

                        break;
                    }
                }

                if (!touching)
                {
                    node = hex + 0x12;
                    seEnum = settlements.elements();

                    while (seEnum.hasMoreElements())
                    {
                        SOCSettlement se = (SOCSettlement) seEnum.nextElement();

                        if (se.getCoordinates() == node)
                        {
                            touching = true;

                            break;
                        }
                    }

                    if (!touching)
                    {
                        ciEnum = cities.elements();

                        while (ciEnum.hasMoreElements())
                        {
                            SOCCity ci = (SOCCity) ciEnum.nextElement();

                            if (ci.getCoordinates() == node)
                            {
                                touching = true;

                                break;
                            }
                        }

                        if (!touching)
                        {
                            node = hex + 0x21;
                            seEnum = settlements.elements();

                            while (seEnum.hasMoreElements())
                            {
                                SOCSettlement se = (SOCSettlement) seEnum.nextElement();

                                if (se.getCoordinates() == node)
                                {
                                    touching = true;

                                    break;
                                }
                            }

                            if (!touching)
                            {
                                ciEnum = cities.elements();

                                while (ciEnum.hasMoreElements())
                                {
                                    SOCCity ci = (SOCCity) ciEnum.nextElement();

                                    if (ci.getCoordinates() == node)
                                    {
                                        touching = true;

                                        break;
                                    }
                                }

                                node = hex + 0x10;
                                seEnum = settlements.elements();

                                while (seEnum.hasMoreElements())
                                {
                                    SOCSettlement se = (SOCSettlement) seEnum.nextElement();

                                    if (se.getCoordinates() == node)
                                    {
                                        touching = true;

                                        break;
                                    }
                                }

                                if (!touching)
                                {
                                    ciEnum = cities.elements();

                                    while (ciEnum.hasMoreElements())
                                    {
                                        SOCCity ci = (SOCCity) ciEnum.nextElement();

                                        if (ci.getCoordinates() == node)
                                        {
                                            touching = true;

                                            break;
                                        }
                                    }

                                    node = hex - 0x01;
                                    seEnum = settlements.elements();

                                    while (seEnum.hasMoreElements())
                                    {
                                        SOCSettlement se = (SOCSettlement) seEnum.nextElement();

                                        if (se.getCoordinates() == node)
                                        {
                                            touching = true;

                                            break;
                                        }
                                    }

                                    if (!touching)
                                    {
                                        ciEnum = cities.elements();

                                        while (ciEnum.hasMoreElements())
                                        {
                                            SOCCity ci = (SOCCity) ciEnum.nextElement();

                                            if (ci.getCoordinates() == node)
                                            {
                                                touching = true;

                                                break;
                                            }
                                        }

                                        node = hex - 0x10;
                                        seEnum = settlements.elements();

                                        while (seEnum.hasMoreElements())
                                        {
                                            SOCSettlement se = (SOCSettlement) seEnum.nextElement();

                                            if (se.getCoordinates() == node)
                                            {
                                                touching = true;

                                                break;
                                            }
                                        }

                                        if (!touching)
                                        {
                                            ciEnum = cities.elements();

                                            while (ciEnum.hasMoreElements())
                                            {
                                                SOCCity ci = (SOCCity) ciEnum.nextElement();

                                                if (ci.getCoordinates() == node)
                                                {
                                                    touching = true;

                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (touching)
            {
                playerList.addElement(players[i]);
            }
        }

        return playerList;
    }

    /**
     * @return a list of possible players to rob
     */
    public Vector getPossibleVictims()
    {
        Vector victims = new Vector();
        Enumeration plEnum = getPlayersOnHex(getBoard().getRobberHex()).elements();

        while (plEnum.hasMoreElements())
        {
            SOCPlayer pl = (SOCPlayer) plEnum.nextElement();

            if ((pl.getPlayerNumber() != currentPlayerNumber) && (pl.getResources().getTotal() > 0))
            {
                victims.addElement(pl);
            }
        }

        return victims;
    }

    /**
     * the current player has choosen a victim to rob.
     * perform the robbery.  Set gameState back to oldGameState.
     *
     * @param pn  the number of the player being robbed
     * @return the type of resource that was stolen
     */
    public int stealFromPlayer(int pn)
    {
        /**
         * pick a resource card at random
         */
        SOCPlayer victim = players[pn];
        int[] rsrcs = new int[victim.getResources().getTotal()];
        int cnt = 0;

        for (int i = SOCResourceConstants.CLAY; i <= SOCResourceConstants.WOOD;
                i++)
        {
            for (int j = 0; j < victim.getResources().getAmount(i); j++)
            {
                rsrcs[cnt] = i;
                cnt++;
            }
        }

        int pick = Math.abs(rand.nextInt() % cnt);

        /**
         * and transfer it to the current player
         */
        victim.getResources().subtract(1, rsrcs[pick]);
        players[currentPlayerNumber].getResources().add(1, rsrcs[pick]);

        /**
         * restore the game state to what it was before the robber moved
         */
        gameState = oldGameState;

        return rsrcs[pick];
    }

    /**
     * @return true if the two players can make the trade
     *         described in the offering players current offer
     *
     * @param offering  the number of the player making the offer
     * @param accepting the number of the player accepting the offer
     */
    public boolean canMakeTrade(int offering, int accepting)
    {
        D.ebugPrintln("*** canMakeTrade ***");
        D.ebugPrintln("*** offering = " + offering);
        D.ebugPrintln("*** accepting = " + accepting);

        if (gameState != PLAY1)
        {
            return false;
        }

        if (players[offering].getCurrentOffer() == null)
        {
            return false;
        }

        if ((currentPlayerNumber != offering) && (currentPlayerNumber != accepting))
        {
            return false;
        }

        SOCPlayer offeringPlayer = players[offering];
        SOCPlayer acceptingPlayer = players[accepting];
        SOCTradeOffer offer = offeringPlayer.getCurrentOffer();

        D.ebugPrintln("*** offer = " + offer);

        if ((offer.getGiveSet().getTotal() == 0) || (offer.getGetSet().getTotal() == 0))
        {
            return false;
        }

        D.ebugPrintln("*** offeringPlayer.getResources() = " + offeringPlayer.getResources());

        if (!(offeringPlayer.getResources().contains(offer.getGiveSet())))
        {
            return false;
        }

        D.ebugPrintln("*** acceptingPlayer.getResources() = " + acceptingPlayer.getResources());

        if (!(acceptingPlayer.getResources().contains(offer.getGetSet())))
        {
            return false;
        }

        return true;
    }

    /**
     * perform a trade between two players
     * the trade performed is described in the offering player's
     * current offer
     *
     * @param offering  the number of the player making the offer
     * @param accepting the number of the player accepting the offer
     */
    public void makeTrade(int offering, int accepting)
    {
        SOCResourceSet offeringPlayerResources = players[offering].getResources();
        SOCResourceSet acceptingPlayerResources = players[accepting].getResources();
        SOCTradeOffer offer = players[offering].getCurrentOffer();

        offeringPlayerResources.subtract(offer.getGiveSet());
        acceptingPlayerResources.subtract(offer.getGetSet());
        offeringPlayerResources.add(offer.getGetSet());
        acceptingPlayerResources.add(offer.getGiveSet());
    }

    /**
     * @return true if the current player can make a
     *         particular bank/port trade
     *
     * @param  give  what the player will give to the bank
     * @param  get   what the player wants from the bank
     */
    public boolean canMakeBankTrade(SOCResourceSet give, SOCResourceSet get)
    {
        if (gameState != PLAY1)
        {
            return false;
        }

        if ((give.getTotal() < 2) || (get.getTotal() == 0))
        {
            return false;
        }

        if (!(players[currentPlayerNumber].getResources().contains(give)))
        {
            return false;
        }

        int groupCount = 0;
        int ratio = give.getTotal() / get.getTotal();

        switch (ratio)
        {
        /**
         * bank trade
         */
        case 4:

            /**
             * check for groups of 4
             */
            for (int i = SOCResourceConstants.CLAY;
                    i <= SOCResourceConstants.WOOD; i++)
            {
                if ((give.getAmount(i) % 4) == 0)
                {
                    groupCount += (give.getAmount(i) / 4);
                }
                else
                {
                    return false;
                }
            }

            break;

        /**
         * 3:1 port trade
         */
        case 3:

            /**
             * check for groups of 3
             */
            for (int i = SOCResourceConstants.CLAY;
                    i <= SOCResourceConstants.WOOD; i++)
            {
                if ((give.getAmount(i) % 3) == 0)
                {
                    groupCount += (give.getAmount(i) / 3);

                    /**
                     * check if this player has a 3:1 port
                     */
                    if (!(players[currentPlayerNumber].getPortFlag(SOCBoard.MISC_PORT)))
                    {
                        return false;
                    }
                }
                else
                {
                    return false;
                }
            }

            break;

        /**
         * 2:1 port trade
         */
        case 2:

            /**
             * check for groups of 2
             */
            /**
             * Note: this only works if SOCResourceConstants.CLAY == 1
             */
            for (int i = SOCResourceConstants.CLAY;
                    i <= SOCResourceConstants.WOOD; i++)
            {
                if (give.getAmount(i) > 0)
                {
                    if (((give.getAmount(i) % 2) == 0) && (players[currentPlayerNumber].getPortFlag(i)))
                    {
                        groupCount += (give.getAmount(i) / 2);
                    }
                    else
                    {
                        return false;
                    }
                }
            }

            break;
        }

        if (groupCount != get.getTotal())
        {
            return false;
        }

        return true;
    }

    /**
     * perform a bank trade
     *
     * @param give  the number of the player making the offer
     * @param get the number of the player accepting the offer
     */
    public void makeBankTrade(SOCResourceSet give, SOCResourceSet get)
    {
        SOCResourceSet playerResources = players[currentPlayerNumber].getResources();

        playerResources.subtract(give);
        playerResources.add(get);
    }

    /**
     * @return true if the player has the resources, pieces, and
     *         room to build a road
     *
     * @param pn  the number of the player
     */
    public boolean couldBuildRoad(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();

        return ((resources.getAmount(SOCResourceConstants.CLAY) >= 1) && (resources.getAmount(SOCResourceConstants.WOOD) >= 1) && (players[pn].getNumPieces(SOCPlayingPiece.ROAD) >= 1) && (players[pn].hasPotentialRoad()));
    }

    /**
     * @return true if the player has the resources, pieces, and
     *         room to build a settlement
     *
     * @param pn  the number of the player
     */
    public boolean couldBuildSettlement(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();

        return ((resources.getAmount(SOCResourceConstants.CLAY) >= 1) && (resources.getAmount(SOCResourceConstants.SHEEP) >= 1) && (resources.getAmount(SOCResourceConstants.WHEAT) >= 1) && (resources.getAmount(SOCResourceConstants.WOOD) >= 1) && (players[pn].getNumPieces(SOCPlayingPiece.SETTLEMENT) >= 1) && (players[pn].hasPotentialSettlement()));
    }

    /**
     * @return true if the player has the resources, pieces, and
     *         room to build a city
     *
     * @param pn  the number of the player
     */
    public boolean couldBuildCity(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();

        return ((resources.getAmount(SOCResourceConstants.ORE) >= 3) && (resources.getAmount(SOCResourceConstants.WHEAT) >= 2) && (players[pn].getNumPieces(SOCPlayingPiece.CITY) >= 1) && (players[pn].hasPotentialCity()));
    }

    /**
     * @return true if the player has the resources
     *         to buy a dev card, and if there are dev cards
     *         left to buy
     *
     * @param pn  the number of the player
     */
    public boolean couldBuyDevCard(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();

        return ((resources.getAmount(SOCResourceConstants.SHEEP) >= 1) && (resources.getAmount(SOCResourceConstants.ORE) >= 1) && (resources.getAmount(SOCResourceConstants.WHEAT) >= 1) && (numDevCards > 0));
    }

    /**
     * a player is buying a road
     *
     * @param pn  the number of the player
     */
    public void buyRoad(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();
        resources.subtract(1, SOCResourceConstants.CLAY);
        resources.subtract(1, SOCResourceConstants.WOOD);
        gameState = PLACING_ROAD;
    }

    /**
     * a player is buying a settlement
     *
     * @param pn  the number of the player
     */
    public void buySettlement(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();
        resources.subtract(1, SOCResourceConstants.CLAY);
        resources.subtract(1, SOCResourceConstants.SHEEP);
        resources.subtract(1, SOCResourceConstants.WHEAT);
        resources.subtract(1, SOCResourceConstants.WOOD);
        gameState = PLACING_SETTLEMENT;
    }

    /**
     * a player is buying a city
     *
     * @param pn  the number of the player
     */
    public void buyCity(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();
        resources.subtract(3, SOCResourceConstants.ORE);
        resources.subtract(2, SOCResourceConstants.WHEAT);
        gameState = PLACING_CITY;
    }

    /**
     * a player is UNbuying a road
     *
     * @param pn  the number of the player
     */
    public void cancelBuildRoad(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();
        resources.add(1, SOCResourceConstants.CLAY);
        resources.add(1, SOCResourceConstants.WOOD);
        gameState = PLAY1;
    }

    /**
     * a player is UNbuying a settlement
     *
     * @param pn  the number of the player
     */
    public void cancelBuildSettlement(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();
        resources.add(1, SOCResourceConstants.CLAY);
        resources.add(1, SOCResourceConstants.SHEEP);
        resources.add(1, SOCResourceConstants.WHEAT);
        resources.add(1, SOCResourceConstants.WOOD);
        gameState = PLAY1;
    }

    /**
     * a player is UNbuying a city
     *
     * @param pn  the number of the player
     */
    public void cancelBuildCity(int pn)
    {
        SOCResourceSet resources = players[pn].getResources();
        resources.add(3, SOCResourceConstants.ORE);
        resources.add(2, SOCResourceConstants.WHEAT);
        gameState = PLAY1;
    }

    /**
     * the current player is buying a dev card
     *
     * @return the card that was drawn
     */
    public int buyDevCard()
    {
        int card = devCardDeck[numDevCards - 1];
        numDevCards--;

        SOCResourceSet resources = players[currentPlayerNumber].getResources();
        resources.subtract(1, SOCResourceConstants.ORE);
        resources.subtract(1, SOCResourceConstants.SHEEP);
        resources.subtract(1, SOCResourceConstants.WHEAT);
        players[currentPlayerNumber].getDevCards().add(1, SOCDevCardSet.NEW, card);
        checkForWinner();

        return (card);
    }

    /**
     * @return true if the player can play a knight card
     *
     * @param pn  the number of the player
     */
    public boolean canPlayKnight(int pn)
    {
        if (!((gameState == PLAY) || (gameState == PLAY1)))
        {
            return false;
        }

        if (players[pn].hasPlayedDevCard())
        {
            return false;
        }

        if (players[pn].getDevCards().getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.KNIGHT) == 0)
        {
            return false;
        }

        return true;
    }

    /**
     * Can the current player play a Road Building card?
     * This card directs the player to place 2 roads.
     * Checks of game rules online show they "MAY" or "CAN", not "MUST", place 2.
     * If they have 2 or more roads, place 2.
     * If they have just 1 road, place 1.
     * If they have 0 roads, cannot play the card.
     *
     * @return true if the player can play a Road Building card.
     *
     * @param pn  the number of the player
     */
    public boolean canPlayRoadBuilding(int pn)
    {
        if (!((gameState == PLAY) || (gameState == PLAY1)))
        {
            return false;
        }

        if (players[pn].hasPlayedDevCard())
        {
            return false;
        }

        if (players[pn].getDevCards().getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.ROADS) == 0)
        {
            return false;
        }

        if (players[pn].getNumPieces(SOCPlayingPiece.ROAD) < 1)
        {
            return false;
        }

        return true;
    }

    /**
     * return true if the player can play a Discovery card
     *
     * @param pn  the number of the player
     */
    public boolean canPlayDiscovery(int pn)
    {
        if (!((gameState == PLAY) || (gameState == PLAY1)))
        {
            return false;
        }

        if (players[pn].hasPlayedDevCard())
        {
            return false;
        }

        if (players[pn].getDevCards().getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.DISC) == 0)
        {
            return false;
        }

        return true;
    }

    /**
     * return true if the player can play a Monopoly card
     *
     * @param pn  the number of the player
     */
    public boolean canPlayMonopoly(int pn)
    {
        if (!((gameState == PLAY) || (gameState == PLAY1)))
        {
            return false;
        }

        if (players[pn].hasPlayedDevCard())
        {
            return false;
        }

        if (players[pn].getDevCards().getAmount(SOCDevCardSet.OLD, SOCDevCardConstants.MONO) == 0)
        {
            return false;
        }

        return true;
    }

    /**
     * the current player plays a Knight card
     */
    public void playKnight()
    {
        players[currentPlayerNumber].setPlayedDevCard(true);
        players[currentPlayerNumber].getDevCards().subtract(1, SOCDevCardSet.OLD, SOCDevCardConstants.KNIGHT);
        players[currentPlayerNumber].incrementNumKnights();
        updateLargestArmy();
        checkForWinner();
        oldGameState = gameState;
        gameState = PLACING_ROBBER;
    }

    /**
     * the current player plays a Road Building card.
     * This card directs the player to place 2 roads.
     * Checks of game rules online show "MAY" or "CAN", not "MUST" place 2.
     * If they have 2 or more roads, may place 2; gameState becomes PLACING_FREE_ROAD1.
     * If they have just 1 road, may place that; gameState becomes PLACING_FREE_ROAD2.
     * If they have 0 roads, cannot play the card.
     * Assumes {@link #canPlayRoadBuilding(int)} has already been called, and move is valid.
     */
    public void playRoadBuilding()
    {
        players[currentPlayerNumber].setPlayedDevCard(true);
        players[currentPlayerNumber].getDevCards().subtract(1, SOCDevCardSet.OLD, SOCDevCardConstants.ROADS);
        oldGameState = gameState;
        if (players[currentPlayerNumber].getNumPieces(SOCPlayingPiece.ROAD) > 1)
        {
            gameState = PLACING_FREE_ROAD1;  // First of 2 free roads
        } else {
            gameState = PLACING_FREE_ROAD2;  // "Second", just 1 free road
        }
    }

    /**
     * the current player plays a Discovery card
     */
    public void playDiscovery()
    {
        players[currentPlayerNumber].setPlayedDevCard(true);
        players[currentPlayerNumber].getDevCards().subtract(1, SOCDevCardSet.OLD, SOCDevCardConstants.DISC);
        oldGameState = gameState;
        gameState = WAITING_FOR_DISCOVERY;
    }

    /**
     * the current player plays a monopoly card
     */
    public void playMonopoly()
    {
        players[currentPlayerNumber].setPlayedDevCard(true);
        players[currentPlayerNumber].getDevCards().subtract(1, SOCDevCardSet.OLD, SOCDevCardConstants.MONO);
        oldGameState = gameState;
        gameState = WAITING_FOR_MONOPOLY;
    }

    /**
     * @return true if the current player can
     *         do the discovery card action and the
     *         pick contains exactly 2 resources
     *
     * @param pick  the resources that the player wants
     */
    public boolean canDoDiscoveryAction(SOCResourceSet pick)
    {
        if (gameState != WAITING_FOR_DISCOVERY)
        {
            return false;
        }

        if (pick.getTotal() != 2)
        {
            return false;
        }

        return true;
    }

    /**
     * @return true if the current player can do
     *         the Monopoly card action
     */
    public boolean canDoMonopolyAction()
    {
        if (gameState != WAITING_FOR_MONOPOLY)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * perform the Discovery card action
     *
     * @param pick  what the player picked
     */
    public void doDiscoveryAction(SOCResourceSet pick)
    {
        for (int i = SOCResourceConstants.CLAY; i <= SOCResourceConstants.WOOD;
                i++)
        {
            players[currentPlayerNumber].getResources().add(pick.getAmount(i), i);
        }

        gameState = oldGameState;
    }

    /**
     * perform the Monopoly card action
     *
     * @param pick  the type of resource to monopolize
     */
    public void doMonopolyAction(int pick)
    {
        int sum = 0;

        for (int i = 0; i < MAXPLAYERS; i++)
        {
            if (! isSeatVacant(i))
            {
                sum += players[i].getResources().getAmount(pick);
                players[i].getResources().setAmount(0, pick);
            }
        }

        players[currentPlayerNumber].getResources().setAmount(sum, pick);
        gameState = oldGameState;
    }

    /**
     * update which player has the largest army
     * larger than 2
     */
    public void updateLargestArmy()
    {
        int size;

        if (playerWithLargestArmy == -1)
        {
            size = 2;
        }
        else
        {
            size = players[playerWithLargestArmy].getNumKnights();
        }

        for (int i = 0; i < MAXPLAYERS; i++)
        {
            if (players[i].getNumKnights() > size)
            {
                playerWithLargestArmy = i;
            }
        }
    }

    /**
     * save the state of who has largest army
     */
    public void saveLargestArmyState()
    {
        oldPlayerWithLargestArmy = playerWithLargestArmy;
    }

    /**
     * restore the state of who had largest army
     */
    public void restoreLargestArmyState()
    {
        playerWithLargestArmy = oldPlayerWithLargestArmy;
    }

    /**
     * update which player has longest road longer
     * than 4
     *
     * this version only calculates the longest road for
     * the player who is affected by the most recently
     * placed piece
     *
     * @param pn  the number of the player who is affected
     */
    public void updateLongestRoad(int pn)
    {
        //D.ebugPrintln("## updateLongestRoad("+pn+")");
        int longestLength;
        int playerLength;
        int tmpPlayerWithLR = -1;

        players[pn].calcLongestRoad2();
        longestLength = 4;

        for (int i = 0; i < MAXPLAYERS; i++)
        {
            playerLength = players[i].getLongestRoadLength();

            //D.ebugPrintln("----- LR length for player "+i+" is "+playerLength);
            if (playerLength > longestLength)
            {
                longestLength = playerLength;
                tmpPlayerWithLR = i;
            }
        }

        if (longestLength == 4)
        {
            playerWithLongestRoad = -1;
        }
        else
        {
            ///
            /// if there is a tie, the last player to have LR keeps it.
            /// if two or more players are tied for LR and none of them
            /// of them used to have LR, then no one has LR.
            ///
            int playersWithLR = 0;

            for (int i = 0; i < MAXPLAYERS; i++)
            {
                if (players[i].getLongestRoadLength() == longestLength)
                {
                    playersWithLR++;
                }
            }

            if (playersWithLR == 1)
            {
                playerWithLongestRoad = tmpPlayerWithLR;
            }
            else if ((playerWithLongestRoad == -1) || (players[playerWithLongestRoad].getLongestRoadLength() != longestLength))
            {
                playerWithLongestRoad = -1;
            }
        }

        //D.ebugPrintln("----- player "+playerWithLongestRoad+" has LR");
    }

    /**
     * check all the vp totals to see if the
     * game is over.  Set game state to OVER,
     * set player with win.
     *
     * @see #getGameState()
     * @see #getPlayerWithWin()
     */
    public void checkForWinner()
    {
        for (int i = 0; i < MAXPLAYERS; i++)
        {
            if (players[i].getTotalVP() >= 10)
            {
                gameState = OVER;
                playerWithWin = i;
                break;
            }
        }
    }

    /**
     * set vars to null so gc can clean up
     */
    public void destroyGame()
    {
        for (int i = 0; i < MAXPLAYERS; i++)
        {
            if (players[i] != null)
            {
                players[i].destroyPlayer();
                players[i] = null;
            }
        }

        players = null;
        board = null;
        rand = null;
    }

    /**
     * Create a new game with same players and name, new board;
     * like calling constructor otherwise.
     * State of current game can be any state. State of copy is NEW.
     * Deep copy: Player names, faceIDs, and robot-flag are copied from
     * old game, but all other fields set as new Player and Board objects.
     * Robot players are NOT carried over, and must be asked to re-join.
     * (This simplifies the robot client.)
     *<P>
     * Old game's state becomes RESET_OLD.
     * Old game's previous state is saved to {@link #getResetOldGameState()}.
     * Please call destroyGame() on old game when done examining its state.
     *<P>
     * Assumes that if the game had more than one human player,
     * they've already voted interactively to reset the board.
     * @see #resetVoteBegin(int)
     */
    public SOCGame resetAsCopy()
    {
        SOCGame cp = new SOCGame(name, active);
        cp.isFromBoardReset = true;
        oldGameState = gameState;  // for getResetOldGameState()
        gameState = RESET_OLD;
        for (int i = 0; i < MAXPLAYERS; i++)
        {
            boolean wasRobot = false;
            if (players[i] != null)
            {
                wasRobot = players[i].isRobot();
                if (! wasRobot)
                {
                    cp.addPlayer(players[i].getName(), i);
                    cp.players[i].setRobotFlag (false);
                    cp.players[i].setFaceId(players[i].getFaceId());
                }
            }
            cp.seatLocks[i] = seatLocks[i];
            if (wasRobot)
                cp.seats[i] = VACANT;
            else
                cp.seats[i] = seats[i];  // reset in case addPlayer cleared VACANT for non-in-use player position
        }
        return cp;
    }

    /**
     * Begin a board-reset vote.
     *
     * @param reqPN Player number requesting the vote
     * @throws IllegalArgumentException If this player number has already
     *     requested a reset this turn
     * @throws IllegalStateException If there is alread a vote in progress 
     *
     * @see #getResetVoteRequester()
     * @see #resetVoteRegister(int, boolean)
     * @see #getResetVoteResult()
     */
    public void resetVoteBegin(int reqPN) throws IllegalArgumentException, IllegalStateException
    {
        if (players[reqPN].hasAskedBoardReset())
            throw new IllegalArgumentException("Player has already asked to reset this turn");

        int numVoters = 0;
        synchronized (boardResetVotes)
        {
             if (boardResetVoteRequester != -1)
                 throw new IllegalStateException("Already voting");
             boardResetVoteRequester = reqPN;
             for (int i = 0; i < MAXPLAYERS; ++i)
             {
                 boardResetVotes[i] = 0;
                 if ((i != reqPN) && ! (isSeatVacant(i) || players[i].isRobot()))
                     ++numVoters;
             }
             boardResetVotesWaiting = numVoters;
        }

        if (gameState >= PLAY)
        {
            players[reqPN].setAskedBoardReset(true);
            // During game setup, normal end-of-turn flags aren't
            // cleared.  Easier to just not set this one.
        }
    }

    /**
     * If a board reset vote is active, player number who requested the vote.
     * All human players must vote unanimously, or board reset is rejected.
     * -1 if no vote is active.
     * After the vote completes, this is set to -1 if the vote was rejected,
     * but retains the requester number if the vote succeeded and the board
     * will soon be reset.
     *
     * @return player number who requested the vote.
     *
     * @see #resetVoteBegin(int)
     */
    public int getResetVoteRequester()
    {
        int rv;
        synchronized (boardResetVotes)
        {
            rv = boardResetVoteRequester;
        }
        return rv;
    }

    /**
     * @return if a board-reset vote is active (waiting for votes).
     */
    public boolean getResetVoteActive()
    {
        boolean va;
        synchronized (boardResetVotes)
        {
            va = (boardResetVotesWaiting > 0);
        }
        return va;        
    }

    /**
     * Register this player's vote in a board reset request.
     *
     * @param pn  Player number
     * @param votingYes Are they voting yes, or no?
     * @return True if voting is now complete, false if still waiting for other votes
     * @throws IllegalArgumentException If pn already voted, or can't vote (vacant or robot).
     * @throws IllegalStateException    If voting is not currently active.
     */
    public boolean resetVoteRegister(int pn, boolean votingYes)
        throws IllegalArgumentException, IllegalStateException
    {
        boolean vcomplete;
        synchronized (boardResetVotes)
        {
            if (boardResetVotes[pn] != 0)
                throw new IllegalArgumentException("Already voted: " + pn);
            if (isSeatVacant(pn) || players[pn].isRobot())
                throw new IllegalArgumentException("Seat cannot vote: " + pn);
            if ((0 == boardResetVotesWaiting) || (-1 == boardResetVoteRequester))
                throw new IllegalStateException("Voting is not active");
            if (votingYes)
                boardResetVotes[pn] = 1;
            else
                boardResetVotes[pn] = 2;
            --boardResetVotesWaiting;
            vcomplete = (0 == boardResetVotesWaiting);
            if (vcomplete)
            {
                if (! getResetVoteResult())
                    boardResetVoteRequester = -1;  // Board Reset rejected; clear requester.
            }
        }
        return vcomplete;
    }

    /**
     * If a board-reset vote is complete, give its result.
     * All human players must vote unanimously, or board reset is rejected.
     *
     * @return True if accepted, false if rejected.
     * @throws IllegalStateException if voting is still active. See {@link #getResetVoteActive()}.
     */
    public boolean getResetVoteResult() throws IllegalStateException
    {
        boolean vr;
        synchronized (boardResetVotes)
        {
            if (boardResetVotesWaiting > 0)
                throw new IllegalStateException("Voting is still active");

            vr = true;  // Assume no "no" votes
            for (int i = 0; i < MAXPLAYERS; ++i)
                if (boardResetVotes[i] == 2)
                {
                    vr = false;
                    break;
                }
        }
        return vr;        
    }

}
