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

import soc.debug.D;  // JM

import soc.game.SOCGame;
import soc.game.SOCPlayer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.StringTokenizer;

import java.io.PrintWriter;  // For chatPrintStackTrace
import java.io.StringWriter;

/**
 * Interface for a player of Settlers of Catan
 *
 * @author Robert S. Thomas
 */
public class SOCPlayerInterface extends Frame implements ActionListener
{
    /**
     * the board display
     */
    protected SOCBoardPanel boardPanel;

    /**
     * where the player types in text
     */
    protected TextField textInput;

    /**
     * where text is displayed
     */
    protected SnippingTextArea textDisplay;

    /**
     * where chat text is displayed
     */
    protected SnippingTextArea chatDisplay;

    /**
     * interface for building pieces
     */
    protected SOCBuildingPanel buildingPanel;

    /**
     * the display for the players' hands
     */
    protected SOCHandPanel[] hands;
    
    /** 
     * Tracks our own hand within hands[], if we are
     * active in a game.  Null otherwise.
     * Set by SOCHandPanel's removePlayer() and addPlayer() methods.
     */
    protected SOCHandPanel clientHand;
    
    /**
     * Player ID of clientHand, or -1.
     * Set by SOCHandPanel's removePlayer() and addPlayer() methods.
     */
    private int clientHandPlayerNum;

    /**
     * the player colors
     */
    protected Color[] playerColors, playerColorsGhost;

    /**
     * the client that spawned us
     */
    protected SOCPlayerClient client;

    /**
     * the game associated with this interface
     */
    protected SOCGame game;

    /**
     * number of columns in the text output area
     */
    protected int ncols;

    /**
     * width of text output area in pixels
     */
    protected int npix;

    /**
     * the dialog for getting what resources the player wants to discard
     */
    protected SOCDiscardDialog discardDialog;

    /**
     * the dialog for choosing a player from which to steal
     */
    protected SOCChoosePlayerDialog choosePlayerDialog;

    /**
     * the dialog for choosing 2 resources to discover
     */
    protected SOCDiscoveryDialog discoveryDialog;

    /**
     * the dialog for choosing a resource to monopolize
     */
    protected SOCMonopolyDialog monopolyDialog;

    /**
     * create a new player interface
     *
     * @param title  title for this interface
     * @param cl     the player client that spawned us
     * @param ga     the game associated with this interface
     */
    public SOCPlayerInterface(String title, SOCPlayerClient cl, SOCGame ga)
    {
        super("Settlers of Catan Game: " + title + " [" + cl.getNickname() + "]");
        setResizable(true);

        client = cl;
        game = ga;
        clientHand = null;
        clientHandPlayerNum = -1;

        /**
         * initialize the player colors
         */
        playerColors = new Color[4];
        playerColorsGhost = new Color[4];
        // FIXME assumes game.MAXPLAYERS==4
        //playerColors[0] = new Color( 10,  63, 172); // blue
        playerColors[0] = new Color(109, 124, 231); // grey-blue
        playerColors[1] = new Color(231,  35,  35); // red
        playerColors[2] = new Color(244, 238, 206); // off-white
        playerColors[3] = new Color(249, 128,  29); // orange
        for (int i = 0; i < SOCGame.MAXPLAYERS; ++i)
        {
            playerColorsGhost[i] = makeGhostColor(playerColors[i]);
        }

        /**
         * initialize the font and the forground, and background colors
         */
        setBackground(Color.black);
        setForeground(Color.black);
        setFont(new Font("Geneva", Font.PLAIN, 10));

        /**
         * setup interface elements
         */
        initInterfaceElements();

        /**
         * we're doing our own layout management
         */
        setLayout(null);

        /**
         * more initialization stuff
         */
        setLocation(50, 50);
        setSize(840, 730);
        validate();
    }

    /**
     * Setup the interface elements
     */
    protected void initInterfaceElements()
    {
        /**
         * initialize the player hand displays and add them to the interface
         */
        hands = new SOCHandPanel[SOCGame.MAXPLAYERS];

        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
        {
            hands[i] = new SOCHandPanel(this, game.getPlayer(i));
            hands[i].setSize(180, 180);
            add(hands[i]);
        }

        /**
         * initialize the building interface and add it to the main interface
         */
        buildingPanel = new SOCBuildingPanel(this);
        buildingPanel.setSize(200, 160);
        add(buildingPanel);

        /**
         * initialize the game board display and add it to the interface
         */
        boardPanel = new SOCBoardPanel(this);
        boardPanel.setBackground(new Color(112, 45, 10));
        boardPanel.setForeground(Color.black);
        boardPanel.setSize(SOCBoardPanel.getPanelX(), SOCBoardPanel.getPanelY());
        add(boardPanel);

        /**
         * initialize the text input and display and add them to the interface
         */
        textDisplay = new SnippingTextArea("", 40, 80, TextArea.SCROLLBARS_VERTICAL_ONLY, 80);
        textDisplay.setFont(new Font("Monoco", Font.PLAIN, 10));
        textDisplay.setBackground(new Color(255, 230, 162));
        textDisplay.setForeground(Color.black);
        textDisplay.setEditable(false);
        add(textDisplay);

        chatDisplay = new SnippingTextArea("", 40, 80, TextArea.SCROLLBARS_VERTICAL_ONLY, 100);
        chatDisplay.setFont(new Font("Monoco", Font.PLAIN, 10));
        chatDisplay.setBackground(new Color(255, 230, 162));
        chatDisplay.setForeground(Color.black);
        chatDisplay.setEditable(false);
        add(chatDisplay);

        textInput = new TextField();
        textInput.setFont(new Font("Monoco", Font.PLAIN, 10));

        FontMetrics fm = this.getFontMetrics(textInput.getFont());
        textInput.setSize(SOCBoardPanel.getPanelX(), fm.getHeight() + 4);
        textInput.setBackground(new Color(255, 230, 162));
        textInput.setForeground(Color.black);
        textInput.setEditable(false);
        textInput.setText("Please wait...");
        add(textInput);
        textInput.addActionListener(this);

        addWindowListener(new MyWindowAdapter());
    }

    /**
     * Overriden so the peer isn't painted, which clears background. Don't call
     * this directly, use {@link #repaint()} instead.
     */
    public void update(Graphics g)
    {
        paint(g);
    }

    /**
     * @return the client that spawned us
     */
    public SOCPlayerClient getClient()
    {
        return client;
    }

    /**
     * @return the game associated with this interface
     */
    public SOCGame getGame()
    {
        return game;
    }

    /**
     * @return the color of a player
     * @param pn  the player number
     */
    public Color getPlayerColor(int pn)
    {
        return getPlayerColor(pn, false);
    }

    /**
     * @return the "ghosted" color of a player
     * @param pn  the player number
     */
    public Color getPlayerColor(int pn, boolean isGhost)
    {
        if (isGhost)
            return playerColorsGhost[pn];
        else
            return playerColors[pn];
    }
    
    /**
     * @return a player's hand panel
     *
     * @param pn  the player's seat number
     * 
     * @see #getClientHand()
     */
    public SOCHandPanel getPlayerHandPanel(int pn)
    {
        return hands[pn];
    }

    /**
     * @return the board panel
     */
    public SOCBoardPanel getBoardPanel()
    {
        return boardPanel;
    }

    /**
     * @return the building panel
     */
    public SOCBuildingPanel getBuildingPanel()
    {
        return buildingPanel;
    }
    
    /** The client player's SOCHandPanel interface, if active in a game.
     * 
     * @return our player's hand interface, or null if not in a game.
     */ 
    public SOCHandPanel getClientHand()
    {
        return clientHand; 
    }
    
    /** Update the client player's SOCHandPanel interface, for joining
     *  or leaving a game.
     *  
     *  Set by SOCHandPanel's removePlayer() and addPlayer() methods.
     *  
     * @param h  The SOCHandPanel for us, or null if none (leaving).
     */ 
    public void setClientHand(SOCHandPanel h)
    {
        clientHand = h;
        if (h != null)
            clientHandPlayerNum = h.getPlayer().getPlayerNumber();
        else
            clientHandPlayerNum = -1;
    }
    
    /** Is the client player active in this game, and the current player? */
    public boolean clientIsCurrentPlayer()
    {
        if (clientHand == null)
            return false;
        else
            return clientHand.isClientAndCurrentPlayer();
    }
    
    /** If client player is active in game, their player number.
     * 
     * @return client's player ID, or -1.
     */
    public int getClientPlayerNumber()
    {
        return clientHandPlayerNum;
    }

    /**
     * send the message that was just typed in
     */
    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == textInput)
        {
            String s = textInput.getText().trim();

            if (s.length() > 100)
            {
                s = s.substring(0, 100);
            }
            else if (s.length() == 0)
            {
                return;
            }

            textInput.setText("");
            client.sendText(game, s + "\n");
        }
    }

    /**
     * leave this game
     */
    public void leaveGame()
    {
        client.leaveGame(game);
        dispose();
    }

    /**
     * print text in the text window
     *
     * @param s  the text
     */
    public void print(String s)
    {
        StringTokenizer st = new StringTokenizer(s, " \n", true);
        String row = "";

        while (st.hasMoreElements())
        {
            String tk = st.nextToken();

            if (tk.equals("\n"))
            {
                continue;
            }

            if ((row.length() + tk.length()) > ncols)
            {
                textDisplay.append(row + "\n");
                row = tk;

                continue;
            }

            row += tk;
        }

        if (row.trim().length() > 0)
        {
            textDisplay.append(row + "\n");
        }
    }

    /**
     * print text in the chat window
     *
     * @param s  the text
     */
    public void chatPrint(String s)
    {
        StringTokenizer st = new StringTokenizer(s, " \n", true);
        String row = "";

        while (st.hasMoreElements())
        {
            String tk = st.nextToken();

            if (tk.equals("\n"))
            {
                continue;
            }

            if ((row.length() + tk.length()) > ncols)
            {
                chatDisplay.append(row + "\n");
                row = tk;

                continue;
            }

            row += tk;
        }

        if (row.trim().length() > 0)
        {
            chatDisplay.append(row + "\n");
        }
    }

    /**
     * an error occured, stop editing
     *
     * @param s  an error message
     */
    public void over(String s)
    {
        textInput.setEditable(false);
        textInput.setText(s);
        textDisplay.append("* Sorry, lost connection to the server.\n");
        textDisplay.append("*** Game stopped. ***\n");
        game.setCurrentPlayerNumber(-1);
        boardPanel.repaint();
    }

    /**
     * start
     */
    public void began()
    {
        textInput.setEditable(true);
        textInput.setText("");
        textInput.requestFocus();

        if ((game.getGameState() == SOCGame.NEW) || (game.getGameState() == SOCGame.READY))
        {
            for (int i = 0; i < 4; i++)
            {
                hands[i].addSitButton();
            }
        }
    }

    /**
     * a player has sat down to play
     *
     * @param n   the name of the player
     * @param pn  the seat number of the player
     */
    public void addPlayer(String n, int pn)
    {
        hands[pn].addPlayer(n);

        if (n.equals(client.getNickname()))
        {
            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                D.ebugPrintln("game.getPlayer(" + i + ").isRobot() = " + game.getPlayer(i).isRobot());

                if (game.getPlayer(i).isRobot())
                {
                    hands[i].addSeatLockBut();
                }
            }
        }
    }

    /**
     * remove a player from the game
     *
     * @param pn the number of the player
     */
    public void removePlayer(int pn)
    {
        hands[pn].removePlayer();

        if (game.getGameState() <= SOCGame.READY)
        {
            boolean match = false;

            for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
            {
                if ((game.getPlayer(i).getName() != null) && (!game.isSeatVacant(i)) && (game.getPlayer(i).getName().equals(client.getNickname())))
                {
                    match = true;

                    break;
                }
            }

            if (!match)
            {
                hands[pn].addSitButton();
            }
        }
    }

    /**
     * Game play is starting. Remove the start buttons and robot-lockout buttons.
     */
    public void startGame()
    {
        for (int i = 0; i < SOCGame.MAXPLAYERS; i++)
        {
            hands[i].removeStartBut();
            // This button has two functions (and two labels).
            // If client joined and then started a game, remove it (as robot lockout).
            // If we're joining a game in progress, keep it (as "sit here").
            hands[i].removeSitLockoutBut();
        }
    }

    /**
     * Game is over; server has sent us the revealed scores
     * for each player.  Refresh the display.
     *
     * @param finalScores Final score for each player position
     */
    public void updateAtOver(int[] finalScores)
    {
        if (game.getGameState() != SOCGame.OVER)
            return;
        
        for (int i = 0; i < finalScores.length; ++i)
        {
            game.getPlayer(i).forceFinalVP(finalScores[i]);
            hands[i].updateValue(SOCHandPanel.VICTORYPOINTS);
        }        
        repaint();
    }

    /**
     * show the discard dialog
     *
     * @param nd  the number of discards
     */
    public void showDiscardDialog(int nd)
    {
        discardDialog = new SOCDiscardDialog(this, nd);
        discardDialog.setVisible(true);
    }

    /**
     * show the choose player dialog box
     *
     * @param count   the number of players to choose from
     * @param pnums   the player ids of those players
     */
    public void choosePlayer(int count, int[] pnums)
    {
        choosePlayerDialog = new SOCChoosePlayerDialog(this, count, pnums);
        choosePlayerDialog.setVisible(true);
    }

    /**
     * show the Discovery dialog box
     */
    public void showDiscoveryDialog()
    {
        discoveryDialog = new SOCDiscoveryDialog(this);
        discoveryDialog.setVisible(true);
    }

    /**
     * show the Monopoly dialog box
     */
    public void showMonopolyDialog()
    {
        monopolyDialog = new SOCMonopolyDialog(this);
        monopolyDialog.setVisible(true);
    }
    
    /** 
     * Client is current player; state changed from PLAY to PLAY1.
     * (Dice has been rolled, or card played.)
     * Update interface accordingly.
     */
    public void updateAtPlay1()
    {
        if (clientHand != null)
            clientHand.updateAtPlay1();
    }


    /**
     * set the face icon for a player
     *
     * @param pn  the number of the player
     * @param id  the id of the face image
     */
    public void changeFace(int pn, int id)
    {
        hands[pn].changeFace(id);
    }

    /**
     * if debug is enabled, print this exception's stack trace in
     * the chat display.  This eases tracing of exceptions when
     * our code is called in AWT threads (such as EventDispatch).
     */
    public void chatPrintStackTrace(Throwable th)
    {
        chatPrintStackTrace(th, false);
    }
    
    private void chatPrintStackTrace(Throwable th, boolean isNested)
    {
        if (! D.ebugIsEnabled())
            return;
        String excepName = th.getClass().getName();
        if (! isNested)
            chatDisplay.append("** Exception occurred **\n");
        if (th.getMessage() != null)
            chatPrint(excepName + ": " + th.getMessage());
        else
            chatPrint(excepName);
        StringWriter backstack = new StringWriter();
        PrintWriter pw = new PrintWriter(backstack);
        th.printStackTrace(pw);
        pw.flush();
        chatPrint (backstack.getBuffer().toString());
        if (th.getCause() != null)  // NOTE: getCause is 1.4+
        {
            chatDisplay.append("** --> Nested Cause Exception: **\n");
            chatPrintStackTrace (th.getCause(), true);
        }        
        if (! isNested)
            chatDisplay.append("-- Exception ends: " + excepName + " --\n\n");
    }

    /** 
     * Calculate a color towards gray for the robber ghost.
     * If srcColor is light, ghost color is darker. (average with gray)
     * If it's dark or midtone, ghost should be lighter. (average with white)
     * 
     * @param srcColor The color to ghost from
     * @return Ghost color based on srcColor
     */
    public static Color makeGhostColor (Color srcColor)
    {
        int outR, outG, outB;
        outR = srcColor.getRed();
        outG = srcColor.getGreen();
        outB = srcColor.getBlue();
        if ((outR + outG + outB) > (160 * 3))
        {
            // src is light, we should be dark. (average with gray)
            outR = (outR + 140) / 2;
            outG = (outG + 140) / 2;
            outB = (outB + 140) / 2;
        } else {
            // src is dark or midtone, we should be light. (average with white)
            outR = (outR + 255) / 2;
            outG = (outG + 255) / 2;
            outB = (outB + 255) / 2;
        }
        return new Color (outR, outG, outB);
    }
    
    /**
     * do the layout
     */
    public void doLayout()
    {
        Insets i = getInsets();
        Dimension dim = getSize();
        dim.width -= (i.left + i.right);
        dim.height -= (i.top + i.bottom);

        int bw = SOCBoardPanel.getPanelX();
        int bh = SOCBoardPanel.getPanelY();
        int hw = (dim.width - bw - 16) / 2;
        int hh = (dim.height - 12) / 2;
        int kw = bw;
        int kh = buildingPanel.getSize().height;
        int tfh = textInput.getSize().height;
        int tah = dim.height - bh - kh - tfh - 16;

        boardPanel.setBounds(i.left + hw + 8, i.top + tfh + tah + 8, SOCBoardPanel.getPanelX(), SOCBoardPanel.getPanelY());

        buildingPanel.setBounds(i.left + hw + 8, i.top + tah + tfh + bh + 12, kw, kh);

        hands[0].setBounds(i.left + 4, i.top + 4, hw, hh);

        if (SOCGame.MAXPLAYERS > 1)
        {
            /* FIXME: Assumes MAXPLAYERS == 4 for layout */ 
            hands[1].setBounds(i.left + hw + bw + 12, i.top + 4, hw, hh);
            hands[2].setBounds(i.left + hw + bw + 12, i.top + hh + 8, hw, hh);
            hands[3].setBounds(i.left + 4, i.top + hh + 8, hw, hh);
        }

        int tdh = tah / 2;
        int cdh = tah - tdh;
        textDisplay.setBounds(i.left + hw + 8, i.top + 4, bw, tdh);
        chatDisplay.setBounds(i.left + hw + 8, i.top + 4 + tdh, bw, cdh);
        textInput.setBounds(i.left + hw + 8, i.top + 4 + tah, bw, tfh);

        npix = textDisplay.getPreferredSize().width;
        ncols = (int) ((((float) bw) * 100.0) / ((float) npix)) - 2;

        //FontMetrics fm = this.getFontMetrics(textDisplay.getFont());
        //int nrows = (tdh / fm.getHeight()) - 1;

        //textDisplay.setMaximumLines(nrows);
        //nrows = (cdh / fm.getHeight()) - 1;

        //chatDisplay.setMaximumLines(nrows);
        boardPanel.doLayout();
    }

    private class MyWindowAdapter extends WindowAdapter
    {
        /**
         * Leave the game when the window closes.
         */
        public void windowClosing(WindowEvent e)
        {
            leaveGame();
        }
    }
}
