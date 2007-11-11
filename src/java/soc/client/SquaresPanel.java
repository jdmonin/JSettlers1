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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Panel;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Display grid of give/get resources
 * for trade and bank/port offers.
 *
 * @author Robert S Thomas
 *
 * @see SOCHandPanel
 * @see TradeOfferPanel
 */
public class SquaresPanel extends Panel implements MouseListener
{
    // Each ColorSquare handles its own mouse events.
    private ColorSquare[] give;
    private ColorSquare[] get;
    boolean interactive;
    boolean notAllZero;
    SOCHandPanel parentHand;

    /**
     * Creates a new SquaresPanel object.
     *
     * @param in Interactive?
     */
    public SquaresPanel(boolean in)
    {
        this (in, null);        
    }

    /**
     * Creates a new SquaresPanel object, as part of a SOCHandPanel.
     *
     * @param in Interactive?
     * @param hand HandPanel containing this SquaresPanel
     */
    public SquaresPanel(boolean in, SOCHandPanel hand)
    {
        super(null);

        interactive = in;
        notAllZero = false;
        parentHand = hand;

        setFont(new Font("Helvetica", Font.PLAIN, 10));

        give = new ColorSquare[5];
        give[0] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.CLAY);
        give[1] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.ORE);
        give[2] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.SHEEP);
        give[3] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.WHEAT);
        give[4] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.WOOD);

        get = new ColorSquare[5];
        get[0] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.CLAY);
        get[1] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.ORE);
        get[2] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.SHEEP);
        get[3] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.WHEAT);
        get[4] = new ColorSquareLarger(ColorSquare.NUMBER, in, ColorSquare.WOOD);

        for (int i = 0; i < 5; i++)
        {
            add(get[i]);
            add(give[i]);
            get[i].setSquaresPanel(this);
            give[i].setSquaresPanel(this);
            get[i].addMouseListener(this);
            give[i].addMouseListener(this);
        }

        int lineH = ColorSquareLarger.HEIGHT_L - 1;
        int sqW = ColorSquareLarger.WIDTH_L - 1;
        setSize((5 * sqW) + 1, (2 * lineH) + 1);
    }

    /**
     * DOCUMENT ME!
     */
    public void doLayout()
    {
        int lineH = ColorSquareLarger.HEIGHT_L - 1;
        int sqW = ColorSquareLarger.WIDTH_L - 1;
        int i;

        for (i = 0; i < 5; i++)
        {
            give[i].setSize(sqW + 1, lineH + 1);
            give[i].setLocation(i * sqW, 0);
            //give[i].draw();
            get[i].setSize(sqW + 1, lineH + 1);
            get[i].setLocation(i * sqW, lineH);
            //get[i].draw();
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseEntered(MouseEvent e)
    {
        ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void mouseExited(MouseEvent e)
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
    public void mousePressed(MouseEvent e)
    {
        Object target = e.getSource();

    if ( ! interactive )
        return;

        for (int i = 0; i < 5; i++)
        {
            if ( (target == get[i]) && (give[i].getIntValue() > 0) )
            {
                give[i].subtractValue(1);
                get[i].subtractValue(1);
            }
            else if ( (target == give[i]) && (get[i].getIntValue() > 0) )
            {
                get[i].subtractValue(1);
                give[i].subtractValue(1);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param give DOCUMENT ME!
     * @param get DOCUMENT ME!
     */
    public void setValues(int[] give, int[] get)
    {
        boolean notAllZ = false;
        for (int i = 0; i < 5; i++)
        {
            this.give[i].setIntValue(give[i]);
            this.get[i].setIntValue(get[i]);
            if ((give[i]!=0) || (get[i]!=0))
                notAllZ = true;
        }
        notAllZero = notAllZ;
    }

    /**
     * DOCUMENT ME!
     *
     * @param give DOCUMENT ME!
     * @param get DOCUMENT ME!
     */
    public void getValues(int[] give, int[] get)
    {
        for (int i = 0; i < 5; i++)
        {
            give[i] = this.give[i].getIntValue();
            get[i] = this.get[i].getIntValue();
        }
    }
 
    /** Does any grid square contain a non-zero value? */
    public boolean containsNonZero()
    {
        return notAllZero;
    }
    
    /** Called by colorsquare when clicked; if we're part of a HandPanel,
     *  could enable/disable its buttons based on new value.
     */
    public void squareChanged(ColorSquare sq, int newValue)
    {
        boolean wasNotZero = notAllZero;
        
        if (newValue != 0)
            notAllZero = true;
        else
        {
            // A square became zero; how are the others?
            boolean notAllZ = false;
            for (int i = 0; i < 5; i++)
            {
                if (0 != this.give[i].getIntValue())
                {
                    notAllZ = true;
                    break;
                }
                if (0 != this.get[i].getIntValue())
                {
                    notAllZ = true;
                    break;
                }
            }
            
            notAllZero = notAllZ;
        }
        
        if ((parentHand != null) && (wasNotZero != notAllZero))
            parentHand.sqPanelZerosChange(notAllZero);
    }

}
