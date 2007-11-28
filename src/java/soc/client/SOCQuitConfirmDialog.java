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
 * This is the dialog to confirm when someone clicks the Quit Game button.
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
class SOCQuitConfirmDialog extends AskDialog
{
    /**
     * Creates a new SOCQuitConfirmDialog.
     *
     * @param cli      Player client interface
     * @param gamePI   Current game's player interface
     */
    public SOCQuitConfirmDialog(SOCPlayerClient cli, SOCPlayerInterface gamePI)
    {
        super(cli, gamePI, "Really quit game "
                + gamePI.getGame().getName() + "?",
            "Do you want to quit the game being played?",
            "Quit this game", "Continue playing", false, true);
    }

    /**
     * React to the Quit button. (call playerInterface.leaveGame)
     */
    public void button1Chosen()
    {
        pi.leaveGame();
    }

    /**
     * React to the Continue button. (Nothing to do)
     */
    public void button2Chosen()
    {
        // Nothing to do (continue playing)
    }

}