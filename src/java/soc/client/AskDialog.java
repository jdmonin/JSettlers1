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
 * This is the dialog to ask players a two-choice question.
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
public abstract class AskDialog extends Dialog implements ActionListener
{
    protected SOCPlayerClient pcli;  // JM TODO - javadocs
    protected SOCPlayerInterface pi;

    protected Label msg;
    protected Button choice1But;
    protected Button choice2But;
    protected boolean choice1Default, choice2Default;

    /**
     * Creates a new AskDialog.
     *
     * @param cli      Player client interface
     * @param gamePI   Current game's player interface
     * @param titlebar Title bar text
     * @param prompt   Prompting text shown above buttons, or null
     * @param choice1  First choice button text
     * @param choice2  Second choice button text
     * @param default1 First choice is default  // JM TODO - return key?
     * @param default2 Second choice is default
     *
     * @throws IllegalArgumentException If choice1 or choice2 is null, or if both
     *    default1 and default2 are true.
     */
    public AskDialog(SOCPlayerClient cli, SOCPlayerInterface gamePI,
        String titlebar, String prompt, String choice1, String choice2,
        boolean default1, boolean default2)
        throws IllegalArgumentException
    {
        super(gamePI, titlebar, true);

    	if (choice1 == null)
                throw new IllegalArgumentException("Choice1 cannot be null");
    	if (choice2 == null)
                throw new IllegalArgumentException("Choice2 cannot be null");
    	if (default1 && default2)
                throw new IllegalArgumentException("Cannot have 2 default buttons");

        pcli = cli;
        pi = gamePI;
        setBackground(new Color(255, 230, 162));
        setForeground(Color.black);
        setFont(new Font("Geneva", Font.PLAIN, 12));

        choice1But = new Button(choice1);
        choice2But = new Button(choice2);
        choice1Default = default1;
        choice2Default = default2;

        setLayout (new BorderLayout());
        setSize(280, 60 + 2 * ColorSquare.HEIGHT);

        msg = new Label(prompt, Label.CENTER);
        add(msg, BorderLayout.CENTER);

        Panel pBtns = new Panel();
        pBtns.setLayout(new FlowLayout(FlowLayout.CENTER));

        pBtns.add(choice1But);
        choice1But.addActionListener(this);

        pBtns.add(choice2But);
        choice2But.addActionListener(this);

        add(pBtns, BorderLayout.SOUTH);

    }

    /**
     * When dialog becomes visible, set location and set focus to the default button.
     *
     * @param b Visible?
     */
    public void setVisible(boolean b)
    {
        super.setVisible(b);

        if (b)
        {
            setLocation(150, 100);  // JM TODO -does not work,shows at (0,0)

            if (choice1Default)
                choice1But.requestFocus();
            else if (choice2Default)
                choice2But.requestFocus();
        }
    }

    /**
     * Button 1 or button 2 has been chosen by the user.
     * Call button1Chosen or button2Chosen, and dispose of this dialog.
     */
    public void actionPerformed(ActionEvent e)
    {
        try {
            Object target = e.getSource();

            if (target == choice1But)
            {
                dispose();
                button1Chosen();
            }
            else if (target == choice2But)
            {
                dispose();
                button2Chosen();
            }
        } catch (Throwable th) {
            pi.chatPrintStackTrace(th);
        }
    }

    /**
     * Button 1 has been chosen by the user. React accordingly.
     * actionPerformed has already called dialog.dispose().
     */
    public abstract void button1Chosen();

    /**
     * Button 2 has been chosen by the user. React accordingly.
     * actionPerformed has already called dialog.dispose().
     */
    public abstract void button2Chosen();

}
