/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file copyright (C) 2007,2008 Jeremy D Monin <jeremy@nand.net>
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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import soc.util.Version;


/**
 * This is the dialog to confirm when someone clicks the Quit Game button.
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
public class SOCConnectOrPracticePanel extends Panel
    implements ActionListener // , WindowListener, KeyListener
{
    private SOCPlayerClient cl;

    private Button prac; 
    private Button runserv;
    private Button connserv;
    
    /** Determined by calling {@link #checkCanLaunchServer()} */
    private boolean canLaunchServer;

    /**
     * Creates a new SOCQuitConfirmDialog.
     *
     * @param cli      Player client interface
     * @param gamePI   Current game's player interface
     * @param gameIsOver The game is over - "Quit" button should be default (if not over, Continue is default)
     */
    public SOCConnectOrPracticePanel(SOCPlayerClient cli)
    {
        cl = cli;
        canLaunchServer = checkCanLaunchServer();

        // same Frame setup as in SOCPlayerClient.main
        setBackground(new Color(Integer.parseInt("61AF71",16)));
        setForeground(Color.black);
        
        initInterfaceElements();
    }

    /**
     * Check with the {@link java.lang.SecurityManager} about being a tcp server.
     * @return
     */
    public static boolean checkCanLaunchServer()
    {
        try
        {
            SecurityManager sm = System.getSecurityManager();
            if (sm == null)
                return true;
            try
            {
                sm.checkAccept("localhost", 4444);
                sm.checkListen(4444);
            }
            catch (SecurityException se)
            {
                return false;
            }
        }
        catch (SecurityException se)
        {
            // can't read security mgr; check it the hard way
            for (int port = 4444; port <= 4544; ++port)
            {
                ServerSocket ss = null;
                try
                {
                    ss = new ServerSocket(port);
                    ss.setReuseAddress(true);
                    ss.setSoTimeout(11);  // very short (11 ms)
                    ss.accept();  // will time out soon
                    ss.close();
                }
                catch (SocketTimeoutException ste)
                {
                    // Allowed to bind
                    try
                    {
                        ss.close();
                    }
                    catch (IOException ie) {}
                    return true;
                }
                catch (IOException ie)
                {
                    // maybe already bound: ok, try next port in loop
                }
                catch (SecurityException se2)
                {
                    return false;  // Not allowed to have a server socket
                }
            }
        }
        return false;
    }

    private void initInterfaceElements()
    {
        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gbl);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1; // GridBagConstraints.REMAINDER;

        Label status = new Label("x");
        gbl.setConstraints(status, gbc);
        add(status);

        gbc.gridwidth = GridBagConstraints.REMAINDER;

        connserv = new Button("Connect to a Server");
        gbl.setConstraints(connserv, gbc);
        add(connserv);
        connserv.addActionListener(this);

        prac = new Button("Practice");
        gbl.setConstraints(prac, gbc);
        add(prac);
        prac.addActionListener(this);

        runserv = new Button("Start a Server");
        gbl.setConstraints(runserv, gbc);
        if (! canLaunchServer)
            runserv.setEnabled(false);
        add(runserv);
        if (canLaunchServer)
            runserv.addActionListener(this);
    }

    public void actionPerformed(ActionEvent ae)
    {
        if (ae.getSource() == prac)
        {
            // Ask client to set up and start a practice game
            cl.clickPracticeButton();
        }
    }

}
