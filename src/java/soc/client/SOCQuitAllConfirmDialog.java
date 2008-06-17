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
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import soc.game.SOCGame;


/**
 * This is the dialog to confirm when someone closes the client.
 * The quit action is System.exit(0).
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
class SOCQuitAllConfirmDialog extends AskDialog
{
    /**
     * Creates and shows a new SOCQuitAllConfirmDialog.
     * "Continue" is default.
     *
     * @param cli      Player client interface
     * @param gamePI   An active game's player interface
     * @throws IllegalArgumentException If cli or gamePI is null
     */
    public static void createAndShow(SOCPlayerClient cli, SOCPlayerInterface gamePI)
        throws IllegalArgumentException
    {
        if ((cli == null) || (gamePI == null))
            throw new IllegalArgumentException("no nulls");
        boolean hasAny = cli.anyHostedActiveGames();

        SOCQuitAllConfirmDialog qcd = new SOCQuitAllConfirmDialog(cli, gamePI);
        qcd.show();      
    }
    

    /**
     * Creates a new SOCQuitAllConfirmDialog.
     *
     * @param cli      Player client interface
     * @param gamePI   Current game's player interface
     */
    protected SOCQuitAllConfirmDialog(SOCPlayerClient cli, SOCPlayerInterface gamePI)
    {
        super(cli, gamePI, "Really quit all games?",
            "One or more games are still active.",
            "Quit all games",
            "Continue playing",
            null,
            2);
    }

    /**
     * React to the Quit button. Call System.exit(0) as SOCPlayerClient does.
     */
    public void button1Chosen()
    {
        System.exit(0);
    }

    /**
     * React to the Continue button. (Nothing to do)
     */
    public void button2Chosen()
    {
        // Nothing to do (continue playing)
    }

    /**
     * Button 3 is not part of this AskDialog.
     */
    public void button3Chosen()
    {
        // This button is not used.
    }

    /**
     * React to the dialog window closed by user. (Nothing to do)
     */
    public void windowCloseChosen()
    {
        // Nothing to do (continue playing)
    }

}
