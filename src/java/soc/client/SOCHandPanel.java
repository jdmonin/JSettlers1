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
package soc.client;

import soc.disableDebug.D;

import soc.game.SOCDevCardConstants;
import soc.game.SOCDevCardSet;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCResourceConstants;
import soc.game.SOCResourceSet;
import soc.game.SOCTradeOffer;
import soc.message.SOCGameTextMsg;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Timer;  // For auto-roll
import java.util.TimerTask;


/**
 * This panel displays a player's information.
 * If the player is us, then more information is
 * displayed than in another player's hand panel.
 *
 * Custom layout: @see #doLayout()
 */
public class SOCHandPanel extends Panel implements ActionListener
{
    public static final int ROADS = 0;
    public static final int SETTLEMENTS = 1;
    public static final int CITIES = 2;
    public static final int NUMRESOURCES = 3;
    public static final int NUMDEVCARDS = 4;
    public static final int NUMKNIGHTS = 5;
    public static final int VICTORYPOINTS = 6;
    public static final int LONGESTROAD = 7;
    public static final int LARGESTARMY = 8;
    public static final int CLAY = 9;
    public static final int ORE = 10;
    public static final int SHEEP = 11;
    public static final int WHEAT = 12;
    public static final int WOOD = 13;

    /** Auto-roll timer countdown, 5 seconds unless changed at program start. */
    public static int AUTOROLL_TIME = 5;

    protected static final int[] zero = { 0, 0, 0, 0, 0 };
    protected static final String SITLOCKED = "Locked: No robot";
    protected static final String SIT = "Sit Here";
    protected static final String START = "Start Game";
    protected static final String ROBOT = "Robot";
    protected static final String TAKEOVER = "Take Over";
    protected static final String LOCKSEAT = "Lock";
    protected static final String UNLOCKSEAT = "Unlock";
    protected static final String ROLL = "Roll";
    protected static final String QUIT = "Quit";
    protected static final String DONE = "Done";
    /** Text of Done button at end of game becomes Restart button */
    protected static final String DONE_RESTART = "Restart";
    protected static final String CLEAR = "Clear";
    protected static final String SEND = "Offer";
    protected static final String BANK = "Bank/Port";
    protected static final String CARD = "  Play Card  ";
    protected static final String GIVE = "I Give:";  // No trailing space (room for wider colorsquares)
    protected static final String GET = "I Get:";
    protected static final String AUTOROLL_COUNTDOWN = "Auto-Roll in: ";
    protected static final String ROLL_OR_PLAY_CARD = "Roll or Play Card";
    protected static final String SENDBUTTIP_ENA = "Send trade offer to other players";
    protected static final String SENDBUTTIP_DIS = "To offer a trade, first click resources";

    /** If player has won the game, update pname label */
    protected static final String WINNER_SUFFIX = " - Winner";

    /** Panel text color, and player name color when not current player */
    protected static final Color COLOR_FOREGROUND = Color.BLACK;
    /** Player name background color when current player (foreground does not change) */
    protected Color pnameActiveBG;

    protected Button sitBut;
    protected Button robotBut;
    protected Button startBut;
    protected Button takeOverBut;

    /** Seat lock/unlock shown in robot handpanels during game play,
     *  to prevent/allow humans to join and take over a robot's seat
     */
    protected Button seatLockBut;

    /** When true, the game is still forming, player has chosen a seat;
     *  "Sit Here" button is labeled as "Lock".  Humans can use this to
     *  lock robots out of that seat, so as to start a game with fewer
     *  players and some vacant seats.
     */
    protected boolean sitButIsLock;
    protected SOCFaceButton faceImg;
    protected Label pname;
    protected Label vpLab;
    protected ColorSquare vpSq;
    protected Label larmyLab;
    protected Label lroadLab;
    protected ColorSquare claySq;
    protected ColorSquare oreSq;
    protected ColorSquare sheepSq;
    protected ColorSquare wheatSq;
    protected ColorSquare woodSq;
    protected Label clayLab;
    protected Label oreLab;
    protected Label sheepLab;
    protected Label wheatLab;
    protected Label woodLab;
    protected ColorSquare settlementSq;
    protected ColorSquare citySq;
    protected ColorSquare roadSq;
    protected Label settlementLab;
    protected Label cityLab;
    protected Label roadLab;
    protected ColorSquare resourceSq;
    protected Label resourceLab;
    protected ColorSquare developmentSq;
    protected Label developmentLab;
    protected ColorSquare knightsSq;
    protected Label knightsLab;
    //protected Label cardLab; // no longer used?
    protected List cardList;
    protected Button playCardBut;
    protected SquaresPanel sqPanel;
    protected Label giveLab;
    protected Label getLab;
    protected Button sendBut;
    /**
     * Hint for "Offer" button; non-null only if interactive.
     * @see #SENDBUTTIP_DIS
     * @see #SENDBUTTIP_ENA
     * @see #interactive
     */
    protected AWTToolTip sendButTip;
    protected Button clearBut;
    protected Button bankBut;

    /**
     * Checkboxes to send to the other three players.
     * Enabled/disabled at removeStartBut().
     *
     * @see #playerSendMap
     */
    protected ColorSquare[] playerSend;

    /** displays auto-roll countdown, or prompts to roll/play card.
     * @see #setRollPrompt(String)
     */
    protected Label rollPromptCountdownLab;
    protected boolean rollPromptInUse;
    protected Timer autoRollTimer;  // Created just once
    protected TimerTask autoRollTimerTask;  // Created every turn when countdown needed
    protected Button rollBut;
    /** "Done" with turn during play; also "Restart" for board reset at end of game */
    protected Button doneBut;
    protected Button quitBut;
    protected SOCPlayerInterface playerInterface;
    protected SOCPlayerClient client;
    protected SOCGame game;
    protected SOCPlayer player;
    /** Does this panel represent our client's own hand?  If true, implies {@link #interactive}. */
    protected boolean playerIsClient;
    /** Is this panel's player the game's current player?  Used for hilight - set in updateAtTurn() */
    protected boolean playerIsCurrent;
    protected boolean inPlay;

    /** Three player numbers to send trade offers to.
     *  For i from 0 to 2, playerSendMap[i] is playerNumber for checkbox i.
     *
     * @see #playerSend
     */
    protected int[] playerSendMap;

    /**
     * Display other players' trade offers and related messages. Not used if playerIsClient.
     * Also used to display board-reset vote messages.
     *
     * @see #offerIsResetMessage
     */
    protected TradeOfferPanel offer;

    /**
     * Board-reset voting: If true, {@link #offer} is holding a message related to a board-reset vote.
     */
    protected boolean offerIsResetMessage;

    /**
     * Board-reset voting: If true, {@link #offer} was holding an active trade offer before {@link #offerIsResetMessage} was set.
     */
    protected boolean offerIsResetWasTrade;

    /**
     * When this flag is true, the panel is interactive.
     * If {@link #playerIsClient} true, implies interactive.
     */
    protected boolean interactive;

    private boolean chatExcepTested = true;  // JM: For testing with BANK button (TODO cleanup)

    /**
     * make a new hand panel
     *
     * @param pi  the interface that this panel is a part of
     * @param pl  the player associated with this panel
     * @param in  the interactive flag setting
     */
    public SOCHandPanel(SOCPlayerInterface pi, SOCPlayer pl, boolean in)
    {
        super(null);
        creation(pi, pl, in);
    }

    /**
     * make a new hand panel
     *
     * @param pi  the interface that this panel is a part of
     * @param pl  the player associated with this panel
     */
    public SOCHandPanel(SOCPlayerInterface pi, SOCPlayer pl)
    {
        this(pi, pl, true);
    }

    /**
     * Stuff to do when a SOCHandPanel is created.
     *   Calls removePlayer() as part of creation.
     *
     * @param pi   player interface
     * @param pl   the player data
     * @param in   the interactive flag setting
     */
    protected void creation(SOCPlayerInterface pi, SOCPlayer pl, boolean in)
    {
        playerInterface = pi;
        client = pi.getClient();
        game = pi.getGame();
        player = pl;
        playerIsCurrent = false;
        playerIsClient = false;  // confirmed by call to removePlayer() at end of method.
        interactive = in;

        // Note no AWT layout is used - custom layout, see doLayout().

        setBackground(playerInterface.getPlayerColor(player.getPlayerNumber()));
        setForeground(COLOR_FOREGROUND);
        setFont(new Font("Helvetica", Font.PLAIN, 10));

        faceImg = new SOCFaceButton(playerInterface, player.getPlayerNumber());
        add(faceImg);

        pname = new Label();
        pname.setFont(new Font("Serif", Font.PLAIN, 14));
        add(pname);
        pnameActiveBG = null;  // Will be calculated at first turn

        startBut = new Button(START);
        startBut.addActionListener(this);
        // this button always enabled
        add(startBut);

        vpLab = new Label("Points: ");
        add(vpLab);
        vpSq = new ColorSquare(ColorSquare.GREY, 0);
        vpSq.setTooltipText("Total victory points for this opponent");
        vpSq.setTooltipHighWarningLevel("Close to winning", 8);  // TODO assumes playing until 10 (hardcoded in SOCGame.checkForWinner)
        add(vpSq);

        larmyLab = new Label("", Label.CENTER);
        larmyLab.setForeground(new Color(142, 45, 10));
        larmyLab.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(larmyLab);

        lroadLab = new Label("", Label.CENTER);
        lroadLab.setForeground(new Color(142, 45, 10));
        lroadLab.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(lroadLab);

        clayLab = new Label("Clay:");
        add(clayLab);
        claySq = new ColorSquare(ColorSquare.CLAY, 0);
        add(claySq);

        oreLab = new Label("Ore:");
        add(oreLab);
        oreSq = new ColorSquare(ColorSquare.ORE, 0);
        add(oreSq);

        sheepLab = new Label("Sheep:");
        add(sheepLab);
        sheepSq = new ColorSquare(ColorSquare.SHEEP, 0);
        add(sheepSq);

        wheatLab = new Label("Wheat:");
        add(wheatLab);
        wheatSq = new ColorSquare(ColorSquare.WHEAT, 0);
        add(wheatSq);

        woodLab = new Label("Wood:");
        add(woodLab);
        woodSq = new ColorSquare(ColorSquare.WOOD, 0);
        add(woodSq);

        //cardLab = new Label("Cards:");
        //add(cardLab);
        cardList = new List(0, false);
        cardList.addActionListener(this);  // double-click support
        add(cardList);

        roadSq = new ColorSquare(ColorSquare.GREY, 0);
        add(roadSq);
        roadSq.setTooltipText("Pieces available to place");
        roadSq.setTooltipLowWarningLevel("Almost out of roads to place", 2);
        roadSq.setTooltipZeroText("No more roads available");
        roadLab = new Label("Roads:");
        add(roadLab);

        settlementSq = new ColorSquare(ColorSquare.GREY, 0);
        add(settlementSq);
        settlementSq.setTooltipText("Pieces available to place");
        settlementSq.setTooltipLowWarningLevel("Almost out of settlements to place", 1);
        settlementSq.setTooltipZeroText("No more settlements available");
        settlementLab = new Label("Stlmts:");
        add(settlementLab);

        citySq = new ColorSquare(ColorSquare.GREY, 0);
        add(citySq);
        citySq.setTooltipText("Pieces available to place");
        citySq.setTooltipLowWarningLevel("Almost out of cities to place", 1);
        citySq.setTooltipZeroText("No more cities available");
        cityLab = new Label("Cities:");
        add(cityLab);

        knightsLab = new Label("Soldiers:");  // No trailing space (room for wider colorsquares at left)
        add(knightsLab);
        knightsSq = new ColorSquare(ColorSquare.GREY, 0);
        add(knightsSq);
        knightsSq.setTooltipText("Size of this army");

        resourceLab = new Label("Resources: ");
        add(resourceLab);
        resourceSq = new ColorSquare(ColorSquare.GREY, 0);
        add(resourceSq);
        resourceSq.setTooltipText("Amount in hand");
        resourceSq.setTooltipHighWarningLevel("If 7 is rolled, would discard half these resources", 8);

        developmentLab = new Label("Dev. Cards: ");
        add(developmentLab);
        developmentSq = new ColorSquare(ColorSquare.GREY, 0);
        add(developmentSq);
        developmentSq.setTooltipText("Amount in hand");

        seatLockBut = new Button(UNLOCKSEAT);
        seatLockBut.addActionListener(this);
        seatLockBut.setEnabled(interactive);
        add(seatLockBut);

        takeOverBut = new Button(TAKEOVER);
        takeOverBut.addActionListener(this);
        takeOverBut.setEnabled(interactive);
        add(takeOverBut);

        sitBut = new Button(SIT);
        sitBut.addActionListener(this);
        sitBut.setEnabled(interactive);
        add(sitBut);
        sitButIsLock = false;

        robotBut = new Button(ROBOT);
        robotBut.addActionListener(this);
        robotBut.setEnabled(interactive);
        add(robotBut);

        playCardBut = new Button(CARD);
        playCardBut.addActionListener(this);
        playCardBut.setEnabled(interactive);
        add(playCardBut);

        giveLab = new Label(GIVE);
        add(giveLab);

        getLab = new Label(GET);
        add(getLab);

        sqPanel = new SquaresPanel(interactive, this);
        add(sqPanel);
        sqPanel.setVisible(false); // else it's visible in all (dunno why?)

        sendBut = new Button(SEND);
        sendBut.addActionListener(this);
        sendBut.setEnabled(interactive);
        add(sendBut);
        if (interactive)
            sendButTip = new AWTToolTip(SENDBUTTIP_ENA, sendBut);

        clearBut = new Button(CLEAR);
        clearBut.addActionListener(this);
        clearBut.setEnabled(interactive);
        add(clearBut);

        bankBut = new Button(BANK);
        bankBut.addActionListener(this);
        bankBut.setEnabled(interactive);
        add(bankBut);

        playerSend = new ColorSquare[SOCGame.MAXPLAYERS-1];
        playerSendMap = new int[SOCGame.MAXPLAYERS-1];

        // set the trade buttons correctly
        int cnt = 0;
        for (int pn = 0; pn < SOCGame.MAXPLAYERS; pn++)
        {
            if (pn != player.getPlayerNumber())
            {
                Color color = playerInterface.getPlayerColor(pn);
                playerSendMap[cnt] = pn;
                playerSend[cnt] = new ColorSquare(ColorSquare.CHECKBOX, true, color);
                playerSend[cnt].setColor(playerInterface.getPlayerColor(pn));
                playerSend[cnt].setBoolValue(true);
                add(playerSend[cnt]);
                cnt++;
            }
        }

        rollPromptCountdownLab = new Label(" ");
        add(rollPromptCountdownLab);
        rollPromptInUse = false;   // Nothing yet (no game in progress)
        autoRollTimer = null;      // Nothing yet
        autoRollTimerTask = null;  // Nothing yet

        rollBut = new Button(ROLL);
        rollBut.addActionListener(this);
        rollBut.setEnabled(interactive);
        add(rollBut);

        doneBut = new Button(DONE);
        doneBut.addActionListener(this);
        doneBut.setEnabled(interactive);
        add(doneBut);

        quitBut = new Button(QUIT);
        quitBut.addActionListener(this);
        quitBut.setEnabled(interactive);
        add(quitBut);

        offer = new TradeOfferPanel(this, player.getPlayerNumber());
        offer.setVisible(false);
        offerIsResetMessage = false;        
        add(offer);

        // set the starting state of the panel
        removePlayer();
    }

    /**
     * @return the player interface
     */
    public SOCPlayerInterface getPlayerInterface()
    {
        return playerInterface;
    }

    /**
     * @return the player
     */
    public SOCPlayer getPlayer()
    {
        return player;
    }

    /**
     * @return the client
     */
    public SOCPlayerClient getClient()
    {
        return client;
    }

    /**
     * @return the game
     */
    public SOCGame getGame()
    {
        return game;
    }

    /**
     * handle interaction
     */
    public void actionPerformed(ActionEvent e)
    {
        try {
        String target = e.getActionCommand();

        SOCPlayerClient client = playerInterface.getClient();
        SOCGame game = playerInterface.getGame();

        if (target == LOCKSEAT)
        {
            client.lockSeat(game, player.getPlayerNumber());
        }
        else if (target == UNLOCKSEAT)
        {
            client.unlockSeat(game, player.getPlayerNumber());
        }
        else if (target == TAKEOVER)
        {
            client.sitDown(game, player.getPlayerNumber());
        }
        else if (target == SIT)
        {
            client.sitDown(game, player.getPlayerNumber());
        }
        else if (target == START)
        {
            client.startGame(game);
        }
        else if (target == ROBOT)
        {
            // cf.cc.addRobot(cf.cname, playerNum);
        }
        else if (target == ROLL)
        {
            if (autoRollTimerTask != null)
            {
                autoRollTimerTask.cancel();
                autoRollTimerTask = null;
            }
            clickRollButton();
        }
        else if (target == QUIT)
        {
            SOCQuitConfirmDialog.createAndShow(client, playerInterface);
        }
        else if (target == DONE)
        {
            // sqPanel.setValues(zero, zero);
            client.endTurn(game);
        }
        else if (target == DONE_RESTART)
        {
            playerInterface.resetBoardRequest();
        }
        else if (target == CLEAR)
        {
            if (playerIsClient)
            {
                clearOffer(true);  // Zero the square panel numbers, etc. (TODO) better descr.
            } else {
                // TODO can target ever be CLEAR without playerIsClient ?
                sqPanel.setValues(zero, zero);
                clearBut.disable();
                sendBut.disable();
            }

            if (game.getGameState() == SOCGame.PLAY1)
            {
                client.clearOffer(game);
            }
        }
        else if (target == BANK)
        {
            int gstate = game.getGameState(); 
            if (gstate == SOCGame.PLAY1)
            {
                int[] give = new int[5];
                int[] get = new int[5];
                sqPanel.getValues(give, get);
                client.clearOffer(game);

                SOCResourceSet giveSet = new SOCResourceSet(give[0], give[1], give[2], give[3], give[4], 0);
                SOCResourceSet getSet = new SOCResourceSet(get[0], get[1], get[2], get[3], get[4], 0);
                client.bankTrade(game, giveSet, getSet);
            }
            else if (gstate == SOCGame.OVER)
            {
                String msg = game.gameOverMessageToPlayer(player);
                    // msg = "The game is over; you are the winner!";
                    // msg = "The game is over; <someone> won.";
                    // msg = "The game is over; no one won.";
                playerInterface.print("* " + msg);
            }
            if (! chatExcepTested)
            {
                try
                {
                    int z = Color.BLACK.getRed();
                    int dz = 15 / z;
                }
                catch (Throwable th)
                {
                    playerInterface.chatPrint("-- test of stacktrace --\n");
                    playerInterface.chatPrintStackTrace(th);
                }
                chatExcepTested = true;
            }
        }
        else if (target == SEND)
        {
            if (game.getGameState() == SOCGame.PLAY1)
            {
                int[] give = new int[5];
                int[] get = new int[5];
                int giveSum = 0;
                int getSum = 0;
                sqPanel.getValues(give, get);

                for (int i = 0; i < 5; i++)
                {
                    giveSum += give[i];
                    getSum += get[i];
                }

                SOCResourceSet giveSet = new SOCResourceSet(give[0], give[1], give[2], give[3], give[4], 0);
                SOCResourceSet getSet = new SOCResourceSet(get[0], get[1], get[2], get[3], get[4], 0);

                if (!player.getResources().contains(giveSet))
                {
                    playerInterface.print("*** You can't offer what you don't have.");
                }
                else if ((giveSum == 0) || (getSum == 0))
                {
                    playerInterface.print("*** A trade must contain at least one resource card from each player.");
                }
                else
                {
                    // bool array elements begin as false
                    boolean[] to = new boolean[SOCGame.MAXPLAYERS];
                    boolean toAny = false;

                    if (game.getCurrentPlayerNumber() == player.getPlayerNumber())
                    {
                        for (int i = 0; i < (SOCGame.MAXPLAYERS - 1); i++)
                        {
                            if (playerSend[i].getBoolValue() && ! game.isSeatVacant(playerSendMap[i]))
                            {
                                to[playerSendMap[i]] = true;
                                toAny = true;
                            }
                        }
                    }
                    else
                    {
                        // can only offer to current player
                        to[game.getCurrentPlayerNumber()] = true;
                        toAny = true;
                    }

                    if (! toAny)
                    {
                        playerInterface.print("*** Please choose at least one opponent's checkbox.");
                    }
                    else
                    {
                        SOCTradeOffer tradeOffer =
                            new SOCTradeOffer(game.getName(),
                                              player.getPlayerNumber(),
                                              to, giveSet, getSet);
                        client.offerTrade(game, tradeOffer);
                    }
                }
            }
        }
        else if ((e.getSource() == cardList) || (target == CARD))
        {
            String item;
            int itemNum;

            item = cardList.getSelectedItem();
            itemNum = cardList.getSelectedIndex();

            if (item == null || item.length() == 0)
            {
                if (cardList.getItemCount() == 1)
                {
                    // No card selected, but only one to choose from
                    item = cardList.getItem(0);
                    itemNum = 0;
                    if (item.length() == 0)
                        return;
                } else {
                    if (cardList.getItemCount() > 1)
                    {
                        playerInterface.print("* Please click a card first to select it.");
                    }
                    return;
                }
            }

            setRollPrompt(null);  // Clear prompt if Play Card clicked (vs Roll)

            if (playerIsCurrent)
            {
                if (player.hasPlayedDevCard())
                {
                    playerInterface.print("*** You may play only one card per turn.");
                    playCardBut.setEnabled(false);
                }
                else if (item.equals("Soldier"))
                {
                    if (game.canPlayKnight(player.getPlayerNumber()))
                    {
                        client.playDevCard(game, SOCDevCardConstants.KNIGHT);
                    }
                }
                else if (item.equals("Road Building"))
                {
                    if (game.canPlayRoadBuilding(player.getPlayerNumber()))
                    {
                        client.playDevCard(game, SOCDevCardConstants.ROADS);
                    }
                }
                else if (item.equals("Year of Plenty"))
                {
                    if (game.canPlayDiscovery(player.getPlayerNumber()))
                    {
                        client.playDevCard(game, SOCDevCardConstants.DISC);
                    }
                }
                else if (item.equals("Monopoly"))
                {
                    if (game.canPlayMonopoly(player.getPlayerNumber()))
                    {
                        client.playDevCard(game, SOCDevCardConstants.MONO);
                    }
                }
                else if (item.indexOf("VP)") > 0)
                {
                    playerInterface.print("*** You secretly played this VP card when you bought it.");
                    itemNum = cardList.getSelectedIndex();
                    if (itemNum >= 0)
                        cardList.deselect(itemNum);
                }
            }
        }
        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }

    /** Handle a click on the roll button.
     *  Called from actionPerformed() and the auto-roll timer task.
     */
    public void clickRollButton()
    {
        if (rollPromptInUse)
            setRollPrompt(null);  // Clear it
        client.rollDice(game);
        rollBut.setEnabled(false);  // Only one roll per turn
    }

    /**
     * Add the "lock" button for when a robot is currently playing in this position.
     * This is not the large "lock" button seen in empty positions when the
     * game is forming, which prevents a robot from sitting down. That button
     * is actually sitBut with a different label.
     */
    public void addSeatLockBut()
    {
        D.ebugPrintln("*** addSeatLockBut() ***");
        D.ebugPrintln("seatLockBut = " + seatLockBut);

            if (game.isSeatLocked(player.getPlayerNumber()))
            {
                seatLockBut.setLabel(UNLOCKSEAT);
            }
            else
            {
                seatLockBut.setLabel(UNLOCKSEAT);
            }

            seatLockBut.setVisible(true);

            //seatLockBut.repaint();
    }

    /**
     * DOCUMENT ME!
     */
    public void addTakeOverBut()
    {
        takeOverBut.setVisible(true);
    }

    /**
     * Add the "Sit Here" button. If this button has been used as
     * a "lock" button to keep out a robot, revert the label to "Sit Here".
     */
    public void addSitButton()
    {
        if (player.getName() == null)
        {
            if (sitButIsLock)
            {
                sitBut.setLabel(SIT);
                sitButIsLock = false;
            }
            sitBut.setVisible(true);
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void addRobotButton()
    {
        robotBut.setVisible(true);
    }

    /**
     * Change the face image
     *
     * @param id  the id of the image
     */
    public void changeFace(int id)
    {
        faceImg.setFace(id);
    }


    /**
     * remove this player
     */
    public void removePlayer()
    {
        //D.ebugPrintln("REMOVE PLAYER");
        //D.ebugPrintln("NAME = "+player.getName());
        vpLab.setVisible(false);
        vpSq.setVisible(false);
        faceImg.setVisible(false);
        pname.setVisible(false);
        roadSq.setVisible(false);
        roadLab.setVisible(false);
        settlementLab.setVisible(false);
        settlementSq.setVisible(false);
        cityLab.setVisible(false);
        citySq.setVisible(false);
        knightsSq.setVisible(false);
        knightsLab.setVisible(false);

        offer.setVisible(false);

        larmyLab.setVisible(false);
        lroadLab.setVisible(false);

        if (playerIsClient)
        {
            // Clean up, since we're leaving the game
            if (playerInterface.getClientHand() == this)
                playerInterface.setClientHand(null);
            playerIsClient = false;
        }

        if (game.getPlayer(client.getNickname()) == null &&
            game.getGameState() == game.NEW)
        {
            if (sitButIsLock)
            {
                sitBut.setLabel(SIT);  // revert from lockout to sit-here
                sitButIsLock = false;
            }
            sitBut.setVisible(true);
        }

        /* Hide items in case this was our hand */
        claySq.setVisible(false);
        clayLab.setVisible(false);
        oreSq.setVisible(false);
        oreLab.setVisible(false);
        sheepSq.setVisible(false);
        sheepLab.setVisible(false);
        wheatSq.setVisible(false);
        wheatLab.setVisible(false);
        woodSq.setVisible(false);
        woodLab.setVisible(false);

        //cardLab.setVisible(false);
        cardList.setVisible(false);
        playCardBut.setVisible(false);

        giveLab.setVisible(false);
        getLab.setVisible(false);
        sqPanel.setVisible(false);
        sendBut.setVisible(false);  // also hides sendButTip if created
        clearBut.setVisible(false);
        bankBut.setVisible(false);

        for (int i = 0; i < (SOCGame.MAXPLAYERS - 1); i++)
        {
            playerSend[i].setVisible(false);
        }

        rollBut.setVisible(false);
        doneBut.setVisible(false);
        quitBut.setVisible(false);

        setRollPrompt(null);  // Clear it
        if (autoRollTimerTask != null)
        {
            autoRollTimerTask.cancel();
            autoRollTimerTask = null;
        }

        /* other player's hand */
        resourceLab.setVisible(false);
        resourceSq.setVisible(false);
        developmentLab.setVisible(false);
        developmentSq.setVisible(false);
        faceImg.removeFacePopupMenu();  // Also disables left-click to change

        removeTakeOverBut();
        removeSeatLockBut();

        inPlay = false;

        validate();
        repaint();
    }

    /**
     * DOCUMENT ME!
     *
     * @param name DOCUMENT ME!
     */
    public void addPlayer(String name)
    {
        /* This is visible for both our hand and opponent hands */
        faceImg.setDefaultFace();
        faceImg.setVisible(true);

        pname.setText(name);
        pname.setVisible(true);

        larmyLab.setVisible(true);
        lroadLab.setVisible(true);

        roadSq.setVisible(true);
        roadLab.setVisible(true);
        settlementSq.setVisible(true);
        settlementLab.setVisible(true);
        citySq.setVisible(true);
        cityLab.setVisible(true);
        knightsLab.setVisible(true);
        knightsSq.setVisible(true);

        playerIsCurrent = (game.getCurrentPlayerNumber() == player.getPlayerNumber());

        if (player.getName().equals(client.getNickname()))
        {
            D.ebugPrintln("SOCHandPanel.addPlayer: This is our hand");

            playerIsClient = true;
            playerInterface.setClientHand(this);

            knightsSq.setTooltipText("Size of your army");
            vpSq.setTooltipText("Your victory point total");

            // show 'Victory Points' and hide "Start Button" if game in progress
            if (game.getGameState() == game.NEW)
            {
                startBut.setVisible(true);
            }
            else
            {
                vpLab.setVisible(true);
                vpSq.setVisible(true);
            }

            faceImg.addFacePopupMenu();  // Also enables left-click to change

            claySq.setVisible(true);
            clayLab.setVisible(true);
            oreSq.setVisible(true);
            oreLab.setVisible(true);
            sheepSq.setVisible(true);
            sheepLab.setVisible(true);
            wheatSq.setVisible(true);
            wheatLab.setVisible(true);
            woodSq.setVisible(true);
            woodLab.setVisible(true);

            //cardLab.setVisible(true);
            cardList.setVisible(true);
            playCardBut.setVisible(true);

            giveLab.setVisible(true);
            getLab.setVisible(true);
            sqPanel.setVisible(true);

            sendBut.setVisible(true);
            clearBut.setVisible(true);
            bankBut.setVisible(true);

            for (int i = 0; i < (SOCGame.MAXPLAYERS - 1); i++)
            {
                playerSend[i].setBoolValue(true);
                playerSend[i].setEnabled(true);
                playerSend[i].setVisible(true);
            }
            rollBut.setVisible(true);
            if (game.getGameState() != SOCGame.OVER)
                doneBut.setLabel(DONE);
            else
                doneBut.setLabel(DONE_RESTART);
            doneBut.setVisible(true);
            quitBut.setVisible(true);

            // Remove all of the sit and take over buttons.
            // If game still forming, can lock seats (for fewer players/robots).
            boolean gameForming = (game.getGameState() == game.NEW);
            int pnum = player.getPlayerNumber();
            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                playerInterface.getPlayerHandPanel(i).removeTakeOverBut();
                if (gameForming && (i != pnum) && game.isSeatVacant(i))
                    playerInterface.getPlayerHandPanel(i).renameSitButLock();
                else
                    playerInterface.getPlayerHandPanel(i).removeSitBut();
            }

            updateButtonsAtAdd();  // Enable,disable the proper buttons
        }
        else
        {
            /* This is another player's hand */

            D.ebugPrintln("**** SOCHandPanel.addPlayer(name) ****");
            D.ebugPrintln("player.getPlayerNumber() = " + player.getPlayerNumber());
            D.ebugPrintln("player.isRobot() = " + player.isRobot());
            D.ebugPrintln("game.isSeatLocked(" + player.getPlayerNumber() + ") = " + game.isSeatLocked(player.getPlayerNumber()));
            D.ebugPrintln("game.getPlayer(client.getNickname()) = " + game.getPlayer(client.getNickname()));

            knightsSq.setTooltipText("Size of this opponent's army");

            if (player.isRobot() && (game.getPlayer(client.getNickname()) == null) && (!game.isSeatLocked(player.getPlayerNumber())))
            {
                addTakeOverBut();
            }

            if (player.isRobot() && (game.getPlayer(client.getNickname()) != null))
            {
                addSeatLockBut();
            }
            else
            {
                removeSeatLockBut();
            }

            vpLab.setVisible(true);
            vpSq.setVisible(true);

            resourceLab.setVisible(true);
            resourceSq.setVisible(true);
            developmentLab.setVisible(true);
            developmentSq.setVisible(true);

            removeSitBut();
            removeRobotBut();
        }

        inPlay = true;

        validate();
        repaint();
    }

    /** Player is client, is current, and has no playable cards,
     *  so begin auto-roll countdown.
     *
     * Called by autoRollOrPromptPlayer when that condition is met.
     * Countdown begins with AUTOROLL_TIME seconds.
     *
     * @see #autoRollOrPromptPlayer()
     */
    protected void autoRollSetupTimer()
    {
        if (autoRollTimerTask != null)
            autoRollTimerTask.cancel();  // cancel any previous
        if (! game.canRollDice(player.getPlayerNumber()))
            return;
        if (autoRollTimer == null)
            autoRollTimer = new Timer(true);  // use daemon thread

        // Set up to run once per second, it will cancel
        //   itself after AUTOROLL_TIME seconds.
        autoRollTimerTask = new HandPanelAutoRollTask();
        autoRollTimer.scheduleAtFixedRate(autoRollTimerTask, 0, 1000 /* ms */ );
    }

    /**
     * Calls updateTakeOverButton, and checks if current player (for hilight).
     *
     * @see #updateTakeOverButton()
     */
    public void updateAtTurn()
    {
        playerIsCurrent = (game.getCurrentPlayerNumber() == player.getPlayerNumber());
        if (playerIsCurrent)
        {
            if (pnameActiveBG == null)
                pnameCalcColors();
            pname.setBackground(pnameActiveBG);
            updateRollButton();
        }
        else
        {
            pname.setBackground(this.getBackground());
        }

        updateTakeOverButton();
        if (playerIsClient)
        {
            int gs = game.getGameState();
            boolean normalTurnStarting = (gs == SOCGame.PLAY || gs == SOCGame.PLAY1);
            clearOffer(normalTurnStarting);  // Zero the square panel numbers, etc. (TODO) better descr.
                // at any player's turn, not just when playerIsCurrent.
            normalTurnStarting = normalTurnStarting && playerIsCurrent;
            doneBut.setEnabled(normalTurnStarting);
            playCardBut.setEnabled(normalTurnStarting && (cardList.getItemCount() > 0));
            bankBut.disable();  // enabled by updateAtPlay1()
        }

        // Although this method is called at the start of our turn,
        // the call to autoRollOrPromptPlayer() is not made here.
        // That call is made when the server says it's our turn to
        // roll, via a SOCRollDicePrompt message.
        // We can then avoid tracking the game's current and
        // previous states in various places in the UI;
        // the server sends such messages at other times (states)
        // besides start-of-turn.
    }

    /**
     * Client is current player; state changed from PLAY to PLAY1.
     * (Dice has been rolled, or card played.)
     * Update interface accordingly.
     * Should not be called except by client's playerinterface.
     */
    public void updateAtPlay1()
    {
       if (! playerIsClient)
           return;

       bankBut.enable();
    }

    /** Enable,disable the proper buttons
     * when the client (player) is added to a game.
     */
    public void updateButtonsAtAdd()
    {
        if (playerIsCurrent)
        {
            updateRollButton();
            bankBut.setEnabled(game.getGameState() == SOCGame.PLAY1);
        }
        else
        {
            rollBut.disable();
            doneBut.disable();
            playCardBut.disable();
            bankBut.disable();  // enabled by updateAtPlay1()
        }

        clearBut.disable();  // No trade offer has been set yet
        sendBut.disable();
        if (sendButTip != null)
            sendButTip.setTip(SENDBUTTIP_DIS);
    }

    /**
     * During this player's first turn, calculate the player name label's
     * background color for current player.
     */
    protected void pnameCalcColors()
    {
        if (pnameActiveBG != null)
            return;
        pnameActiveBG = SOCPlayerInterface.makeGhostColor(getBackground());
    }

    /** If enable/disable buttons accordingly. */
    public void sqPanelZerosChange(boolean notAllZero)
    {
        int gs = game.getGameState();
        clearBut.setEnabled(notAllZero);
        boolean enaSendBut = notAllZero && ((gs == SOCGame.PLAY) || (gs == SOCGame.PLAY1));
        sendBut.setEnabled(enaSendBut);
        if (sendButTip != null)
        {
            if (enaSendBut)
                sendButTip.setTip(SENDBUTTIP_ENA);
            else
                sendButTip.setTip(SENDBUTTIP_DIS);
        }
    }

    /**
     * If the player (client) has no playable
     * cards, begin auto-roll countdown,
     * Otherwise, prompt them to roll or pick a card.
     *
     * Call only if panel's player is the client, and the game's current player.
     *
     * Called when server sends a SOCRollDicePrompt message.
     *
     * @see #updateAtTurn()
     * @see #autoRollSetupTimer()
     */
    public void autoRollOrPromptPlayer()
    {
        updateAtTurn();  // Game state may have changed
        if (player.hasUnplayedDevCards()
                && ! player.hasPlayedDevCard())
            setRollPrompt(ROLL_OR_PLAY_CARD);
        else
            autoRollSetupTimer();
    }

    /**
     * DOCUMENT ME!
     */
    public void updateDevCards()
    {
        SOCDevCardSet cards = player.getDevCards();

        int[] cardTypes = { SOCDevCardConstants.DISC,
                            SOCDevCardConstants.KNIGHT,
                            SOCDevCardConstants.MONO,
                            SOCDevCardConstants.ROADS,
                            SOCDevCardConstants.CAP,
                            SOCDevCardConstants.LIB,
                            SOCDevCardConstants.TEMP,
                            SOCDevCardConstants.TOW,
                            SOCDevCardConstants.UNIV };
        String[] cardNames = {"Year of Plenty",
                              "Soldier",
                              "Monopoly",
                              "Road Building",
                              "Gov. House (1VP)",
                              "Market (1VP)",
                              "Temple (1VP)",
                              "Chapel (1VP)",
                              "University (1VP)"};
        boolean hasCards = false;

        synchronized (cardList.getTreeLock())
        {
            cardList.removeAll();

            // add items to the list for each new and old card, of each type
            for (int i = 0; i < cardTypes.length; i++)
            {
                int numOld = cards.getAmount(SOCDevCardSet.OLD, cardTypes[i]);
                int numNew = cards.getAmount(SOCDevCardSet.NEW, cardTypes[i]);
                if ((numOld > 0) || (numNew > 0))
                    hasCards = true;

                for (int j = 0; j < numOld; j++)
                {
                    cardList.add(cardNames[i]);
                }
                for (int j = 0; j < numNew; j++)
                {
                    // VP cards (starting at 4) are valid immidiately
                    String prefix = (i < 4) ? "*NEW* " : "";
                    cardList.add(prefix + cardNames[i]);
                }
            }
        }

        playCardBut.setEnabled (hasCards && playerIsCurrent);
    }

    /**
     * DOCUMENT ME!
     */
    public void removeSeatLockBut()
    {
        seatLockBut.setVisible(false);
    }

    /**
     * DOCUMENT ME!
     */
    public void removeTakeOverBut()
    {
        takeOverBut.setVisible(false);
    }

    /**
     * Remove the sit-here / lockout-robot button.
     * If it's currently "lockout", revert label to "sit-here",
     * and hide the "locked, no robot" text.
     */
    public void removeSitBut()
    {
        if (sitBut.isVisible())
            sitBut.setVisible(false);
        if (sitButIsLock)
        {
            sitBut.setLabel(SIT);
            sitButIsLock = false;
            if ((player == null) || (player.getName() == null))
                pname.setVisible(false);  // Hide "Locked: No robot" text
        }
    }

    /**
     * Remove the sit-here/lockout-robot button, only if its label
     * is currently "lockout". (sitButIsLock == true).  This button
     * is also used for newly joining players to choose a seat.  If the
     * button label is "sit here", our interface is a newly joining
     * player to a game that's already started; otherwise they arrived
     * while the game was forming, and now it's started, so clean up the window.
     */
    public void removeSitLockoutBut()
    {
        if (sitButIsLock)
            removeSitBut();
    }

    /**
     * If game is still forming (state NEW), and player has
     * just chosen a seat, can lock empty seats for a game
     * with fewer players/robots. This uses the same server-interface as
     * the "lock" button shown when robot is playing in the position,
     * but a different button in the client (the sit-here button).
     */
    public void renameSitButLock()
    {
        if (game.getGameState() != SOCGame.NEW)
            return;  // TODO consider IllegalStateException
        if (game.isSeatLocked(player.getPlayerNumber()))
        {
            sitBut.setLabel(UNLOCKSEAT);  // actionPerformed target becomes UNLOCKSEAT
            pname.setText(SITLOCKED);
            pname.setVisible(true);
        }
        else
        {
            sitBut.setLabel(LOCKSEAT);
        }
        sitButIsLock = true;
        sitBut.repaint();
    }

    /**
     * DOCUMENT ME!
     */
    public void removeRobotBut()
    {
        robotBut.setVisible(false);
    }

    /**
     * Internal mechanism to remove start button (if visible) and add VP label.
     * Also refreshes status of "send-offer" checkboxes vs. vacant seats.
     */
    public void removeStartBut()
    {
        // First, hide or show victory-point buttons
        {
            boolean seatTaken = ! game.isSeatVacant(getPlayer().getPlayerNumber());
            vpLab.setVisible(seatTaken);
            vpSq.setVisible(seatTaken);
        }

        startBut.setVisible(false);

        for (int i = 0; i < (SOCGame.MAXPLAYERS - 1); i++)
        {
            boolean seatTaken = ! game.isSeatVacant(playerSendMap[i]);
            playerSend[i].setBoolValue(seatTaken);
            playerSend[i].setEnabled(seatTaken);
            if (seatTaken)
            {
                String pname = game.getPlayer(playerSendMap[i]).getName();
                if (pname != null)
                    playerSend[i].setTooltipText(pname);
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void updateCurrentOffer()
    {
        if (inPlay)
        {
            SOCTradeOffer currentOffer = player.getCurrentOffer();

            if (currentOffer != null)
            {
                if (! offerIsResetMessage)
                {
                    offer.setOffer(currentOffer);
                    offer.setVisible(true);
                    offer.repaint();
                }
                else
                    offerIsResetWasTrade = true;  // Will show after voting
            }
            else
            {
                clearOffer(false);
            }
        }
    }

    /**
     * DOCUMENT ME!
     */
    public void rejectOffer()
    {
        offer.setMessage("No thanks.");
        offer.setVisible(true);
        //validate();
        repaint();
    }

    /**
     * DOCUMENT ME!
     */
    public void clearTradeMsg()
    {
        if ((offer.getMode() == TradeOfferPanel.MESSAGE_MODE) && ! offerIsResetMessage)
        {
            offer.setVisible(false);
            repaint();
        }
    }

    /**
     * Clear the current offer.
     * If player is client, clear the numbers in the resource "offer" squares,
     * and disable the "offer" and "clear" buttons (since no resources are selected).
     * Otherwise just hide the last-displayed offer.
     *
     * @param updateSendCheckboxes If true, and player is client, update the
     *    selection checkboxes for which opponents are sent the offer.
     *    If it's currently our turn, check all boxes where the seat isn't empty.
     *    Otherwise, check only the box for the opponent whose turn it is.
     */
    public void clearOffer(boolean updateSendCheckboxes)
    {
        if (! offerIsResetMessage)
        {
            offer.setVisible(false);
            offer.clearOffer();  // Clear to zero the offer and counter-offer
        }

        if (playerIsClient)
        {
            // clear the squares panel
            sqPanel.setValues(zero, zero);

            // reset the send squares (checkboxes)
            if (updateSendCheckboxes)
            {
                int pcurr = game.getCurrentPlayerNumber();  // current player number
                boolean pIsCurr = (pcurr == player.getPlayerNumber());  // are we current? 
                for (int i = 0; i < 3; i++)
                {
                    boolean canSend;
                    if (pIsCurr)
                        // send to any occupied seat
                        canSend = ! game.isSeatVacant(playerSendMap[i]);
                    else
                        // send only to current player
                        canSend = (pcurr == playerSendMap[i]);
                    playerSend[i].setBoolValue(canSend);
                    playerSend[i].setEnabled(canSend);
                }
            }

            clearBut.disable();
            sendBut.disable();
            sendButTip.setTip(SENDBUTTIP_DIS);
        }
        validate();
        repaint();
    }

    /**
     * Show or hide a message related to board-reset voting.
     *
     * @param message Message to show, or null to hide
     */
    public void resetBoardSetMessage(String message)
    {
        if (message != null)
        {
            offerIsResetWasTrade = (offer.isVisible() && (offer.getMode() == TradeOfferPanel.OFFER_MODE));
            offerIsResetMessage = true;
            offer.setMessage(message);
            offer.setVisible(true);
            repaint();
        }
        else
        {
            // restore previous state of offer panel
            offerIsResetMessage = false;
            if ((! offerIsResetWasTrade) || (! inPlay))
                clearTradeMsg();
            else
                updateCurrentOffer();
        }
    }
    
    /**
     * update the takeover button so that it only
     * allows takover when it's not the robot's turn
     */
    public void updateTakeOverButton()
    {
        if ((!game.isSeatLocked(player.getPlayerNumber())) &&
            (game.getCurrentPlayerNumber() != player.getPlayerNumber()))
        {
            takeOverBut.setLabel(TAKEOVER);
        }
        else
        {
            takeOverBut.setLabel("* Seat Locked *");
        }
    }

    /** Client is current player, turn has just begun.
     * Enable any previously disabled buttons.
     */
    public void updateRollButton()
    {
        rollBut.setEnabled(game.getGameState() == SOCGame.PLAY);
    }

    /**
     * update the seat lock button so that it
     * allows a player to lock an unlocked seat
     * and vice versa. Called from client when server
     * sends a SETSEATLOCK message. Updates both
     * buttons: The robot-seat-lock (when robot playing at
     * this position) and the robot-lockout (game forming,
     * seat vacant, no robot here please) buttons.
     */
    public void updateSeatLockButton()
    {
        boolean isLocked = game.isSeatLocked(player.getPlayerNumber());
        if (isLocked)
        {
            seatLockBut.setLabel(UNLOCKSEAT);
        }
        else
        {
            seatLockBut.setLabel(LOCKSEAT);
        }
        if (sitButIsLock)
        {
            boolean noPlayer = (player == null) || (player.getName() == null);
            if (isLocked)
            {
                sitBut.setLabel(UNLOCKSEAT);
                if (noPlayer)
                {
                    pname.setText(SITLOCKED);
                    pname.setVisible(true);
                }
            }
            else
            {
                sitBut.setLabel(LOCKSEAT);
                if (noPlayer)
                {
                    pname.setText(" ");
                    pname.setVisible(false);
                }
            }
            repaint();
        }
    }

    /**
     * turn the "largest army" label on or off
     *
     * @param haveIt  true if this player has the largest army
     */
    protected void setLArmy(boolean haveIt)
    {
        larmyLab.setText(haveIt ? "L. Army" : "");
    }

    /**
     * turn the "longest road" label on or off
     *
     * @param haveIt  true if this player has the longest road
     */
    protected void setLRoad(boolean haveIt)
    {
        lroadLab.setText(haveIt ? "L. Road" : "");
    }

    /**
     * update the value of a player element.
     * If VICTORYPOINTS is updated, and game state is over, check for winner
     * and update (player name label, victory-points tooltip, disable bank/trade btn)
     *
     * @param vt  the type of value
     */
    public void updateValue(int vt)
    {
        /**
         * We say that we're getting the total vp, but
         * for other players this will automatically get
         * the public vp because we will assume their
         * dev card vp total is zero.
         */
        switch (vt)
        {
        case VICTORYPOINTS:

            {
                int newVP = player.getTotalVP();
                vpSq.setIntValue(newVP);
                if (game.getGameState() == SOCGame.OVER)
                {
                    if (game.getPlayerWithWin() == player)
                    {
                        vpSq.setTooltipText("Winner with " + newVP + " victory points");
                        pname.setText(player.getName() + WINNER_SUFFIX);
                    }
                    if (interactive)
                        bankBut.setEnabled(false);
                    if (interactive)
                        playCardBut.setEnabled(false);
                    doneBut.setLabel(DONE_RESTART);
                    doneBut.setEnabled(true);  // In case it's another player's turn
                }
            }
            break;

        case LONGESTROAD:

            setLRoad(player.hasLongestRoad());

            break;

        case LARGESTARMY:

            setLArmy(player.hasLargestArmy());

            break;

        case CLAY:

            claySq.setIntValue(player.getResources().getAmount(SOCResourceConstants.CLAY));

            break;

        case ORE:

            oreSq.setIntValue(player.getResources().getAmount(SOCResourceConstants.ORE));

            break;

        case SHEEP:

            sheepSq.setIntValue(player.getResources().getAmount(SOCResourceConstants.SHEEP));

            break;

        case WHEAT:

            wheatSq.setIntValue(player.getResources().getAmount(SOCResourceConstants.WHEAT));

            break;

        case WOOD:

            woodSq.setIntValue(player.getResources().getAmount(SOCResourceConstants.WOOD));

            break;

        case NUMRESOURCES:

            resourceSq.setIntValue(player.getResources().getTotal());

            break;

        case ROADS:

            roadSq.setIntValue(player.getNumPieces(SOCPlayingPiece.ROAD));

            break;

        case SETTLEMENTS:

            settlementSq.setIntValue(player.getNumPieces(SOCPlayingPiece.SETTLEMENT));

            break;

        case CITIES:

            citySq.setIntValue(player.getNumPieces(SOCPlayingPiece.CITY));

            break;

        case NUMDEVCARDS:

            developmentSq.setIntValue(player.getDevCards().getTotal());

            break;

        case NUMKNIGHTS:

            knightsSq.setIntValue(player.getNumKnights());

            break;
        }
    }

    /**
     * Re-read player's resource info and victory points, update the display.
     */
    public void updateResourcesVP()
    {
        if (playerIsClient)
        {
            updateValue(CLAY);
            updateValue(ORE);
            updateValue(SHEEP);
            updateValue(WHEAT);
            updateValue(WOOD);
        }
        else
        {
            updateValue(NUMRESOURCES);
        }
        updateValue(VICTORYPOINTS);
    }

    /** Is this panel showing the client player,
     *  and is that player the game's current player?
     */
    public boolean isClientAndCurrentPlayer()
    {
        return (playerIsClient && playerIsCurrent);
    }

    /** Set or clear the roll prompt / auto-roll countdown display.
     *
     * @param prompt The message to display, or null to clear it.
     */
    protected void setRollPrompt(String prompt)
    {
        boolean wasUse = rollPromptInUse; 
        rollPromptInUse = (prompt != null);
        if (rollPromptInUse)
        {
            rollPromptCountdownLab.setText(prompt);
            rollPromptCountdownLab.repaint();
        }
        else if (wasUse)
        {
            rollPromptCountdownLab.setText(" ");
            rollPromptCountdownLab.repaint();
        }
    }

    /**
     * Custom layout for player hand panel.
     */
    public void doLayout()
    {
        Dimension dim = getSize();
        int inset = 8;
        int space = 2;

        FontMetrics fm = this.getFontMetrics(this.getFont());
        int lineH = ColorSquare.HEIGHT;
        int faceW = 40;
        int pnameW = dim.width - (inset + faceW + inset + inset);

        if (!inPlay)
        {
            /* just show the 'sit' button */
            /* and the 'robot' button     */
            /* and the pname label        */
            sitBut.setBounds((dim.width - 60) / 2, (dim.height - 82) / 2, 60, 40);
            pname.setBounds(inset + faceW + inset, inset, pnameW, lineH);
        }
        else
        {
            int stlmtsW = fm.stringWidth("Stlmts:_");     //Bug in stringWidth does not give correct size for ' '
            int knightsW = fm.stringWidth("Soldiers:") + 2;  // +2 because Label text does not start at pixel column 0

            faceImg.setBounds(inset, inset, faceW, faceW);
            pname.setBounds(inset + faceW + inset, inset, pnameW, lineH);

            //if (true) {
            if (playerIsClient)
            {
                /* This is our hand */
                //sqPanel.doLayout();

                Dimension sqpDim = sqPanel.getSize();
                int sheepW = fm.stringWidth("Sheep:_");           //Bug in stringWidth does not give correct size for ' '
                int pcW = fm.stringWidth(CARD.replace(' ','_'));  //Bug in stringWidth
                int giveW = fm.stringWidth(GIVE.replace(' ','_'));
                int clearW = fm.stringWidth(CLEAR.replace(' ','_'));
                int bankW = fm.stringWidth(BANK.replace(' ','_'));
                int cardsH = 5 * (lineH + space);
                int tradeH = sqpDim.height + space + (2 * (lineH + space));
                int sectionSpace = (dim.height - (inset + faceW + cardsH + tradeH + lineH + inset)) / 3;
                int tradeY = inset + faceW + sectionSpace;
                int cardsY = tradeY + tradeH + sectionSpace;

                // Always reposition everything
                startBut.setBounds(inset + faceW + inset, inset + lineH + space, dim.width - (inset + faceW + inset + inset), lineH);

                    int vpW = fm.stringWidth(vpLab.getText().replace(' ','_'));  //Bug in stringWidth
                    vpLab.setBounds(inset + faceW + inset, (inset + faceW) - lineH, vpW, lineH);
                    vpSq.setBounds(inset + faceW + inset + vpW + space, (inset + faceW) - lineH, ColorSquare.WIDTH, ColorSquare.WIDTH);

                    int topStuffW = inset + faceW + inset + vpW + space + ColorSquare.WIDTH + space;

                    // always position these: though they may not be visible
                    larmyLab.setBounds(topStuffW, (inset + faceW) - lineH, (dim.width - (topStuffW + inset + space)) / 2, lineH);
                    lroadLab.setBounds(topStuffW + ((dim.width - (topStuffW + inset + space)) / 2) + space, (inset + faceW) - lineH, (dim.width - (topStuffW + inset + space)) / 2, lineH);

                giveLab.setBounds(inset, tradeY, giveW, lineH);
                getLab.setBounds(inset, tradeY + ColorSquareLarger.HEIGHT_L, giveW, lineH);
                sqPanel.setLocation(inset + giveW + space, tradeY);

                int tbW = ((giveW + sqpDim.width) / 2);
                int tbX = inset;
                int tbY = tradeY + sqpDim.height + space;
                sendBut.setBounds(tbX, tbY, tbW, lineH);
                clearBut.setBounds(tbX, tbY + lineH + space, tbW, lineH);
                bankBut.setBounds(tbX + tbW + space, tbY + lineH + space, tbW, lineH);

                    playerSend[0].setBounds(tbX + tbW + space, tbY, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                    playerSend[1].setBounds(tbX + tbW + space + ((tbW - ColorSquare.WIDTH) / 2), tbY, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                    playerSend[2].setBounds((tbX + tbW + space + tbW) - ColorSquare.WIDTH, tbY, ColorSquare.WIDTH, ColorSquare.HEIGHT);

                knightsLab.setBounds(dim.width - inset - knightsW - ColorSquare.WIDTH - space, tradeY, knightsW, lineH);
                knightsSq.setBounds(dim.width - inset - ColorSquare.WIDTH, tradeY, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                roadLab.setBounds(dim.width - inset - knightsW - ColorSquare.WIDTH - space, tradeY + lineH + space, knightsW, lineH);
                roadSq.setBounds(dim.width - inset - ColorSquare.WIDTH, tradeY + lineH + space, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                settlementLab.setBounds(dim.width - inset - knightsW - ColorSquare.WIDTH - space, tradeY + (2 * (lineH + space)), knightsW, lineH);
                settlementSq.setBounds(dim.width - inset - ColorSquare.WIDTH, tradeY + (2 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                cityLab.setBounds(dim.width - inset - knightsW - ColorSquare.WIDTH - space, tradeY + (3 * (lineH + space)), knightsW, lineH);
                citySq.setBounds(dim.width - inset - ColorSquare.WIDTH, tradeY + (3 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);

                clayLab.setBounds(inset, cardsY, sheepW, lineH);
                claySq.setBounds(inset + sheepW + space, cardsY, ColorSquare.WIDTH, ColorSquare.HEIGHT);
                oreLab.setBounds(inset, cardsY + (lineH + space), sheepW, lineH);
                oreSq.setBounds(inset + sheepW + space, cardsY + (lineH + space), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                sheepLab.setBounds(inset, cardsY + (2 * (lineH + space)), sheepW, lineH);
                sheepSq.setBounds(inset + sheepW + space, cardsY + (2 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                wheatLab.setBounds(inset, cardsY + (3 * (lineH + space)), sheepW, lineH);
                wheatSq.setBounds(inset + sheepW + space, cardsY + (3 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                woodLab.setBounds(inset, cardsY + (4 * (lineH + space)), sheepW, lineH);
                woodSq.setBounds(inset + sheepW + space, cardsY + (4 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);

                int clW = dim.width - (inset + sheepW + space + ColorSquare.WIDTH + (4 * space) + inset);
                int clX = inset + sheepW + space + ColorSquare.WIDTH + (4 * space);
                cardList.setBounds(clX, cardsY, clW, (4 * (lineH + space)) - 2);
                playCardBut.setBounds(((clW - pcW) / 2) + clX, cardsY + (4 * (lineH + space)), pcW, lineH);

                int bbW = 50;
                // Label lines up over Roll button
                rollPromptCountdownLab.setBounds(dim.width - (bbW + space + bbW + inset), dim.height - inset - (2 * (lineH + space)), dim.width - 2*inset, lineH);
                // Bottom row of buttons
                quitBut.setBounds(inset, dim.height - lineH - inset, bbW, lineH);
                rollBut.setBounds(dim.width - (bbW + space + bbW + inset), dim.height - lineH - inset, bbW, lineH);
                doneBut.setBounds(dim.width - inset - bbW, dim.height - lineH - inset, bbW, lineH);
            }
            else
            {
                /* This is another player's hand */
                int balloonH = dim.height - (inset + (4 * (lineH + space)) + inset);
                int dcardsW = fm.stringWidth("Dev._Cards:_");                //Bug in stringWidth does not give correct size for ' '
                int vpW = fm.stringWidth(vpLab.getText().replace(' ','_'));  //Bug in stringWidth

                if (player.isRobot())
                {
                    if (game.getPlayer(client.getNickname()) == null)
                    {
                        takeOverBut.setBounds(10, (inset + balloonH) - 10, dim.width - 20, 20);
                    }
                    else if (seatLockBut.isVisible())
                    {
                        //seatLockBut.setBounds(10, inset+balloonH-10, dim.width-20, 20);
                        seatLockBut.setBounds(inset + dcardsW + space + ColorSquare.WIDTH + space, inset + balloonH + (lineH + space) + (lineH / 2), (dim.width - (2 * (inset + ColorSquare.WIDTH + (2 * space))) - stlmtsW - dcardsW), 2 * (lineH + space));
                    }
                }

                    offer.setBounds(inset, inset + faceW + space, dim.width - (2 * inset), balloonH);
                    offer.doLayout();

                vpLab.setBounds(inset + faceW + inset, (inset + faceW) - lineH, vpW, lineH);
                vpSq.setBounds(inset + faceW + inset + vpW + space, (inset + faceW) - lineH, ColorSquare.WIDTH, ColorSquare.HEIGHT);

                int topStuffW = inset + faceW + inset + vpW + space + ColorSquare.WIDTH + space;

                // always position these: though they may not be visible
                larmyLab.setBounds(topStuffW, (inset + faceW) - lineH, (dim.width - (topStuffW + inset + space)) / 2, lineH);
                lroadLab.setBounds(topStuffW + ((dim.width - (topStuffW + inset + space)) / 2) + space, (inset + faceW) - lineH, (dim.width - (topStuffW + inset + space)) / 2, lineH);

                resourceLab.setBounds(inset, inset + balloonH + (2 * (lineH + space)), dcardsW, lineH);
                resourceSq.setBounds(inset + dcardsW + space, inset + balloonH + (2 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                developmentLab.setBounds(inset, inset + balloonH + (3 * (lineH + space)), dcardsW, lineH);
                developmentSq.setBounds(inset + dcardsW + space, inset + balloonH + (3 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                knightsLab.setBounds(inset, inset + balloonH + (lineH + space), dcardsW, lineH);
                knightsSq.setBounds(inset + dcardsW + space, inset + balloonH + (lineH + space), ColorSquare.WIDTH, ColorSquare.HEIGHT);

                roadLab.setBounds(dim.width - inset - stlmtsW - ColorSquare.WIDTH - space, inset + balloonH + (lineH + space), stlmtsW, lineH);
                roadSq.setBounds(dim.width - inset - ColorSquare.WIDTH, inset + balloonH + (lineH + space), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                settlementLab.setBounds(dim.width - inset - stlmtsW - ColorSquare.WIDTH - space, inset + balloonH + (2 * (lineH + space)), stlmtsW, lineH);
                settlementSq.setBounds(dim.width - inset - ColorSquare.WIDTH, inset + balloonH + (2 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
                cityLab.setBounds(dim.width - inset - stlmtsW - ColorSquare.WIDTH - space, inset + balloonH + (3 * (lineH + space)), stlmtsW, lineH);
                citySq.setBounds(dim.width - inset - ColorSquare.WIDTH, inset + balloonH + (3 * (lineH + space)), ColorSquare.WIDTH, ColorSquare.HEIGHT);
            }
        }
    }


    /**
     * Used for countdown before auto-roll of the current player.
     * Updates on-screen countdown, fires auto-roll at 0.
     *
     * @see #SOCHandPanel.AUTOROLL_TIME
     * @see #SOCHandPanel.autoRollSetupTimer()
     *
     * @author Jeremy D Monin <jeremy@nand.net>
     */
    protected class HandPanelAutoRollTask extends java.util.TimerTask
    {
        int timeRemain;  // seconds displayed, seconds at start of "run" tick

        protected HandPanelAutoRollTask()
        {
            timeRemain = AUTOROLL_TIME;
        }

        public void run()
        {
            // for debugging
            if (Thread.currentThread().getName().startsWith("Thread-"))
            {
                try {
                    Thread.currentThread().setName("timertask-autoroll");
                }
                catch (Throwable th) {}
            }

            // autoroll function
            try
            {
                if (timeRemain > 0)
                {
                    setRollPrompt(AUTOROLL_COUNTDOWN + Integer.toString(timeRemain));
                } else {
                    clickRollButton();  // Clear prompt, click Roll
                    cancel();  // End of countdown for this timer
                }
            }
            catch (Throwable thr)
            {
                playerInterface.chatPrintStackTrace(thr);
            }
            finally
            {
                --timeRemain;  // for next tick                
            }
        }

    }  // inner class HandPanelAutoRollTask

}  // class SOCHandPanel
