/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * Copyright (C) 2003  Robert S. Thomas
 * This file copyright (C) 2007 Jeremy D Monin <jeremy@nand.net>
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
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This is the dialog to ask players if they want to join an existing practice game,
 * or start a new one.
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
class SOCPracticeAskDialog extends Dialog implements ActionListener
{
    Button showBut;
    Button createBut;
    Label msg;
    SOCPlayerClient pcli;
    SOCPlayerInterface pi;

    /**
     * Creates a new SOCPracticeAskDialog.
     *
     * @param cli      Player client interface
     * @param gamePI   Current game's player interface
     */
    public SOCPracticeAskDialog(SOCPlayerClient cli, SOCPlayerInterface gamePI)
    {
        super(gamePI, "Practice game in progress", true);

        pcli = cli;
        pi = gamePI;
        setBackground(new Color(255, 230, 162));
        setForeground(Color.black);
        setFont(new Font("Geneva", Font.PLAIN, 12));

        showBut = new Button("Show this game");
        createBut = new Button("Create another");

        setLayout (new BorderLayout());
        setSize(280, 60 + 2 * ColorSquareLarger.HEIGHT_L);

        msg = new Label("A practice game is already being played.", Label.CENTER);
        add(msg, BorderLayout.CENTER);

        Panel pBtns = new Panel();
        pBtns.setLayout(new FlowLayout(FlowLayout.CENTER));
        
        pBtns.add(showBut);
        showBut.addActionListener(this);

        pBtns.add(createBut);
        createBut.addActionListener(this);

        add(pBtns, BorderLayout.SOUTH);

    }

    /**
     * When dialog becomes visible, set focus to the "Show" button.
     *
     * @param b Visible?
     */
    public void setVisible(boolean b)
    {
        super.setVisible(b);

        if (b)
        {
            showBut.requestFocus();
        }
    }

    /**
     * React to the Show or Create button.
     */
    public void actionPerformed(ActionEvent e)
    {
        try {
            Object target = e.getSource();
    
            if (target == showBut)
            {
                dispose();
                pi.show();
            }
            else if (target == createBut)
            {
                dispose();
                pcli.startPracticeGame();
            }
        } catch (Throwable th) {
            pi.chatPrintStackTrace(th);
        }
    }
}
