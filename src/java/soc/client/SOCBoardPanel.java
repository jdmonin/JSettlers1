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

import soc.client.SOCHandPanel.HandPanelAutoRollTask;
import soc.game.SOCBoard;
import soc.game.SOCCity;
import soc.game.SOCGame;
import soc.game.SOCPlayer;
import soc.game.SOCPlayingPiece;
import soc.game.SOCRoad;
import soc.game.SOCSettlement;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;


/**
 * This is a component that can display a Settlers of Catan Board.
 * It can be used in an applet or an application.
 * It loads gifs from a directory named "images" in the same
 * directory at the code.
 */
public class SOCBoardPanel extends Canvas implements MouseListener, MouseMotionListener
{
    private static String IMAGEDIR = "/soc/client/images";

    /**
     * size of the whole panel
     */
    public static final int panelx = 379;
    public static final int panely = 340;
    
    private static final int deltaY = 46;     //How many pixels to drop for each row of hexes
    private static final int deltaX = 54;     //How many pixels to move over for a new hex
    private static final int halfdeltaX = 27; //Each row only moves a half hex over horizontally

    /**
     * hex coordinates for drawing
     */
    private static final int[] hexX = 
    {
        deltaX + halfdeltaX, 2 * deltaX + halfdeltaX, 3 * deltaX + halfdeltaX, 4 * deltaX + halfdeltaX,  // row 1 4 hexes
        deltaX, 2 * deltaX, 3 * deltaX, 4 * deltaX, 5 * deltaX,                                          // row 2 5 hexes
        halfdeltaX, deltaX + halfdeltaX, 2 * deltaX + halfdeltaX, 3 * deltaX + halfdeltaX, 4 * deltaX + halfdeltaX, 5 * deltaX + halfdeltaX,  // row 3 6 hexes
        0, deltaX, 2 * deltaX, 3 * deltaX, 4 * deltaX, 5 * deltaX, 6 * deltaX,                           // row 4 7 hexes
        halfdeltaX, deltaX + halfdeltaX, 2 * deltaX + halfdeltaX, 3 * deltaX + halfdeltaX, 4 * deltaX + halfdeltaX, 5 * deltaX + halfdeltaX,  // row 5 6 hexes
        deltaX, 2 * deltaX, 3 * deltaX, 4 * deltaX, 5 * deltaX,                                          // row 6 5 hexes
        deltaX + halfdeltaX, 2 * deltaX + halfdeltaX, 3 * deltaX + halfdeltaX, 4 * deltaX + halfdeltaX   // row 7 4 hexes
    };
    private static final int[] hexY = 
    {
        0, 0, 0, 0, 
        deltaY, deltaY, deltaY, deltaY, deltaY, 
        2 * deltaY, 2 * deltaY, 2 * deltaY, 2 * deltaY, 2 * deltaY, 2 * deltaY, 
        3 * deltaY, 3 * deltaY, 3 * deltaY, 3 * deltaY, 3 * deltaY, 3 * deltaY, 3 * deltaY,
        4 * deltaY, 4 * deltaY, 4 * deltaY, 4 * deltaY, 4 * deltaY, 4 * deltaY, 
        5 * deltaY, 5 * deltaY, 5 * deltaY, 5 * deltaY, 5 * deltaY,
        6 * deltaY, 6 * deltaY, 6 * deltaY, 6 * deltaY
    };

    /**
     * coordinates for drawing the playing pieces
     */
    /***  road looks like "|"  ***/
    private static final int[] vertRoadX = { -2, 3, 3, -2, -2 };
    private static final int[] vertRoadY = { 17, 17, 47, 47, 17 };

    /***  road looks like "/"  ***/
    private static final int[] upRoadX = { -1, 26, 29, 2, -1 };
    private static final int[] upRoadY = { 15, -2, 2, 19, 15 };

    /***  road looks like "\"  ***/
    private static final int[] downRoadX = { -1, 2, 29, 26, -1 };
    private static final int[] downRoadY = { 49, 45, 62, 66, 49 };

    /***  settlement  ***/
    private static final int[] settlementX = { -7, 0, 7, 7, -7, -7, 7 };
    private static final int[] settlementY = { -7, -15, -7, 5, 5, -7, -7 };

    /***  city  ***/
    private static final int[] cityX = 
    {
        -10, -4, 2, 2, 10, 10, -10, -10, 0, 0, 10, 5, -10
    };
    private static final int[] cityY = 
    {
        -8, -14, -8, -4, -4, 6, 6, -8, -8, -4, -4, -8, -8
    };

    /***  robber  ***/
    private static final int[] robberX = 
    {
        6, 4, 4, 6, 10, 12, 12, 10, 12, 12, 4, 4, 6, 10
    };
    private static final int[] robberY = 
    {
        6, 4, 2, 0, 0, 2, 4, 6, 8, 16, 16, 8, 6, 6
    };
    public final static int NONE = 0;
    public final static int PLACE_ROAD = 1;
    public final static int PLACE_SETTLEMENT = 2;
    public final static int PLACE_CITY = 3;
    public final static int PLACE_ROBBER = 4;
    public final static int PLACE_INIT_SETTLEMENT = 5;
    public final static int PLACE_INIT_ROAD = 6;
    public final static int CONSIDER_LM_SETTLEMENT = 7;
    public final static int CONSIDER_LM_ROAD = 8;
    public final static int CONSIDER_LM_CITY = 9;
    public final static int CONSIDER_LT_SETTLEMENT = 10;
    public final static int CONSIDER_LT_ROAD = 11;
    public final static int CONSIDER_LT_CITY = 12;
    public final static int GAME_FORMING = 98;
    public final static int GAME_OVER = 99;
    
    /** During initial-piece placement, the tooltip is moved this far over to make room. */
    public final static int HOVER_OFFSET_X_FOR_INIT_PLACE = 9;
    
    /** During robber placement, the tooltip is moved this far over to make room. */
    public final static int HOVER_OFFSET_X_FOR_ROBBER = 15;
    
    /** for popup-menu build request, network send maximum delay (seconds) */
    protected static int BUILD_REQUEST_MAX_DELAY_SEC = 5;
    
    /** for popup-menu build request, length of time after popup to ignore further
     *  mouse-clicks.  Avoids Windows accidental build by popup-click during game's
     *  initial piece placement. 
     */ 
    protected static int POPUP_MENU_IGNORE_MS = 150;

    /**
     * hex size
     */
    private int HEXWIDTH = 55;
    private int HEXHEIGHT = 64;

    /**
     * translate hex ID to number to get coords
     */
    private int[] hexIDtoNum;

    /**
     * Hex pix
     */
    private static Image[] hexes;
    private static Image[] ports;

    /**
     * number pix
     */
    private static Image[] numbers;

    /**
     * arrow/dice pix
     */
    private static Image arrowR;
    private static Image arrowL;
    private static Image[] dice;

    /**
     * Old pointer coords for interface
     */
    private int ptrOldX;
    private int ptrOldY;
    
    /**
     * (tooltip) Hover text.  Its mode uses boardpanel mode
     * constants: Will be NONE, PLACE_ROAD, PLACE_SETTLEMENT,
     *   PLACE_ROBBER for hex, or PLACE_INIT_SETTLEMENT for port.
     */
    private BoardToolTip hoverTip;
    
    /**
     * Context menu for build/cancel-build
     */
    private BoardPopupMenu popupMenu;
        
    /**
     * Tracks last popup time.  Avoids misinterpretation of popup-click with placement-click
     * during initial placement: On Windows, popup-click must be caught in mouseReleased,
     * but mousePressed is called immediately afterwards.    
     */
    private long popupMenuSystime;

    protected Timer buildReqTimer;  // Created just once
    protected BoardPanelSendBuildTask buildReqTimerTask;  // Created whenever right-click build request sent

    /**
     * Edge or node being pointed to.
     */
    private int hilight;

    /**
     * Map grid sectors to hex edges
     */
    private int[] edgeMap;

    /**
     * Map grid sectors to hex nodes
     */
    private int[] nodeMap;

    /**
     * Map grid sectors to hexes
     */
    private int[] hexMap;

    /**
     * The game which this board is a part of
     */
    private SOCGame game;

    /**
     * The board in the game
     */
    private SOCBoard board;

    /**
     * The player that is using this interface
     */
    private SOCPlayer player;
    
    /**
     * player number if in a game, or -1.
     */
    private int playerNumber;

    /**
     * When in "consider" mode, this is the player
     * we're talking to
     */
    private SOCPlayer otherPlayer;

    /**
     * offscreen buffer
     */
    private Image buffer;

    /**
     * modes of interaction; for correlation to game state, see {@see #updateMode()}.
     */
    private int mode;

    /**
     * This holds the coord of the last stlmt
     * placed in the initial phase.
     */
    private int initstlmt;

    /**
     * the player interface that this board is a part of
     */
    private SOCPlayerInterface playerInterface;

    /** Cached colors, for use for robber's "ghost"
     *  (previous position) when moving the robber.
     *  Values are determined the first time the
     *  robber is ghosted on that type of tile.
     *  
     *  Index ranges from 0 to SOCBoard.MAX_ROBBER_HEX.
     *  
     *  @see soc.client.ColorSquare
     *  @see #drawRobber(Graphics, int, boolean)
     */
    protected Color[] robberGhostFill, robberGhostOutline;

    /**
     * create a new board panel in an applet
     *
     * @param pi  the player interface that spawned us
     */
    public SOCBoardPanel(SOCPlayerInterface pi)
    {
        super();

        game = pi.getGame();
        playerInterface = pi;
        player = null;
        playerNumber = -1;
        board = game.getBoard();

        int i;

        // init coord holders
        ptrOldX = 0;
        ptrOldY = 0;

        hilight = 0;

        // init edge map
        edgeMap = new int[345];

        for (i = 0; i < 345; i++)
        {
            edgeMap[i] = 0;
        }

        initEdgeMapAux(4, 3, 9, 6, 0x37);
        initEdgeMapAux(3, 6, 10, 9, 0x35);
        initEdgeMapAux(2, 9, 11, 12, 0x33);
        initEdgeMapAux(3, 12, 10, 15, 0x53);
        initEdgeMapAux(4, 15, 9, 18, 0x73);

        // init node map
        nodeMap = new int[345];

        for (i = 0; i < 345; i++)
        {
            nodeMap[i] = 0;
        }

        initNodeMapAux(4, 3, 10, 7, 0x37);
        initNodeMapAux(3, 6, 11, 10, 0x35);
        initNodeMapAux(2, 9, 12, 13, 0x33);
        initNodeMapAux(3, 12, 11, 16, 0x53);
        initNodeMapAux(4, 15, 10, 19, 0x73);

        // init hex map
        hexMap = new int[345];

        for (i = 0; i < 345; i++)
        {
            hexMap[i] = 0;
        }

        initHexMapAux(4, 4, 9, 5, 0x37);
        initHexMapAux(3, 7, 10, 8, 0x35);
        initHexMapAux(2, 10, 11, 11, 0x33);
        initHexMapAux(3, 13, 10, 14, 0x53);
        initHexMapAux(4, 16, 9, 17, 0x73);

        hexIDtoNum = new int[0xDE];

        for (i = 0; i < 0xDE; i++)
        {
            hexIDtoNum[i] = 0;
        }

        initHexIDtoNumAux(0x17, 0x7D, 0);
        initHexIDtoNumAux(0x15, 0x9D, 4);
        initHexIDtoNumAux(0x13, 0xBD, 9);
        initHexIDtoNumAux(0x11, 0xDD, 15);
        initHexIDtoNumAux(0x31, 0xDB, 22);
        initHexIDtoNumAux(0x51, 0xD9, 28);
        initHexIDtoNumAux(0x71, 0xD7, 33);

        // set mode of interaction
        mode = NONE;

        // Set up mouse listeners
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        
        // Cached colors to be determined later
        robberGhostFill = new Color [1 + SOCBoard.MAX_ROBBER_HEX];
        robberGhostOutline = new Color [1 + SOCBoard.MAX_ROBBER_HEX];
        
        // Set up hover tooltip info
        hoverTip = new BoardToolTip(this);
        
        // Set up popup menu
        popupMenu = new BoardPopupMenu(this);
        add (popupMenu);
        popupMenuSystime = System.currentTimeMillis();  // Set to a reasonable value 

        // load the static images
        loadImages(this);
    }

    private final void initEdgeMapAux(int x1, int y1, int x2, int y2, int startHex)
    {
        int x;
        int y;
        int facing = 0;
        int count = 0;
        int hexNum;
        int edgeNum = 0;

        for (y = y1; y <= y2; y++)
        {
            hexNum = startHex;

            switch (count)
            {
            case 0:
                facing = 6;
                edgeNum = hexNum - 0x10;

                break;

            case 1:
                facing = 5;
                edgeNum = hexNum - 0x11;

                break;

            case 2:
                facing = 5;
                edgeNum = hexNum - 0x11;

                break;

            case 3:
                facing = 4;
                edgeNum = hexNum - 0x01;

                break;

            default:
                System.out.println("initEdgeMap error");

                return;
            }

            for (x = x1; x <= x2; x++)
            {
                edgeMap[x + (y * 15)] = edgeNum;

                switch (facing)
                {
                case 1:
                    facing = 6;
                    hexNum += 0x22;
                    edgeNum = hexNum - 0x10;

                    break;

                case 2:
                    facing = 5;
                    hexNum += 0x22;
                    edgeNum = hexNum - 0x11;

                    break;

                case 3:
                    facing = 4;
                    hexNum += 0x22;
                    edgeNum = hexNum - 0x01;

                    break;

                case 4:
                    facing = 3;
                    edgeNum = hexNum + 0x10;

                    break;

                case 5:
                    facing = 2;
                    edgeNum = hexNum + 0x11;

                    break;

                case 6:
                    facing = 1;
                    edgeNum = hexNum + 0x01;

                    break;

                default:
                    System.out.println("initEdgeMap error");

                    return;
                }
            }

            count++;
        }
    }

    private final void initHexMapAux(int x1, int y1, int x2, int y2, int startHex)
    {
        int x;
        int y;
        int hexNum;
        int count = 0;

        for (y = y1; y <= y2; y++)
        {
            hexNum = startHex;

            for (x = x1; x <= x2; x++)
            {
                hexMap[x + (y * 15)] = hexNum;

                if ((count % 2) != 0)
                {
                    hexNum += 0x22;
                }

                count++;
            }
        }
    }

    private final void initNodeMapAux(int x1, int y1, int x2, int y2, int startHex)
    {
        int x;
        int y;
        int facing = 0;
        int count = 0;
        int hexNum;
        int edgeNum = 0;

        for (y = y1; y <= y2; y++)
        {
            hexNum = startHex;

            switch (count)
            {
            case 0:
                facing = -1;
                edgeNum = 0;

                break;

            case 1:
                facing = 6;
                edgeNum = hexNum - 0x10;

                break;

            case 2:
                facing = -7;
                edgeNum = 0;

                break;

            case 3:
                facing = 5;
                edgeNum = hexNum - 0x01;

                break;

            case 4:
                facing = -4;
                edgeNum = 0;

                break;

            default:
                System.out.println("initNodeMap error");

                return;
            }

            for (x = x1; x <= x2; x++)
            {
                nodeMap[x + (y * 15)] = edgeNum;

                switch (facing)
                {
                case 1:
                    facing = -1;
                    hexNum += 0x22;
                    edgeNum = 0;

                    break;

                case -1:
                    facing = 1;
                    edgeNum = hexNum + 0x01;

                    break;

                case 2:
                    facing = -2;
                    hexNum += 0x22;
                    edgeNum = 0;

                    break;

                case -2:
                    facing = 2;
                    edgeNum = hexNum + 0x12;

                    break;

                case 6:
                    facing = -2;
                    edgeNum = 0;

                    break;

                case -7:
                    edgeNum = 0;

                    break;

                case 5:
                    facing = -3;
                    edgeNum = 0;

                    break;

                case 3:
                    facing = -3;
                    hexNum += 0x22;
                    edgeNum = 0;

                    break;

                case -3:
                    facing = 3;
                    edgeNum = hexNum + 0x21;

                    break;

                case 4:
                    facing = -4;
                    hexNum += 0x22;
                    edgeNum = 0;

                    break;

                case -4:
                    facing = 4;
                    edgeNum = hexNum + 0x10;

                    break;

                default:
                    System.out.println("initNodeMap error");

                    return;
                }
            }

            count++;
        }
    }

    private final void initHexIDtoNumAux(int begin, int end, int num)
    {
        int i;

        for (i = begin; i <= end; i += 0x22)
        {
            hexIDtoNum[i] = num;
            num++;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Dimension getPreferredSize()
    {
        return new Dimension(panelx, panely);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Dimension getMinimumSize()
    {
        return new Dimension(panelx, panely);
    }

    /**
     * Redraw the board using double buffering. Don't call this directly, use
     * {@link Component#repaint()} instead.
     */
    public void paint(Graphics g)
    {
        try {
        if (buffer == null)
        {
            buffer = this.createImage(panelx, panely);
        }
        drawBoard(buffer.getGraphics());
        if (hoverTip.isVisible())
            hoverTip.paint(buffer.getGraphics());
        buffer.flush();
        g.drawImage(buffer, 0, 0, this);

        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }

    /**
     * Overriden so the peer isn't painted, which clears background. Don't call
     * this directly, use {@link Component#repaint()} instead.
     */
    public void update(Graphics g)
    {
        paint(g);
    }

    /**
     * draw a board tile
     */
    private final void drawHex(Graphics g, int hexNum)
    {
        int tmp;
        int[] hexLayout = board.getHexLayout();
        int[] numberLayout = board.getNumberLayout();
        int hexType = hexLayout[hexNum];

        tmp = hexType & 15; // get only the last 4 bits;
        g.drawImage(hexes[tmp], hexX[hexNum], hexY[hexNum], this);

        tmp = hexType >> 4; // get the facing of the port

        if (tmp > 0)
        {
            g.drawImage(ports[tmp], hexX[hexNum], hexY[hexNum], this);
        }

        if (numberLayout[hexNum] >= 0)
        {
            g.drawImage(numbers[numberLayout[hexNum]], hexX[hexNum] + 17, hexY[hexNum] + 22, this);
        }
    }

    /**
     * draw the robber
     * 
     * @param g       Graphics context
     * @param hexID   Board hex encoded position
     * @param fullNotGhost  Draw normally, not "ghost" of previous position
     *                (as during PLACE_ROBBER movement)
     */
    private final void drawRobber(Graphics g, int hexID, boolean fullNotGhost)
    {
        int[] tmpX = new int[14];
        int[] tmpY = new int[14];
        int hexNum = hexIDtoNum[hexID];

        for (int i = 0; i < 14; i++)
        {
            tmpX[i] = robberX[i] + hexX[hexNum] + 19;
            tmpY[i] = robberY[i] + hexY[hexNum] + 23;
        }
        
        Color rFill, rOutline;
        if (fullNotGhost)
        {
            rFill = Color.lightGray;
            rOutline = Color.black;
        } else {
            // Determine "ghost" color, we're moving the robber
            int hexType = board.getHexLayout()[hexNum];
            if (hexType >= robberGhostFill.length)
            {
                // should not happen
                rFill = Color.lightGray;
                rOutline = Color.black;
            } else if (robberGhostFill[hexType] != null)
            {
                // was cached from previous calculation
                rFill = robberGhostFill[hexType];
                rOutline = robberGhostOutline[hexType];
            } else {
                // find basic color, "ghost" it
                switch (hexType)
                {
                case SOCBoard.DESERT_HEX:
                    rOutline = ColorSquare.DESERT;
                    break;
                case SOCBoard.CLAY_HEX:
                    rOutline = ColorSquare.CLAY;
                    break;
                case SOCBoard.ORE_HEX:
                    rOutline = ColorSquare.ORE;                
                    break;
                case SOCBoard.SHEEP_HEX:
                    rOutline = ColorSquare.SHEEP;
                    break;
                case SOCBoard.WHEAT_HEX:
                    rOutline = ColorSquare.WHEAT;
                    break;
                case SOCBoard.WOOD_HEX:
                    rOutline = ColorSquare.WOOD;
                    break;
                default:
                    // Should not happen
                    rOutline = Color.lightGray;
                }

                // If hex is light, robber fill color should be dark. (average with gray)
                // If hex is dark or midtone, it should be light. (average with white)
                rFill = SOCPlayerInterface.makeGhostColor(rOutline);
                rOutline = rOutline.darker();  // Always darken the outline

                // Remember for next time
                robberGhostFill[hexType] = rFill;
                robberGhostOutline[hexType] = rOutline;
                
            }  // cached ghost color?
        }  // normal or ghost?

        g.setColor(rFill);
        g.fillPolygon(tmpX, tmpY, 13);
        g.setColor(rOutline);
        g.drawPolygon(tmpX, tmpY, 14);
    }

    /**
     * draw a road
     */
    private final void drawRoad(Graphics g, int edgeNum, int pn, boolean isHilight)
    {
        // Draw a road
        int i;
        int[] tmpX = new int[5];
        int[] tmpY = new int[5];
        int hexNum;

        if ((((edgeNum & 0x0F) + (edgeNum >> 4)) % 2) == 0)
        { // If first and second digit 
            hexNum = hexIDtoNum[edgeNum + 0x11]; // are even, then it is '|'.

            for (i = 0; i < 5; i++)
            {
                tmpX[i] = vertRoadX[i] + hexX[hexNum];
                tmpY[i] = vertRoadY[i] + hexY[hexNum];
            }
        }
        else if (((edgeNum >> 4) % 2) == 0)
        { // If first digit is even,
            hexNum = hexIDtoNum[edgeNum + 0x10]; // then it is '/'.
            hexNum = hexIDtoNum[edgeNum + 0x10];

            for (i = 0; i < 5; i++)
            {
                tmpX[i] = upRoadX[i] + hexX[hexNum];
                tmpY[i] = upRoadY[i] + hexY[hexNum];
            }
        }
        else
        { // Otherwise it is '\'.
            hexNum = hexIDtoNum[edgeNum + 0x01];

            for (i = 0; i < 5; i++)
            {
                tmpX[i] = downRoadX[i] + hexX[hexNum];
                tmpY[i] = downRoadY[i] + hexY[hexNum];
            }
        }

        if (isHilight)
            g.setColor(playerInterface.getPlayerColor(pn, true));
        else
            g.setColor(playerInterface.getPlayerColor(pn));

        g.fillPolygon(tmpX, tmpY, 5);
        if (isHilight)
            g.setColor(playerInterface.getPlayerColor(pn, false));
        else
            g.setColor(Color.black);
        g.drawPolygon(tmpX, tmpY, 5);
    }

    /**
     * draw a settlement
     */
    private final void drawSettlement(Graphics g, int nodeNum, int pn, boolean isHilight)
    {
        int i;
        int[] tmpX = new int[7];
        int[] tmpY = new int[7];
        int hexNum;

        if (((nodeNum >> 4) % 2) == 0)
        { // If first digit is even,
            hexNum = hexIDtoNum[nodeNum + 0x10]; // then it is a 'Y' node

            for (i = 0; i < 7; i++)
            {
                tmpX[i] = settlementX[i] + hexX[hexNum];
                tmpY[i] = settlementY[i] + hexY[hexNum] + 17;
            }
        }
        else
        { // otherwise it is an 'A' node
            hexNum = hexIDtoNum[nodeNum - 0x01];

            for (i = 0; i < 7; i++)
            {
                tmpX[i] = settlementX[i] + hexX[hexNum] + 27;
                tmpY[i] = settlementY[i] + hexY[hexNum] + 2;
            }
        }

        // System.out.println("NODEID = "+Integer.toHexString(nodeNum)+" | HEXNUM = "+hexNum);
        if (isHilight)
            g.setColor(playerInterface.getPlayerColor(pn, true));
        else
            g.setColor(playerInterface.getPlayerColor(pn));
        g.fillPolygon(tmpX, tmpY, 6);
        if (isHilight)
            g.setColor(playerInterface.getPlayerColor(pn, false));
        else
            g.setColor(Color.black);
        g.drawPolygon(tmpX, tmpY, 7);
    }

    /**
     * draw a city
     */
    private final void drawCity(Graphics g, int nodeNum, int pn, boolean isHilight)
    {
        int i;
        int[] tmpX = new int[13];
        int[] tmpY = new int[13];
        int hexNum;

        if (((nodeNum >> 4) % 2) == 0)
        { // If first digit is even,
            hexNum = hexIDtoNum[nodeNum + 0x10]; // then it is a 'Y' node

            for (i = 0; i < 13; i++)
            {
                tmpX[i] = cityX[i] + hexX[hexNum];
                tmpY[i] = cityY[i] + hexY[hexNum] + 17;
            }
        }
        else
        { // otherwise it is an 'A' node
            hexNum = hexIDtoNum[nodeNum - 0x01];

            for (i = 0; i < 13; i++)
            {
                tmpX[i] = cityX[i] + hexX[hexNum] + 27;
                tmpY[i] = cityY[i] + hexY[hexNum] + 2;
            }
        }

        if (isHilight)
        {
            g.setColor(playerInterface.getPlayerColor(pn, true));
            g.drawPolygon(tmpX, tmpY, 8);
            // Draw again, slightly offset, for "ghost", since we can't fill and
            // cover up the underlying settlement.
            for (i = 0; i < 13; i++)
            {
                ++tmpX[i];
                ++tmpY[i];
            }
            g.drawPolygon(tmpX, tmpY, 8);
            return;
        }
        
        g.setColor(playerInterface.getPlayerColor(pn));

        g.fillPolygon(tmpX, tmpY, 8);
        g.setColor(Color.black);
        g.drawPolygon(tmpX, tmpY, 8);
    }

    /**
     * draw the arrow that shows whose turn it is
     */
    private final void drawArrow(Graphics g, int pnum, int diceResult)
    {
        switch (pnum)
        {
        case 0:

            // top left
            g.drawImage(arrowL, 3, 5, this);

            if ((diceResult >= 2) && (game.getGameState() != SOCGame.PLAY))
            {
                g.drawImage(dice[diceResult], 13, 10, this);
            }

            break;

        case 1:

            // top right
            g.drawImage(arrowR, 339, 5, this);

            if ((diceResult >= 2) && (game.getGameState() != SOCGame.PLAY))
            {
                g.drawImage(dice[diceResult], 339, 10, this);
            }

            break;

        case 2:

            // bottom right
            g.drawImage(arrowR, 339, 298, this);

            if ((diceResult >= 2) && (game.getGameState() != SOCGame.PLAY))
            {
                g.drawImage(dice[diceResult], 339, 303, this);
            }

            break;

        case 3:

            // bottom left
            g.drawImage(arrowL, 3, 298, this);

            if ((diceResult >= 2) && (game.getGameState() != SOCGame.PLAY))
            {
                g.drawImage(dice[diceResult], 13, 303, this);
            }

            break;
        }
    }

    /**
     * draw the whole board
     */
    private void drawBoard(Graphics g)
    {
        g.setPaintMode();

        g.setColor(getBackground());
        g.fillRect(0, 0, panelx, panely);

        for (int i = 0; i < 37; i++)
        {
            drawHex(g, i);
        }

        if (board.getRobberHex() != -1)
        {
            drawRobber(g, board.getRobberHex(), (mode != PLACE_ROBBER));
        }

        int pn;
        int idx;
        int max;

        int gameState = game.getGameState();

        if (gameState != SOCGame.NEW)
        {
            drawArrow(g, game.getCurrentPlayerNumber(), game.getCurrentDice());
        }

        /**
         * draw the roads
         */
        Enumeration roads = board.getRoads().elements();

        while (roads.hasMoreElements())
        {
            SOCRoad r = (SOCRoad) roads.nextElement();
            drawRoad(g, r.getCoordinates(), r.getPlayer().getPlayerNumber(), false);
        }

        /**
         * draw the settlements
         */
        Enumeration settlements = board.getSettlements().elements();

        while (settlements.hasMoreElements())
        {
            SOCSettlement s = (SOCSettlement) settlements.nextElement();
            drawSettlement(g, s.getCoordinates(), s.getPlayer().getPlayerNumber(), false);
        }

        /**
         * draw the cities
         */
        Enumeration cities = board.getCities().elements();

        while (cities.hasMoreElements())
        {
            SOCCity c = (SOCCity) cities.nextElement();
            drawCity(g, c.getCoordinates(), c.getPlayer().getPlayerNumber(), false);
        }

        /**
         * Draw the hilight when in interactive mode
         */
        switch (mode)
        {
        case PLACE_ROAD:
        case PLACE_INIT_ROAD:

            if (hilight > 0)
            {
                drawRoad(g, hilight, player.getPlayerNumber(), true);
            }

            break;

        case PLACE_SETTLEMENT:
        case PLACE_INIT_SETTLEMENT:

            if (hilight > 0)
            {
                drawSettlement(g, hilight, player.getPlayerNumber(), true);
            }

            break;

        case PLACE_CITY:

            if (hilight > 0)
            {
                drawCity(g, hilight, player.getPlayerNumber(), true);
            }

            break;

        case CONSIDER_LM_SETTLEMENT:
        case CONSIDER_LT_SETTLEMENT:

            if (hilight > 0)
            {
                drawSettlement(g, hilight, otherPlayer.getPlayerNumber(), true);
            }

            break;

        case CONSIDER_LM_ROAD:
        case CONSIDER_LT_ROAD:

            if (hilight > 0)
            {
                drawRoad(g, hilight, otherPlayer.getPlayerNumber(), false);
            }

            break;

        case CONSIDER_LM_CITY:
        case CONSIDER_LT_CITY:

            if (hilight > 0)
            {
                drawCity(g, hilight, otherPlayer.getPlayerNumber(), true);
            }

            break;

        case PLACE_ROBBER:

            if (hilight > 0)
            {
                drawRobber(g, hilight, true);
            }

            break;
        }
    }

    /**
     * update the type of interaction mode
     */
    public void updateMode()
    {
        if (player != null)
        {
            if (game.getCurrentPlayerNumber() == player.getPlayerNumber())
            {
                switch (game.getGameState())
                {
                case SOCGame.START1A:
                case SOCGame.START2A:
                    mode = PLACE_INIT_SETTLEMENT;

                    break;

                case SOCGame.START1B:
                case SOCGame.START2B:
                    mode = PLACE_INIT_ROAD;

                    break;

                case SOCGame.PLACING_ROAD:
                case SOCGame.PLACING_FREE_ROAD1:
                case SOCGame.PLACING_FREE_ROAD2:
                    mode = PLACE_ROAD;

                    break;

                case SOCGame.PLACING_SETTLEMENT:
                    mode = PLACE_SETTLEMENT;

                    break;

                case SOCGame.PLACING_CITY:
                    mode = PLACE_CITY;

                    break;

                case SOCGame.PLACING_ROBBER:
                    mode = PLACE_ROBBER;
                    
                    break;
                    
                case SOCGame.NEW:
                case SOCGame.READY:
                    mode = GAME_FORMING;
                    
                    break;
                    
                case SOCGame.OVER:
                    mode = GAME_OVER;

                    break;

                default:
                    mode = NONE;

                    break;
                }
            }
            else
            {
                mode = NONE;
            }
        }
        else
        {
            mode = NONE;
        }
                
        updateHoverTipToMode();
    }
    
    protected void updateHoverTipToMode()
    {
        if ((mode == NONE) || (mode == GAME_OVER))            
            hoverTip.setOffsetX(0);
        else if (mode == PLACE_ROBBER)
            hoverTip.setOffsetX(HOVER_OFFSET_X_FOR_ROBBER);
        else if ((mode == PLACE_INIT_SETTLEMENT) || (mode == PLACE_INIT_ROAD))
            hoverTip.setOffsetX(HOVER_OFFSET_X_FOR_INIT_PLACE);
        else
            hoverTip.setHoverText(null);
    }
    
    protected void clearModeAndHilight()
    {
        mode = NONE;
        hilight = 0;
        updateHoverTipToMode();
    }

    /**
     * set the player that is using this board panel.
     */
    public void setPlayer()
    {
        player = game.getPlayer(playerInterface.getClient().getNickname());
        playerNumber = player.getPlayerNumber();
    }

    /**
     * set the other player
     *
     * @param op  the other player
     */
    public void setOtherPlayer(SOCPlayer op)
    {
        otherPlayer = op;
    }

    /*********************************
     * Handle Events
     *********************************/
    public void mouseEntered(MouseEvent e)
    {
        ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mousePressed(MouseEvent e)
    {
        ;  // JM: was mouseClicked (TODO check e.isPopupTrigger)
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseReleased(MouseEvent e)
    {
        // Needed in Windows for popup-menu handling
        if (e.isPopupTrigger())
        {
            popupMenuSystime = e.getWhen();
            e.consume();
            doBoardMenuPopup(e.getX(), e.getY());
            return;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseDragged(MouseEvent e)
    {
        ;
    }

    /**
     * Mouse has left the panel; hide tooltip and any hovering piece.
     *
     * @param e MouseEvent
     */
    public void mouseExited(MouseEvent e)
    {
        boolean wantsRepaint = false;
        if (hoverTip.isVisible())
        {
            hoverTip.hideHoverAndPieces();
            wantsRepaint = true;
        }
        if (mode != NONE)
        {
            hilight = 0;
            wantsRepaint = true;
        }
        if (wantsRepaint)
            repaint();
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseMoved(MouseEvent e)
    {
        try {
        int x = e.getX();
        int y = e.getY();

        int edgeNum;
        int nodeNum;
        int hexNum;

        switch (mode)
        {
        case PLACE_INIT_ROAD:

            /**** Code for finding an edge ********/
            edgeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                edgeNum = findEdge(x, y);

                // Figure out if this is a legal road
                // It must be attached to the last stlmt
                if (!((player.isPotentialRoad(edgeNum)) && ((edgeNum == initstlmt) || (edgeNum == (initstlmt - 0x11)) || (edgeNum == (initstlmt - 0x01)) || (edgeNum == (initstlmt - 0x10)))))
                {
                    edgeNum = 0;
                }

                if (hilight != edgeNum)
                {
                    hilight = edgeNum;
                    repaint();
                }
            }

            break;

        case PLACE_ROAD:

            /**** Code for finding an edge ********/
            edgeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                edgeNum = findEdge(x, y);

                if (!player.isPotentialRoad(edgeNum))
                {
                    edgeNum = 0;
                }

                if (hilight != edgeNum)
                {
                    hilight = edgeNum;
                    repaint();
                }
            }

            break;

        case PLACE_SETTLEMENT:
        case PLACE_INIT_SETTLEMENT:

            /**** Code for finding a node *********/
            nodeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                nodeNum = findNode(x, y);

                if (!player.isPotentialSettlement(nodeNum))
                {
                    nodeNum = 0;
                }

                if (hilight != nodeNum)
                {
                    hilight = nodeNum;
                    if (mode == PLACE_INIT_SETTLEMENT)
                        hoverTip.handleHover(x,y);
                    repaint();
                }
                else if (mode == PLACE_INIT_SETTLEMENT)
                {
                    hoverTip.handleHover(x,y);  // Will call repaint() if needed
                }
            }

            break;

        case PLACE_CITY:

            /**** Code for finding a node *********/
            nodeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                nodeNum = findNode(x, y);

                if (!player.isPotentialCity(nodeNum))
                {
                    nodeNum = 0;
                }

                if (hilight != nodeNum)
                {
                    hilight = nodeNum;
                    repaint();
                }
            }

            break;

        case PLACE_ROBBER:

            /**** Code for finding a hex *********/
            hexNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                hexNum = findHex(x, y);

                if (hexNum == board.getRobberHex())
                {
                    hexNum = 0;
                }

                if (hilight != hexNum)
                {
                    hilight = hexNum;
                    hoverTip.handleHover(x,y);
                    repaint();
                }
                else
                {
                    hoverTip.positionToMouse(x,y); // calls repaint
                }
            }

            break;

        case CONSIDER_LM_SETTLEMENT:
        case CONSIDER_LT_SETTLEMENT:

            /**** Code for finding a node *********/
            nodeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                nodeNum = findNode(x, y);

                //if (!otherPlayer.isPotentialSettlement(nodeNum))
                //  nodeNum = 0;
                if (hilight != nodeNum)
                {
                    hilight = nodeNum;
                    repaint();
                }
            }

            break;

        case CONSIDER_LM_ROAD:
        case CONSIDER_LT_ROAD:

            /**** Code for finding an edge ********/
            edgeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                edgeNum = findEdge(x, y);

                if (!otherPlayer.isPotentialRoad(edgeNum))
                {
                    edgeNum = 0;
                }

                if (hilight != edgeNum)
                {
                    hilight = edgeNum;
                    repaint();
                }
            }

            break;

        case CONSIDER_LM_CITY:
        case CONSIDER_LT_CITY:

            /**** Code for finding a node *********/
            nodeNum = 0;

            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                nodeNum = findNode(x, y);

                if (!otherPlayer.isPotentialCity(nodeNum))
                {
                    nodeNum = 0;
                }

                if (hilight != nodeNum)
                {
                    hilight = nodeNum;
                    repaint();
                }
            }

            break;

        case NONE:
        case GAME_OVER:
            // see hover
            if ((ptrOldX != x) || (ptrOldY != y))
            {
                ptrOldX = x;
                ptrOldY = y;
                hoverTip.handleHover(x,y);
            }
            
            break;
            
        case GAME_FORMING:
            // No hover for forming
            break;
        
        }
        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param evt DOCUMENT ME!
     */
    public void mouseClicked(MouseEvent evt)
    {
        try {
        int x = evt.getX();
        int y = evt.getY();
        
        if (evt.isPopupTrigger())
        {
            popupMenuSystime = evt.getWhen();
            evt.consume();
            doBoardMenuPopup(x,y);
            return;  // <--- Pop up menu, nothing else to do ---
        }
        
        if (evt.getWhen() < (popupMenuSystime + POPUP_MENU_IGNORE_MS))
        {
            return;  // <--- Ignore click: too soon after popup click ---
        }

        if (hilight > 0)
        {
            SOCPlayerClient client = playerInterface.getClient();

            switch (mode)
            {
            case NONE:
                break;

            case PLACE_INIT_ROAD:
            case PLACE_ROAD:

                if (player.isPotentialRoad(hilight))
                {
                    client.putPiece(game, new SOCRoad(player, hilight));

                    // Now that we've placed, clear the mode and the hilight.
                    clearModeAndHilight();
                }

                break;

            case PLACE_INIT_SETTLEMENT:
                initstlmt = hilight;

                if (player.isPotentialSettlement(hilight))
                {
                    client.putPiece(game, new SOCSettlement(player, hilight));
                    clearModeAndHilight();
                }

                break;

            case PLACE_SETTLEMENT:

                if (player.isPotentialSettlement(hilight))
                {
                    client.putPiece(game, new SOCSettlement(player, hilight));
                    clearModeAndHilight();
                }

                break;

            case PLACE_CITY:

                if (player.isPotentialCity(hilight))
                {
                    client.putPiece(game, new SOCCity(player, hilight));
                    clearModeAndHilight();
                }

                break;

            case PLACE_ROBBER:

                if (hilight != board.getRobberHex())
                {
                    client.moveRobber(game, player, hilight);
                    clearModeAndHilight();
                }

                break;

            case CONSIDER_LM_SETTLEMENT:

                if (otherPlayer.isPotentialSettlement(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCSettlement(otherPlayer, hilight));
                    clearModeAndHilight();
                }

                break;

            case CONSIDER_LM_ROAD:

                if (otherPlayer.isPotentialRoad(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCRoad(otherPlayer, hilight));
                    clearModeAndHilight();
                }

                break;

            case CONSIDER_LM_CITY:

                if (otherPlayer.isPotentialCity(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCCity(otherPlayer, hilight));
                    clearModeAndHilight();
                }

                break;

            case CONSIDER_LT_SETTLEMENT:

                if (otherPlayer.isPotentialSettlement(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCSettlement(otherPlayer, hilight));
                    clearModeAndHilight();
                }

                break;

            case CONSIDER_LT_ROAD:

                if (otherPlayer.isPotentialRoad(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCRoad(otherPlayer, hilight));
                    clearModeAndHilight();
                }

                break;

            case CONSIDER_LT_CITY:

                if (otherPlayer.isPotentialCity(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCCity(otherPlayer, hilight));
                    clearModeAndHilight();
                }

                break;
            }
        }
        else if (game.getCurrentPlayerNumber() == player.getPlayerNumber())
        {
            // No hilight. But, they clicked the board, expecting something.
            // It's possible the mode is incorrect.
            // Update and wait for the next click.
            updateMode();
            ptrOldX = 0;
            ptrOldY = 0;
            mouseMoved(evt);  // Should establish hilight using click's x,y
        }
        
        evt.consume();
        
        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
    }
    
    /** Bring up the popup menu; called from mousePressed. */
    protected void doBoardMenuPopup (int x, int y)
    {
        // Determine mode, to see if we're building or cancelling.
        switch (mode)
        {
        case PLACE_ROAD:
            popupMenu.showCancelBuild(SOCPlayingPiece.ROAD, x, y, hilight);
            break;

        case PLACE_SETTLEMENT:
            popupMenu.showCancelBuild(SOCPlayingPiece.SETTLEMENT, x, y, hilight);
            break;

        case PLACE_CITY:
            popupMenu.showCancelBuild(SOCPlayingPiece.CITY, x, y, hilight);
            break;
            
        case PLACE_INIT_ROAD:
            popupMenu.showBuild(x, y, hilight, 0, 0);
            break;
            
        case PLACE_INIT_SETTLEMENT:
            popupMenu.showBuild(x, y, 0, hilight, 0);
            break;
            
        default:  // NONE, GAME_FORMING, PLACE_ROBBER, etc
            popupMenu.showBuild(x, y, hoverTip.hoverRoadID, hoverTip.hoverSettlementID, hoverTip.hoverCityID);
        }
    }
    
    /** If the client has used the board popup menu to request building a piece,  
     *  this method is used in client network-receive message treatment.
     */
    public boolean popupExpectingBuildRequest()
    {
        if ((buildReqTimer == null) || (buildReqTimerTask == null))
            return false;
        return ! buildReqTimerTask.wasItSentAlready();
    }
    
    public void popupSetBuildRequest(int coord, int ptype)
    {
        synchronized (this)
        {
            if (buildReqTimer == null)
                buildReqTimer = new Timer(true);  // use daemon thread
        }
        synchronized (buildReqTimer)
        {
            if (buildReqTimerTask != null)
            {
                buildReqTimerTask.doNotSend();
                buildReqTimerTask.cancel();  // cancel any previous
            }
            buildReqTimerTask = new BoardPanelSendBuildTask(coord, ptype);
            // Run once, at maximum permissable delay;
            // hopefully the network is responsive and
            // we've heard back by then.
            buildReqTimer.schedule(buildReqTimerTask, 1000 * BUILD_REQUEST_MAX_DELAY_SEC );
            
        }
    }
    
    public void popupClearBuildRequest()
    {
        if (buildReqTimer == null)
            return;
        synchronized (buildReqTimer)
        {
            if (buildReqTimerTask == null)
                return;
            buildReqTimerTask.doNotSend();
            buildReqTimerTask.cancel();
            buildReqTimerTask = null;
        }
    }
    
    /** Have received gamestate placing message; send the building request in reply. */
    public void popupFireBuildingRequest()
    {
        if (buildReqTimer == null)
            return;
        synchronized (buildReqTimer)
        {
            if (buildReqTimerTask == null)
                return;
            buildReqTimerTask.sendOnceFromClientIfCurrentPlayer();
            buildReqTimerTask.cancel();
            buildReqTimerTask = null;
        }
        hoverTip.hideHoverAndPieces();  // Reset hover state
    }


    /**
     * given a pixel on the board, find the edge that contains it
     *
     * @param x  x coordinate
     * @param y  y coordinate
     * @return the coordinates of the edge, or 0 if none
     */
    private final int findEdge(int x, int y)
    {
        // find which grid section the pointer is in 
        // ( 46 is the y-distance between the centers of two hexes )
        //int sector = (x / 18) + ((y / 10) * 15);
        int sector = (x / 27) + ((y / 15) * 15);

        // System.out.println("SECTOR = "+sector+" | EDGE = "+edgeMap[sector]);
        if ((sector >= 0) && (sector < edgeMap.length))
            return edgeMap[sector];
        else
            return 0;
    }

    /**
     * given a pixel on the board, find the node that contains it
     *
     * @param x  x coordinate
     * @param y  y coordinate
     * @return the coordinates of the node, or 0 if none
     */
    private final int findNode(int x, int y)
    {
        // find which grid section the pointer is in 
        // ( 46 is the y-distance between the centers of two hexes )
        //int sector = ((x + 9) / 18) + (((y + 5) / 10) * 15);
        int sector = ((x + 13) / 27) + (((y + 7) / 15) * 15);

        // System.out.println("SECTOR = "+sector+" | NODE = "+nodeMap[sector]);
        if ((sector >= 0) && (sector < nodeMap.length))
            return nodeMap[sector];
        else
            return 0;
    }

    /**
     * given a pixel on the board, find the hex that contains it
     *
     * @param x  x coordinate
     * @param y  y coordinate
     * @return the coordinates of the hex, or 0 if none
     */
    private final int findHex(int x, int y)
    {
        // find which grid section the pointer is in 
        // ( 46 is the y-distance between the centers of two hexes )
        //int sector = (x / 18) + ((y / 10) * 15);
        int sector = (x / 27) + ((y / 15) * 15);

        // System.out.println("SECTOR = "+sector+" | HEX = "+hexMap[sector]);
        if ((sector >= 0) && (sector < hexMap.length))
            return hexMap[sector];
        else
            return 0;
    }

    /**
     * set the interaction mode
     *
     * @param m  mode
     * 
     * @see #updateMode()
     */
    public void setMode(int m)
    {
        mode = m;
        updateHoverTipToMode();
    }

    /**
     * get the interaction mode
     *
     * @return the mode
     */
    public int getMode()
    {
        return mode;
    }

    /**
     * load the images for the board
     * we need to know if this board is in an applet
     * or an application
     */
    private static synchronized void loadImages(Component c)
    {
        if (hexes == null)
        {
            MediaTracker tracker = new MediaTracker(c);
            Toolkit tk = c.getToolkit();
            Class clazz = c.getClass();
        
            hexes = new Image[13];
            numbers = new Image[10];
            ports = new Image[7];

            dice = new Image[14];

            hexes[0] = tk.getImage(clazz.getResource(IMAGEDIR + "/desertHex.gif"));
            hexes[1] = tk.getImage(clazz.getResource(IMAGEDIR + "/clayHex.gif"));
            hexes[2] = tk.getImage(clazz.getResource(IMAGEDIR + "/oreHex.gif"));
            hexes[3] = tk.getImage(clazz.getResource(IMAGEDIR + "/sheepHex.gif"));
            hexes[4] = tk.getImage(clazz.getResource(IMAGEDIR + "/wheatHex.gif"));
            hexes[5] = tk.getImage(clazz.getResource(IMAGEDIR + "/woodHex.gif"));
            hexes[6] = tk.getImage(clazz.getResource(IMAGEDIR + "/waterHex.gif"));

            for (int i = 0; i < 7; i++)
            {
                tracker.addImage(hexes[i], 0);
            }

            for (int i = 0; i < 6; i++)
            {
                hexes[i + 7] = tk.getImage(clazz.getResource(IMAGEDIR + "/miscPort" + i + ".gif"));
                tracker.addImage(hexes[i + 7], 0);
            }

            for (int i = 0; i < 6; i++)
            {
                ports[i + 1] = tk.getImage(clazz.getResource(IMAGEDIR + "/port" + i + ".gif"));
                tracker.addImage(ports[i + 1], 0);
            }

            numbers[0] = tk.getImage(clazz.getResource(IMAGEDIR + "/two.gif"));
            numbers[1] = tk.getImage(clazz.getResource(IMAGEDIR + "/three.gif"));
            numbers[2] = tk.getImage(clazz.getResource(IMAGEDIR + "/four.gif"));
            numbers[3] = tk.getImage(clazz.getResource(IMAGEDIR + "/five.gif"));
            numbers[4] = tk.getImage(clazz.getResource(IMAGEDIR + "/six.gif"));
            numbers[5] = tk.getImage(clazz.getResource(IMAGEDIR + "/eight.gif"));
            numbers[6] = tk.getImage(clazz.getResource(IMAGEDIR + "/nine.gif"));
            numbers[7] = tk.getImage(clazz.getResource(IMAGEDIR + "/ten.gif"));
            numbers[8] = tk.getImage(clazz.getResource(IMAGEDIR + "/eleven.gif"));
            numbers[9] = tk.getImage(clazz.getResource(IMAGEDIR + "/twelve.gif"));

            for (int i = 0; i < 10; i++)
            {
                tracker.addImage(numbers[i], 0);
            }

            arrowL = tk.getImage(clazz.getResource(IMAGEDIR + "/arrowL.gif"));
            arrowR = tk.getImage(clazz.getResource(IMAGEDIR + "/arrowR.gif"));

            tracker.addImage(arrowL, 0);
            tracker.addImage(arrowR, 0);

            for (int i = 2; i < 13; i++)
            {
                dice[i] = tk.getImage(clazz.getResource(IMAGEDIR + "/dice" + i + ".gif"));
                tracker.addImage(dice[i], 0);
            }

            try
            {
                tracker.waitForID(0);
            }
            catch (InterruptedException e) {}

            if (tracker.isErrorID(0))
            {
                System.out.println("Error loading board images");
            }
        }
    }

    /**
     * @return panelx
     */
    public static int getPanelX()
    {
        return panelx;
    }

    /**
     * @return panely
     */
    public static int getPanelY()
    {
        return panely;
    }
    
    
    protected class BoardToolTip
    {
        private SOCBoardPanel bpanel;
        
        /** Text to hover-display, or null if nothing to show */
        private String hoverText;
        /** Uses board mode constants: Will be NONE, PLACE_ROAD, PLACE_SETTLEMENT,
         *  PLACE_ROBBER for hex, or PLACE_INIT_SETTLEMENT for port.
         */
        private int hoverMode;
        /** "ID" coord as returned by findNode, findEdge, findHex */
        private int hoverID;
        /** Object last pointed at; null for hexes and ports */
        private SOCPlayingPiece hoverPiece;
        /** hover road ID, or 0. Readonly please from outside this inner class */
        int hoverRoadID;
        /** hover settlement or city node ID, or 0. Readonly please from outside this inner class */
        int hoverSettlementID, hoverCityID;
        /** Mouse position */
        private int mouseX, mouseY;
        /** Our position (upper-left of tooltip box) */
        private int boxX, boxY;
        /** Requested X-offset from mouse pointer (used for robber placement) */
        private int offsetX;
        /** Our size */
        private int boxW, boxH;
        
        private final int TEXT_INSET = 3;
        private final int PADDING_HORIZ = 2 * TEXT_INSET + 2;
        
        BoardToolTip(SOCBoardPanel ourBoardPanel)
        {
            bpanel = ourBoardPanel;
            hoverText = null;
            hoverMode = NONE;
            hoverID = 0;
            hoverPiece = null;
            hoverRoadID = 0;
            hoverSettlementID = 0;
            hoverCityID = 0;
            mouseX = 0;
            mouseY = 0;
            offsetX = 0;
        }
        
        /** Currently displayed text.
         * 
         * @return Tooltip text, or null if nothing.
         */
        public String getHoverText()
        {
            return hoverText;
        }
        
        public boolean isVisible()
        {
            return ((hoverText != null) || (hoverRoadID != 0)
                    || (hoverSettlementID != 0) || (hoverCityID != 0));
        }
        
        public void positionToMouse(int x, int y)
        {
            mouseX = x;
            mouseY = y;

            boxX = mouseX + offsetX;
            boxY = mouseY;
            if (SOCBoardPanel.panelx < ( boxX + boxW ))
            {
                // Try to float it to left of mouse pointer
                boxX = mouseX - boxW - offsetX;
                if (boxX < 0)
                {
                    // Not enough room, just place flush against right-hand side
                    boxX = SOCBoardPanel.panelx - boxW;
                }
            }
            
            bpanel.repaint();
            // JM TODO consider repaint(boundingbox).            
        }
        
        public void setOffsetX(int ofsX)
        {
            offsetX = ofsX;
        }
        
        public void setHoverText(String t)
        {
            hoverText = t;
            if (t == null)
            {
                bpanel.repaint();
                return;
            }

            FontMetrics fm = getFontMetrics(bpanel.getFont());           
            boxW = fm.stringWidth(hoverText) + PADDING_HORIZ;
            boxH = fm.getHeight();
            positionToMouse(mouseX, mouseY);  // Also calls repaint
        }
        
        /** Clear hover text, and cancel any hovering roads/settlements/cities */
        public void hideHoverAndPieces()
        {
            hoverRoadID = 0;
            hoverSettlementID = 0;
            hoverCityID = 0;
            setHoverText(null);
        }
        
        /** Draw; Graphics should be the boardpanel's gc, as seen in its paint method. */
        public void paint(Graphics g)
        {
            if (playerNumber != -1)
            {
                if (hoverRoadID != 0)
                {
                    drawRoad(g, hoverRoadID, playerNumber, true);
                }
                if (hoverSettlementID != 0)
                {
                    drawSettlement(g, hoverSettlementID, playerNumber, true);
                }
                if (hoverCityID != 0)
                {
                    drawCity(g, hoverCityID, playerNumber, true);
                }
            }
            if (hoverText == null)
                return;
            
            g.setColor(Color.WHITE);
            g.fillRect(boxX, boxY, boxW - 1, boxH - 1);
            g.setColor(Color.BLACK);
            g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
            g.drawString(hoverText, boxX + TEXT_INSET, boxY + boxH - TEXT_INSET);
        }

        /**
         * Mouse is hovering during normal play; look for info for tooltip text.
         * Assumes x or y has changed since last call.
         * Does not affect the "hilight" variable used by SOCBoardPanel during
         * initial placement, and during placement from clicking "Buy" buttons.
         * 
         * @param x Cursor x
         * @param y Cursor y
         */
        private void handleHover(int x, int y)
        {
            mouseX = x;
            mouseY = y;
            
            // Previous: hoverMode, hoverID, hoverText
            int id;
            boolean modeAllowsHoverPieces = ((mode != PLACE_INIT_SETTLEMENT)
                && (mode != PLACE_INIT_ROAD) && (mode != PLACE_ROBBER)
                && (mode != GAME_OVER));
            
            boolean playerIsCurrent = playerInterface.clientIsCurrentPlayer();
            boolean hoverTextSet = false;  // True once determined
            
            if (! modeAllowsHoverPieces)
            {
                hoverRoadID = 0;
                hoverSettlementID = 0;
                hoverCityID = 0;
            }

            // Look first for settlements
            //   - reminder: socboard.getAdjacentHexesToNode
            id = findNode(x,y);
            if (id > 0)
            {
                // Are we already looking at it?
                if ((hoverMode == PLACE_SETTLEMENT) && (hoverID == id))
                {
                    positionToMouse(x,y);
                    return;  // <--- Early ret: No work needed ---
                }
                
                // Is anything there?
                SOCPlayingPiece p = board.settlementAtNode(id);
                if (p != null)
                {
                    hoverMode = PLACE_SETTLEMENT;
                    hoverPiece = p;
                    hoverID = id;
                    StringBuffer sb = new StringBuffer();
                    if (p.getType() == SOCPlayingPiece.CITY)
                        sb.append("City: ");
                    else
                        sb.append("Settlement: ");
                    sb.append(p.getPlayer().getName());
                    setHoverText(sb.toString());
                    hoverTextSet = true;

                    // If we're at the player's settlement, ready to upgrade to city
                    if (modeAllowsHoverPieces && playerIsCurrent
                         && (p.getPlayer() == player)
                         && (p.getType() == SOCPlayingPiece.SETTLEMENT)
                         && (player.isPotentialCity(id)))
                    {
                        hoverCityID = id;
                    } else {
                        hoverCityID = 0;
                    }
                    hoverSettlementID = 0;
                }
                else {
                    if (playerIsCurrent)
                    {
                        // Nothing currently here.
                        hoverCityID = 0;
                        if (modeAllowsHoverPieces && player.isPotentialSettlement(id))
                            hoverSettlementID = id;
                        else
                            hoverSettlementID = 0;
                    }
                    
                    // Port check.  At most one adjacent will be a port.
                    if ((hoverMode == PLACE_INIT_SETTLEMENT) && (hoverID == id))
                    {
                        // Already looking at a port at this coordinate.
                        positionToMouse(x,y);
                        hoverTextSet = true;
                    }
                    else
                    {
                        int portType;
                        Integer coordInteger = new Integer(id);
    
                        for (portType = SOCBoard.MISC_PORT; portType <= SOCBoard.WOOD_PORT; portType++)
                        {
                            if (game.getBoard().getPortCoordinates(portType).contains(coordInteger))
                            {
                                break;
                            }
                        }
                        if (portType <= SOCBoard.WOOD_PORT)
                        {
                            String portDesc;
                            switch (portType)
                            {
                            case SOCBoard.MISC_PORT:
                                portDesc = "3:1 Port";
                                break;
    
                            case SOCBoard.CLAY_PORT:
                                portDesc = "2:1 Clay port";
                                break;
    
                            case SOCBoard.ORE_PORT:
                                portDesc = "2:1 Ore port";
                                break;
    
                            case SOCBoard.SHEEP_PORT:
                                portDesc = "2:1 Sheep port";
                                break;
    
                            case SOCBoard.WHEAT_PORT:
                                portDesc = "2:1 Wheat port";
                                break;
    
                            case SOCBoard.WOOD_PORT:
                                portDesc = "2:1 Wood port";
                                break;                            
    
                            default:
                                portDesc = "port type " + portType;                        
                            }
                        
                            setHoverText(portDesc);
                            hoverTextSet = true;
                            hoverMode = PLACE_INIT_SETTLEMENT;  // const used for hovering-at-port
                            hoverID = id;
                            
                        }  // end if-port-found                      
                    }
                }  // end if-node-has-settlement
            }
            else
            {
                hoverSettlementID = 0;
                hoverCityID = 0;                
            }

            // If not over a settlement, look for a road
            id = findEdge(x,y);
            if (id > 0)
            {
                // Are we already looking at it?
                if ((hoverMode == PLACE_ROAD) && (hoverID == id))
                {
                    positionToMouse(x,y);
                    return;  // <--- Early ret: No work needed ---
                }

                // Is anything there?
                SOCPlayingPiece p = board.roadAtEdge(id);
                if (p != null)
                {
                    if (! hoverTextSet)
                    {
                        hoverMode = PLACE_ROAD;
                        hoverPiece = p;
                        hoverID = id;
                        setHoverText("Road: " + p.getPlayer().getName());
                    }
                    hoverRoadID = 0;
                    
                    return;  // <--- Early return: Found road ---
                }
                else if (playerIsCurrent)
                {
                    // No piece there
                    if (modeAllowsHoverPieces && player.isPotentialRoad(id))
                        hoverRoadID = id;
                    else
                        hoverRoadID = 0;
                }
            }
            
            // By now we've set hoverRoadID, hoverCityID, hoverSettlementID.
            if (hoverTextSet)
            {
                return;  // <--- Early return: Text and hover-pieces set ---
            }

            // If no road, look for a hex
            //  - reminder: socboard.getHexTypeFromCoord, getNumberOnHexFromCoord, socgame.getPlayersOnHex
            id = findHex(x,y);
            if (id > 0)
            {
                // Are we already looking at it?
                if ((hoverMode == PLACE_ROBBER) && (hoverID == id))
                {
                    positionToMouse(x,y);
                    return;  // <--- Early ret: No work needed ---
                }
                
                hoverMode = PLACE_ROBBER;  // const used for hovering-at-hex
                hoverPiece = null;
                hoverID = id;
                {
                    StringBuffer sb = new StringBuffer();
                    switch (board.getHexTypeFromCoord(id))
                    {
                    case SOCBoard.DESERT_HEX:
                        sb.append("Desert");  break;
                    case SOCBoard.CLAY_HEX:
                        sb.append("Clay");    break;
                    case SOCBoard.ORE_HEX:
                        sb.append("Ore");     break;
                    case SOCBoard.SHEEP_HEX:
                        sb.append("Sheep");   break;
                    case SOCBoard.WHEAT_HEX:
                        sb.append("Wheat");   break;
                    case SOCBoard.WOOD_HEX:                     
                        sb.append("Wood");    break;
                    case SOCBoard.WATER_HEX:
                        sb.append("Water");   break;
                    default:
                        sb.append("Hex type ");
                        sb.append(board.getHexTypeFromCoord(id));
                    }
                    if (board.getRobberHex() == id)
                    {
                        int num = board.getNumberOnHexFromCoord(id);
                        if (num > 0)
                        {
                            sb.append(": ");
                            sb.append(num);
                        }
                        sb.append(" (ROBBER)");
                    }
                    setHoverText(sb.toString());                     
                }
                
                return;  // <--- Early return: Found hex ---
            }

            if (hoverRoadID != 0)
            {
                setHoverText(null); // hoverMode = PLACE_ROAD;
                bpanel.repaint();
                return;
            }
            
            // If no hex, nothing.
            hoverMode = NONE;
            setHoverText(null);
        }
        
    }  // inner class BoardToolTip
    
    

    /** This class creates a popup menu as the interface to 
      the illustrious jplot package. (TODO verbiage -JM)
      */
    class BoardPopupMenu extends PopupMenu
        implements java.awt.event.ActionListener
    {

      SOCBoardPanel bp;
      MenuItem buildRoadItem, buildSettleItem, upgradeCityItem; // , aboutItem, exitItem;
      MenuItem cancelBuildItem;
      // About us;  // TODO - JM
      /** determined at menu-show time */
      private int menuPlayerID;
      /** determined at menu-show time */
      private boolean menuPlayerIsCurrent;
      /** determined at menu-show time */
      private boolean wantsCancel;
      private int cancelBuildType;
      /** hover road ID, or 0, at menu-show time */
      private int hoverRoadID;
      /** hover settlement or city node ID, or 0, at menu-show time */
      private int hoverSettlementID, hoverCityID;
      /** Will this be for initial placement (send putpiece right away),
       *  or for placement during game (send build, receive gamestate, send putpiece)?
       */
      protected boolean isInitialPlacement;
      

      /** the constructor handles everything. The calling class needs only
          to specify which window the menu should appear in. This is
          done by passing us the menu's parent component (hopefully this
          will be a plotwinThread).  Any application specific menu items
          will be passed into this constructor by telling us what 
          function is being used. If the function has a jplot friendly menu,
          we'll find it.
          Currently, only one app-specific menu may be added on. 
          if it is large, it is expected to handle it's own submenus.
          the menu will look like this:
          (TODO verbiage - JM)
          
              ----------------
          |Zoom...       -----------
          |App Spec Menu>|Your Menu|
          |close         -----------
          |--------------|
          |about...      |
          |exit          |
          ----------------

      **/
      public BoardPopupMenu(SOCBoardPanel bpanel)
      {
        super ("JSettlers");
        bp = bpanel;

        buildRoadItem = new MenuItem("Build Road");         
        buildSettleItem = new MenuItem("Build Settlement");
        upgradeCityItem = new MenuItem("Upgrade to City");
        cancelBuildItem = new MenuItem("Cancel build");

        add(buildRoadItem);
        add(buildSettleItem);
        add(upgradeCityItem);
        addSeparator();
        add(cancelBuildItem);

        buildRoadItem.addActionListener(this);
        buildSettleItem.addActionListener(this);
        upgradeCityItem.addActionListener(this);
        cancelBuildItem.addActionListener(this);
      }

      /** Custom 'cancel' show method for when placing a road/settlement/city,
       *  giving the build/cancel options for that type of piece.
       * 
       * @param buildType piece type (SOCPlayingPiece.ROAD, CITY, SETTLEMENT)
       * @param x   Mouse x-position
       * @param y   Mouse y-position
       * @param hilightAt Current hover/hilight coordinates of piece being cancelled/placed
       */
      public void showCancelBuild(int buildType, int x, int y, int hilightAt)
      {
          menuPlayerIsCurrent = playerInterface.clientIsCurrentPlayer();
          wantsCancel = true;
          cancelBuildType = buildType;
          hoverRoadID = 0;
          hoverSettlementID = 0;
          hoverCityID = 0;

          buildRoadItem.setEnabled(false);
          buildSettleItem.setEnabled(false);
          upgradeCityItem.setEnabled(false);
          cancelBuildItem.setEnabled(menuPlayerIsCurrent);

          // Check for initial placement (for different cancel message (TODO enable initial-cancel))
          switch (game.getGameState())
          {
          case SOCGame.START1A:
          case SOCGame.START2A:
          case SOCGame.START1B:
          case SOCGame.START2B:
              isInitialPlacement = true;
              break;
          
          default:
              isInitialPlacement = false;
          }

          switch (buildType)
          {
          case SOCPlayingPiece.ROAD:
              cancelBuildItem.setLabel("Cancel road");
              buildRoadItem.setEnabled(menuPlayerIsCurrent);
              hoverRoadID = hilightAt; 
              break;
              
          case SOCPlayingPiece.SETTLEMENT:
              cancelBuildItem.setLabel("Cancel settlement");
              buildSettleItem.setEnabled(menuPlayerIsCurrent);
              hoverSettlementID = hilightAt; 
              break;
              
          case SOCPlayingPiece.CITY:
              cancelBuildItem.setLabel("Cancel city upgrade");
              upgradeCityItem.setEnabled(menuPlayerIsCurrent);
              hoverCityID = hilightAt;
              break;
              
          default:
              throw new IllegalArgumentException ("bad buildtype: " + buildType);
          }
          
          super.show(bp, x, y);
      }
      
      /** Custom show method that finds current game status and player status.
       * 
       * @param x   Mouse x-position
       * @param y   Mouse y-position
       * @param hR  Hover road ID, or 0
       * @param hS  Hover settle ID, or 0
       * @param hC  Hover city ID, or 0
       */
      public void showBuild(int x, int y, int hR, int hS, int hC)
      {
          wantsCancel = false;
          isInitialPlacement = false;
          cancelBuildItem.setEnabled(false);
          cancelBuildItem.setLabel("Cancel build");
         
          menuPlayerIsCurrent = playerInterface.clientIsCurrentPlayer();
          if (menuPlayerIsCurrent)
          {
              int gs = game.getGameState();
              switch (gs)
              {
              case SOCGame.START1A:
              case SOCGame.START2A:
                  isInitialPlacement = true;  // Settlement
                  buildRoadItem.setEnabled(false);
                  buildSettleItem.setEnabled(hS != 0);
                  upgradeCityItem.setEnabled(false);
                  break;

              case SOCGame.START1B:
              case SOCGame.START2B:
                  isInitialPlacement = true;  // Road
                  buildRoadItem.setEnabled(hR != 0);
                  buildSettleItem.setEnabled(false);
                  upgradeCityItem.setEnabled(false);
                  cancelBuildItem.setLabel("Cancel settlement");  // Initial settlement
                  cancelBuildItem.setEnabled(true);
                  cancelBuildType = SOCPlayingPiece.SETTLEMENT;
                  break;
              
              default:
                  if (gs < SOCGame.PLAY1)
                      menuPlayerIsCurrent = false;  // Not in a state to place items
              }
          }
          
          if (! menuPlayerIsCurrent)
          {
              buildRoadItem.setEnabled(false);
              buildSettleItem.setEnabled(false);
              upgradeCityItem.setEnabled(false);
              hoverRoadID = 0;
              hoverSettlementID = 0;
              hoverCityID = 0;
          } else {
              int cpn = game.getCurrentPlayerNumber();

              if (! isInitialPlacement)
              {
                  buildRoadItem.setEnabled(game.couldBuildRoad(cpn) && player.isPotentialRoad(hR));
                  buildSettleItem.setEnabled(game.couldBuildSettlement(cpn) && player.isPotentialSettlement(hS));
                  upgradeCityItem.setEnabled(game.couldBuildCity(cpn) && player.isPotentialCity(hC));
              }
              hoverRoadID = hR;
              hoverSettlementID = hS;
              hoverCityID = hC;
          }
          
          super.show(bp, x, y);
      }

      /** Handling the menu items **/
      public void actionPerformed(ActionEvent e)
      {
          if (! playerInterface.clientIsCurrentPlayer())
              return;
          if (! menuPlayerIsCurrent)
              return;
          Object target = e.getSource();
          if (target == buildRoadItem)
              tryBuild(SOCPlayingPiece.ROAD);
          else if (target == buildSettleItem)
              tryBuild(SOCPlayingPiece.SETTLEMENT);
          else if (target == upgradeCityItem)
              tryBuild(SOCPlayingPiece.CITY);
          else if (target == cancelBuildItem)
              tryCancel();
      } 

      /** Assumes player is current when calling */
      void tryBuild(int ptype)
      {
          int cpn = playerInterface.getClientPlayerNumber();
          int buildLoc;      // location
          boolean canBuild;  // resources, rules
          String btarget;    // button name on buildpanel
          
          // If we're in initial placement, or cancel/build during game, send putpiece right now.
          // Otherwise, multi-phase send.
          
          // Note that if we're in gameplay have clicked the "buy road" button
          // and trying to place it, game.couldBuildRoad will be false because
          // we've already spent the resources.  So, wantsCancel won't check it.
          
          switch (ptype)
          {
          case SOCPlayingPiece.ROAD:
              buildLoc = hoverRoadID;
              canBuild = player.isPotentialRoad(buildLoc);
              if (! (isInitialPlacement || wantsCancel))
                  canBuild = canBuild && game.couldBuildRoad(cpn);
              if (canBuild && (isInitialPlacement || wantsCancel))
                  playerInterface.getClient().putPiece(game, new SOCRoad(player, buildLoc));
              btarget = SOCBuildingPanel.ROAD;
              break;

          case SOCPlayingPiece.SETTLEMENT:
              buildLoc = hoverSettlementID;
              canBuild = player.isPotentialSettlement(buildLoc);
              if (! (isInitialPlacement || wantsCancel))
                  canBuild = canBuild && game.couldBuildSettlement(cpn);
              if (canBuild && (isInitialPlacement || wantsCancel))
              {
                  playerInterface.getClient().putPiece(game, new SOCSettlement(player, buildLoc));
                  if (isInitialPlacement)
                      initstlmt = buildLoc;  // track for initial road mouseover hilight
              }
              btarget = SOCBuildingPanel.STLMT;
              break;
          
          case SOCPlayingPiece.CITY:
              buildLoc = hoverCityID;
              canBuild = game.couldBuildCity(cpn) && player.isPotentialCity(buildLoc);
              if (! (isInitialPlacement || wantsCancel))             
                  canBuild = canBuild && game.couldBuildCity(cpn);
              if (canBuild && (isInitialPlacement || wantsCancel))
                  playerInterface.getClient().putPiece(game, new SOCCity(player, buildLoc));
              btarget = SOCBuildingPanel.CITY;
              break;

          default:
              throw new IllegalArgumentException ("Bad build type: " + ptype);
          }
          
          if (! canBuild)
          {
              playerInterface.print("Sorry, you cannot build there.");
              return;
          }
          
          if (isInitialPlacement || wantsCancel)
          {
              // - Easy, we've sent it right away.  Done with placing this piece.
              clearModeAndHilight();
              return;
          }

          // - During gameplay: Send, wait to receive gameState, send.
              
          // Set up timer to expect first-reply (and then send the second message)
          bp.popupSetBuildRequest(buildLoc, ptype);

          // Now that we're expecting that, use buttons to send the first message         
          playerInterface.getBuildingPanel().clickBuildingButton
              (game, playerInterface.getClient(), btarget, true);          
      }
      
      void tryCancel()
      {
          String btarget = null;
          switch (cancelBuildType)
          {
          case SOCPlayingPiece.ROAD:
              btarget = SOCBuildingPanel.ROAD;
              break;
          case SOCPlayingPiece.SETTLEMENT:
              btarget = SOCBuildingPanel.STLMT;
              break;
          case SOCPlayingPiece.CITY:
              btarget = SOCBuildingPanel.CITY;
              break;
          }          
          // Use buttons to cancel the build request
          playerInterface.getBuildingPanel().clickBuildingButton
              (game, playerInterface.getClient(), btarget, false);
      }

    }  // inner class BoardPopupMenu    
    

    
    /** 
     * Used for the delay between sending a build-request message,
     * and receiving a game-state message.
     * 
     * This timer will probably not be called, unless there's a large lag
     * between the server and client.  It's here just in case.
     * Ideally the server responds right away, and the client responds then.
     * 
     * @see #SOCBoardPanel.autoRollSetupTimer()
     */
    protected class BoardPanelSendBuildTask extends java.util.TimerTask
    {
        protected int buildLoc, pieceType;
        protected boolean wasSentAlready;
        
        /** Send this after maximum delay.
         * 
         * @param coord Board coordinates, as used in SOCPutPiece message
         * @param ptype Piece type, as used in SOCPlayingPiece / SOCPutPiece
         */
        protected BoardPanelSendBuildTask (int coord, int ptype)
        {
            buildLoc = coord;
            pieceType = ptype;
            wasSentAlready = false;
        }
        
        /** Board coordinates, as used in SOCPutPiece message */
        public int getBuildLoc()
        {
            return buildLoc;
        }
        
        /** Piece type, as used in SOCPlayingPiece / SOCPutPiece */
        public int getPieceType()
        {
            return pieceType;
        }
        
        /**
         * This timer will probably not be called, unless there's a large lag
         * between the server and client.  It's here just in case.
         */
        public void run()
        {
            // for debugging
            if (Thread.currentThread().getName().startsWith("Thread-"))
            {
                try {
                    Thread.currentThread().setName("timertask-boardpanel");
                }
                catch (Throwable th) {}
            }
            
            // Time is up.
            sendOnceFromClientIfCurrentPlayer();
        }
        
        public synchronized void doNotSend()
        {
            wasSentAlready = true;
        }
        
        public synchronized boolean wasItSentAlready()
        {
            return wasSentAlready;
        }
        
        /** Internally synchronized around setSentAlready/wasItSentAlready */
        public void sendOnceFromClientIfCurrentPlayer()
        {
            synchronized (this)
            {
                if (wasItSentAlready())
                    return;
                doNotSend();  // Since we're about to send it.
            }
            
            // Should only get here once, in one thread.
            if (! playerInterface.clientIsCurrentPlayer())
                return;  // Stale request, player's already changed
            
            SOCPlayerClient client = playerInterface.getClient();
            
            switch (pieceType)
            {
            case SOCPlayingPiece.ROAD:
                if (player.isPotentialRoad(buildLoc))
                    client.putPiece(game, new SOCRoad(player, buildLoc));
                break;
            case SOCPlayingPiece.SETTLEMENT:
                if (player.isPotentialSettlement(buildLoc))
                    client.putPiece(game, new SOCSettlement(player, buildLoc));
                break;
            case SOCPlayingPiece.CITY:
                if (player.isPotentialCity(buildLoc))
                    client.putPiece(game, new SOCCity(player, buildLoc));
                break;
            }

            clearModeAndHilight();           
        }
        
    }  // inner class BoardPanelSendBuildTask
    
}  // class SOCBoardPanel
