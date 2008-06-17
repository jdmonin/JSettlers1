/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file copyright (C) 2008 Jeremy D Monin <jeremy@nand.net>
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
import java.awt.TextField;
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

    /** "Practice" */
    private Button prac;

    /** "Connect to server" */
    private Button connserv;
    /** Contains GUI elements for details in {@link #connserv} */
    private Panel panel_conn;
    private TextField conn_servhost, conn_servport, conn_user, conn_pass;
    private Button conn_connect, conn_cancel;

    /** "Start a server" */
    private Button runserv;
    /** Contains GUI elements for details in {@link #runserv}, or null if can't run. */
    private Panel panel_run;
    private TextField run_servport;
    private Button run_startserv, run_cancel;

    /**
     * Do we have security to run a TCP server?
     * Determined by calling {@link #checkCanLaunchServer()}.
     */
    private boolean canLaunchServer;

    /**
     * Creates a new SOCConnectOrPracticePanel.
     *
     * @param cli      Player client interface
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
     * Port {@link SOCPlayerClient#SOC_PORT_DEFAULT} and some subsequent ports are checked (to be above 1024).
     * @return True if we have perms to start a server and listen on a port
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
                sm.checkAccept("localhost", SOCPlayerClient.SOC_PORT_DEFAULT);
                sm.checkListen(SOCPlayerClient.SOC_PORT_DEFAULT);
            }
            catch (SecurityException se)
            {
                return false;
            }
        }
        catch (SecurityException se)
        {
            // can't read security mgr; check it the hard way
            int port = SOCPlayerClient.SOC_PORT_DEFAULT;
            for (int i = 0; i <= 100; ++i)
            {
                ServerSocket ss = null;
                try
                {
                    ss = new ServerSocket(i + port);
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
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        Label L = new Label("Please choose an option.");
        L.setAlignment(Label.CENTER);
        gbl.setConstraints(L, gbc);
        add(L);

        /**
         * Interface setup: Connect to a Server
         */

        connserv = new Button("Connect to a Server...");
        gbl.setConstraints(connserv, gbc);
        add(connserv);
        connserv.addActionListener(this);

        panel_conn = initInterface_conn();  // panel_conn setup
        panel_conn.setVisible(false);
        gbl.setConstraints(panel_conn, gbc);
        add (panel_conn);

        /**
         * Interface setup: Practice
         */
        prac = new Button("Practice");
        gbl.setConstraints(prac, gbc);
        add(prac);
        prac.addActionListener(this);

        /**
         * Interface setup: Start a Server
         */
        runserv = new Button("Start a Server...");
        gbl.setConstraints(runserv, gbc);
        if (! canLaunchServer)
            runserv.setEnabled(false);
        add(runserv);
        if (canLaunchServer)
        {
            runserv.addActionListener(this);
            panel_run = initInterface_run();  // panel_run setup
            panel_run.setVisible(false);
            gbl.setConstraints(panel_run, gbc);
            add (panel_run);
        } else {
            panel_run = null;
        }
    }

    /** panel_conn setup */
    private Panel initInterface_conn()
    {
        Panel pconn = new Panel();
        Label L;

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        pconn.setLayout(gbl);

        L = new Label("Server");
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_servhost = new TextField(20);
        gbc.gridwidth = 2;
        gbl.setConstraints(conn_servhost, gbc);
        pconn.add(conn_servhost);        
        L = new Label(" ");  // Spacing for rest of form's rows
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(L, gbc);
        pconn.add(L);

        L = new Label("Port");
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_servport = new TextField(20);
        conn_servport.setText(Integer.toString(cl.port));
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(conn_servport, gbc);
        pconn.add(conn_servport);

        L = new Label("Nickname");
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_user = new TextField(20);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(conn_user, gbc);
        pconn.add(conn_user);

        L = new Label("Password");
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_pass = new TextField(20);
        conn_pass.setEchoChar('*');
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(conn_pass, gbc);
        pconn.add(conn_pass);

        L = new Label(" ");
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        pconn.add(L);
        conn_connect = new Button("Connect...");
        conn_connect.addActionListener(this);
        gbl.setConstraints(conn_connect, gbc);
        pconn.add(conn_connect);

        conn_cancel = new Button("Cancel");
        conn_cancel.addActionListener(this);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(conn_cancel, gbc);
        pconn.add(conn_cancel);
        
        return pconn;
    }

    /** panel_run setup */
    private Panel initInterface_run()
    {
        Panel prun = new Panel();
        Label L;

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        prun.setLayout(gbl);

        L = new Label("Port");
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        prun.add(L);
        run_servport = new TextField(15);
        run_servport.setText(Integer.toString(cl.port));
        gbc.gridwidth = 2;
        gbl.setConstraints(run_servport, gbc);
        prun.add(run_servport);
        L = new Label(" ");  // Spacing for rest of form's rows
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(L, gbc);
        prun.add(L);

        L = new Label(" ");
        gbc.gridwidth = 1;
        gbl.setConstraints(L, gbc);
        prun.add(L);
        run_startserv = new Button(" Start ");
        run_startserv.addActionListener(this);
        gbl.setConstraints(run_startserv, gbc);
        prun.add(run_startserv);

        run_cancel = new Button("Cancel");
        run_cancel.addActionListener(this);
        gbl.setConstraints(run_cancel, gbc);
        prun.add(run_cancel);
        
        return prun;
    }

    public void actionPerformed(ActionEvent ae)
    {
        try {
            
        Object src = ae.getSource();
        if (src == prac)
        {
            // Ask client to set up and start a practice game
            cl.clickPracticeButton();
            return;
        }
        
        if ((src == conn_connect)
            || (panel_conn.isVisible() && (src == connserv)))
        {
            // After clicking connserv,
            // actually connect to server
            clickConnConnect();
            return;
        }

        if (src == connserv)
        {
            // Show fields to get details to connect to server later
            panel_conn.setVisible(true);
            if ((panel_run != null) && panel_run.isVisible())
                panel_run.setVisible(false);
            validate();
        }

        if (src == conn_cancel)
        {
            // Hide fields used to connect to server
            panel_conn.setVisible(false);
            validate();
            return;
        }

        if ((src == run_startserv)
            || (panel_run.isVisible() && (src == runserv)))
        {
            // After clicking runserv,
            // actually start a server
            int cport = 0;
            try {
                cport = Integer.parseInt(conn_servport.getText());
            }
            catch (NumberFormatException e)
            {
                // TODO show error?
                return;
            }
            cl.startLocalTCPServer(cport);
            return;
        }

        if (src == runserv)
        {
            // Show fields to get details to start a TCP server
            panel_run.setVisible(true);
            if ((panel_conn != null) && panel_conn.isVisible())
                panel_conn.setVisible(false);
            validate();
            return;
        }
        
        if (src == run_cancel)
        {
            // Hide fields used to start a server
            panel_run.setVisible(false);
            validate();
            return;
        }

        }  // try
        catch(Throwable thr)
        {
            System.err.println("-- Error caught in AWT event thread: " + thr + " --");
            thr.printStackTrace();
            while (thr.getCause() != null)
            {
                thr = thr.getCause();
                System.err.println(" --> Cause: " + thr + " --");
                thr.printStackTrace();
            }
            System.err.println("-- Error stack trace end --");
            System.err.println();
        }

    }

    /** "Connect..." from connect setup; check fields, etc */
    private void clickConnConnect()
    {
        // TODO Check contents of fields
        String cserv = conn_servhost.getText().trim();
        if (cserv.length() == 0)
            cserv = null;  // localhost
        int cport = 0;
        try {
            cport = Integer.parseInt(conn_servport.getText());
        }
        catch (NumberFormatException e)
        {
            // TODO show error?
            return;
        }

        // Copy fields, show MAIN_PANEL, and connect in client
        cl.connect(cserv, cport, conn_user.getText(), conn_pass.getText());
    }
}
