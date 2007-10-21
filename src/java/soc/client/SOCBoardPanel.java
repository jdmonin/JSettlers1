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
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.util.Enumeration;


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
    public final static int GAME_FORMING = 99;
    
    /** During robber placement, the tooltip is moved this far over to make room. */
    public final static int HOVER_OFFSET_X_FOR_ROBBER = 15;

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
     * constants: Will be NONE, PLACE_ROAD, PLACE_SETTLEMENT, or PLACE_ROBBER for hex.
     */
    private BoardToolTip hoverTip;

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
    public Dimension getPreferedSize()
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
    private final void drawRoad(Graphics g, int edgeNum, int pn)
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

        g.setColor(playerInterface.getPlayerColor(pn));

        g.fillPolygon(tmpX, tmpY, 5);
        g.setColor(Color.black);
        g.drawPolygon(tmpX, tmpY, 5);
    }

    /**
     * draw a settlement
     */
    private final void drawSettlement(Graphics g, int nodeNum, int pn)
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
        g.setColor(playerInterface.getPlayerColor(pn));
        g.fillPolygon(tmpX, tmpY, 6);
        g.setColor(Color.black);
        g.drawPolygon(tmpX, tmpY, 7);
    }

    /**
     * draw a city
     */
    private final void drawCity(Graphics g, int nodeNum, int pn)
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
            drawRoad(g, r.getCoordinates(), r.getPlayer().getPlayerNumber());
        }

        /**
         * draw the settlements
         */
        Enumeration settlements = board.getSettlements().elements();

        while (settlements.hasMoreElements())
        {
            SOCSettlement s = (SOCSettlement) settlements.nextElement();
            drawSettlement(g, s.getCoordinates(), s.getPlayer().getPlayerNumber());
        }

        /**
         * draw the cities
         */
        Enumeration cities = board.getCities().elements();

        while (cities.hasMoreElements())
        {
            SOCCity c = (SOCCity) cities.nextElement();
            drawCity(g, c.getCoordinates(), c.getPlayer().getPlayerNumber());
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
                drawRoad(g, hilight, player.getPlayerNumber());
            }

            break;

        case PLACE_SETTLEMENT:
        case PLACE_INIT_SETTLEMENT:

            if (hilight > 0)
            {
                drawSettlement(g, hilight, player.getPlayerNumber());
            }

            break;

        case PLACE_CITY:

            if (hilight > 0)
            {
                drawCity(g, hilight, player.getPlayerNumber());
            }

            break;

        case CONSIDER_LM_SETTLEMENT:
        case CONSIDER_LT_SETTLEMENT:

            if (hilight > 0)
            {
                drawSettlement(g, hilight, otherPlayer.getPlayerNumber());
            }

            break;

        case CONSIDER_LM_ROAD:
        case CONSIDER_LT_ROAD:

            if (hilight > 0)
            {
                drawRoad(g, hilight, otherPlayer.getPlayerNumber());
            }

            break;

        case CONSIDER_LM_CITY:
        case CONSIDER_LT_CITY:

            if (hilight > 0)
            {
                drawCity(g, hilight, otherPlayer.getPlayerNumber());
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
        if (mode == NONE)            
            hoverTip.setOffsetX(0);
        else if (mode == PLACE_ROBBER)
            hoverTip.setOffsetX(HOVER_OFFSET_X_FOR_ROBBER);
        else
            hoverTip.setHoverText(null);
    }

    /**
     * set the player that is using this board panel
     */
    public void setPlayer()
    {
        player = game.getPlayer(playerInterface.getClient().getNickname());
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
    public void mouseClicked(MouseEvent e)
    {
        ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseReleased(MouseEvent e)
    {
        ;
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
            hoverTip.setHoverText(null);  // Hide it
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
                    repaint();
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
    public void mousePressed(MouseEvent evt)
    {
        try {
        int x = evt.getX();
        int y = evt.getY();

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
                }

                break;

            case PLACE_INIT_SETTLEMENT:
                initstlmt = hilight;

                if (player.isPotentialSettlement(hilight))
                {
                    client.putPiece(game, new SOCSettlement(player, hilight));
                }

                break;

            case PLACE_SETTLEMENT:

                if (player.isPotentialSettlement(hilight))
                {
                    client.putPiece(game, new SOCSettlement(player, hilight));
                }

                break;

            case PLACE_CITY:

                if (player.isPotentialCity(hilight))
                {
                    client.putPiece(game, new SOCCity(player, hilight));
                }

                break;

            case PLACE_ROBBER:

                if (hilight != board.getRobberHex())
                {
                    client.moveRobber(game, player, hilight);
                }

                break;

            case CONSIDER_LM_SETTLEMENT:

                if (otherPlayer.isPotentialSettlement(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCSettlement(otherPlayer, hilight));
                }

                break;

            case CONSIDER_LM_ROAD:

                if (otherPlayer.isPotentialRoad(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCRoad(otherPlayer, hilight));
                }

                break;

            case CONSIDER_LM_CITY:

                if (otherPlayer.isPotentialCity(hilight))
                {
                    client.considerMove(game, otherPlayer.getName(), new SOCCity(otherPlayer, hilight));
                }

                break;

            case CONSIDER_LT_SETTLEMENT:

                if (otherPlayer.isPotentialSettlement(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCSettlement(otherPlayer, hilight));
                }

                break;

            case CONSIDER_LT_ROAD:

                if (otherPlayer.isPotentialRoad(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCRoad(otherPlayer, hilight));
                }

                break;

            case CONSIDER_LT_CITY:

                if (otherPlayer.isPotentialCity(hilight))
                {
                    client.considerTarget(game, otherPlayer.getName(), new SOCCity(otherPlayer, hilight));
                }

                break;
            }

            mode = NONE;
            hilight = 0;
        }
        } catch (Throwable th) {
            playerInterface.chatPrintStackTrace(th);
        }
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
        /** Uses mode constants: Will be NONE, PLACE_ROAD, PLACE_SETTLEMENT, or PLACE_ROBBER for hex */
        private int hoverMode;
        /** "ID" coord as returned by findNode, findEdge, findHex */
        private int hoverID;
        /** Object last pointed at; null for hexes */
        private SOCPlayingPiece hoverPiece;
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
            return (hoverText != null);
        }
        
        public void positionToMouse(int x, int y)
        {
            mouseX = x;
            mouseY = y;

            boxX = mouseX + offsetX;
            boxY = mouseY;
            if (SOCBoardPanel.panelx < ( boxX + boxW ))
            {
                boxX = SOCBoardPanel.panelx - boxW;
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
        
        /** Draw; Graphics should be the boardpanel's gc, as seen in its paint method. */
        public void paint(Graphics g)
        {
            if (hoverText == null)
                return;
            
            g.setColor(Color.WHITE);
            g.fillRect(boxX, boxY, boxW - 1, boxH - 1);
            g.setColor(Color.BLACK);
            g.drawRect(boxX, boxY, boxW - 1, boxH - 1);
            g.drawString(hoverText, boxX + TEXT_INSET, boxY + boxH - TEXT_INSET);
        }

        /**
         * Mouse is hovering during normal play; look for info for tooltip.
         * Assumes x or y has changed since last call.
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

                    return;  // <--- Early return: Found settlement/city ---
                }
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
                    hoverMode = PLACE_ROAD;
                    hoverPiece = p;
                    hoverID = id;
                    setHoverText("road: " + p.getPlayer().getName());
                    
                    return;  // <--- Early return: Found road ---
                }
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
                
                hoverMode = PLACE_ROBBER;
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
                        sb.append(" (ROBBER)");
                    }
                    setHoverText(sb.toString());                     
                }
                
                return;  // <--- Early return: Found hex ---
            }
            
            // If no hex, nothing.
            hoverMode = NONE;
            setHoverText(null);
        }
        
    }  // inner class BoardToolTip
    
}  // class SOCBoardPanel
