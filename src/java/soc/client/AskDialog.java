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
import java.awt.FontMetrics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


/**
 * This is the generic dialog to ask players a two-choice question.
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
public abstract class AskDialog extends Dialog implements ActionListener, WindowListener
{
    /** Player client; passed to constructor, not null */
    protected SOCPlayerClient pcli;

    /** Player interface; passed to constructor, not null */
    protected SOCPlayerInterface pi;

    /** Prompt message, or null */
    protected Label msg;

    /** Button for first choice.
     *
     * @see #button1Chosen()
     */
    protected Button choice1But;

    /** Button for second choice.
     *
     * @see #button2Chosen()
     */
    protected Button choice2But;

    /** Is this choice the default? */
    protected boolean choice1Default, choice2Default;

    /** Desired size **/
    protected int wantW, wantH;

    /** Padding beyond desired size; not known until show() **/
    protected int padW, padH;

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
     * @throws IllegalArgumentException If both default1 and default2 are true,
     *    or if any of these is null: cli, gamePI, prompt, choice1, choice2.
     */
    public AskDialog(SOCPlayerClient cli, SOCPlayerInterface gamePI,
        String titlebar, String prompt, String choice1, String choice2,
        boolean default1, boolean default2)
        throws IllegalArgumentException
    {
        super(gamePI, titlebar, true);

        if (cli == null)
            throw new IllegalArgumentException("cli cannot be null");
        if (gamePI == null)
            throw new IllegalArgumentException("gamePI cannot be null");
    	if (choice1 == null)
            throw new IllegalArgumentException("choice1 cannot be null");
    	if (choice2 == null)
            throw new IllegalArgumentException("choice2 cannot be null");
    	if (default1 && default2)
            throw new IllegalArgumentException("Cannot have 2 default buttons");

        pcli = cli;
        pi = gamePI;
        setBackground(new Color(255, 230, 162));
        setForeground(Color.black);
        setFont(new Font("Dialog", Font.PLAIN, 12));  // JM TODO - font name?

        choice1But = new Button(choice1);
        choice2But = new Button(choice2);
        choice1Default = default1;
        choice2Default = default2;

        setLayout (new BorderLayout());

        msg = new Label(prompt, Label.CENTER);
        add(msg, BorderLayout.CENTER);

        wantW = 6 + getFontMetrics(msg.getFont()).stringWidth(prompt);
        if (wantW < 280)
            wantW = 280;
        wantH = 40 + 2 * ColorSquare.HEIGHT;
        padW = 0;  // Won't be able to call getInsets and know the values, until show()
        padH = 0;
        setSize(wantW + 6, wantH + 20);
        setLocation(150, 100);

        Panel pBtns = new Panel();
        pBtns.setLayout(new FlowLayout(FlowLayout.CENTER));

        pBtns.add(choice1But);
        choice1But.addActionListener(this);

        pBtns.add(choice2But);
        choice2But.addActionListener(this);

        add(pBtns, BorderLayout.SOUTH);
        
        addWindowListener(this);  // To handle close-button
    }

    /**
     * Adjust size (insets) and set focus to the default button.
     */
    protected void checkSizeAndFocus()
    {
        // Can't call getInsets and know the values, until show().
        // Maybe not even then (STATE).
        padW = getInsets().left + getInsets().right;
        padH = getInsets().top + getInsets().bottom;
        if ((padW > 0) || (padH > 0))
        {
            setSize (wantW + padW, wantH + padH);
        }

        if (choice1Default)
            choice1But.requestFocus();
        else if (choice2Default)
            choice2But.requestFocus();
    }

    /**
     * When dialog becomes visible, adjust size (insets) and set focus to the default button.
     */
    public void show()
    {
        super.show();
        checkSizeAndFocus();
    }

    /**
     * When dialog becomes visible, adjust size (insets) and set focus to the default button.
     *
     * @param b Visible?
     */
    public void setVisible(boolean b)
    {
        super.setVisible(b);

        if (b)
            checkSizeAndFocus();
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

    /**
     * The dialog window was closed by the user, or ESC was pressed. React accordingly.
     * AskDialog has already called dialog.dispose().
     */
    public abstract void windowCloseChosen();

    /**
     * Dialog close requested by user. Dispose and call windowCloseChosen.
     */
    public void windowClosing(WindowEvent e)
    {
        dispose();
        windowCloseChosen();
    }

    /** Stub required by WindowListener */
    public void windowActivated(WindowEvent e) { }

    /** Stub required by WindowListener */
    public void windowClosed(WindowEvent e) { }

    /** Stub required by WindowListener */
    public void windowDeactivated(WindowEvent e) { }

    /** Stub required by WindowListener */
    public void windowDeiconified(WindowEvent e) { }

    /** Stub required by WindowListener */
    public void windowIconified(WindowEvent e) { }

    /** Stub required by WindowListener */
    public void windowOpened(WindowEvent e) { }

}
