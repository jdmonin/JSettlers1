/**
 * Java Settlers - An online multiplayer version of the game Settlers of Catan
 * This file Copyright (C) 2008 Jeremy Monin <jeremy@nand.net>
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
package soc.game;



/**
 * This class holds the results of a call to {@link SOCGame#forceEndTurn()}.
 * Specifically, the resulting action type, and possibly list of discarded
 * resources.
 */
public class SOCForceEndTurnResult
{
    /** Result type, like {@link #FORCE_ENDTURN_NONE} */
    private int result;

    /**
     * Resources gained (returned to cancel piece placement) or lost (discarded), or null.
     * Lost resources are negative values in this set.
     */
    private SOCResourceSet gainLoss;

    /**
     * {@link SOCGame#forceEndTurn()} return values
     */
    public static final int FORCE_ENDTURN_MIN              = 1;  // Lowest possible
    /** Cannot end turn yet; Gamestate is WAITING_FOR_OTHER_DISCARDS_ENDTURN */
    public static final int FORCE_ENDTURN_NOTYET_WAITING   = 1;
    public static final int FORCE_ENDTURN_NONE             = 2;
    public static final int FORCE_ENDTURN_RSRC_RET_UNPLACE = 3;
    public static final int FORCE_ENDTURN_UNPLACE_ROBBER   = 4;
    public static final int FORCE_ENDTURN_RSRC_DISCARD     = 5;
    public static final int FORCE_ENDTURN_RSRC_DISCARD_WAIT = 6;
    public static final int FORCE_ENDTURN_LOST_CHOICE      = 7;
    public static final int FORCE_ENDTURN_MAX              = 7;  // Highest possible

    /**
     * Creates a new SOCForceEndTurnResult object, no resources gained/lost.
     *
     * @param res Result type, from constants in this class
     *            ({@link #FORCE_ENDTURN_UNPLACE_ROBBER, etc.)
     * @throws IllegalArgumentException If res is not in the range
     *            {@link #FORCE_ENDTURN_MIN} to {@link #FORCE_ENDTURN_MAX}.
     */
    public SOCForceEndTurnResult(int res)
    {
        this(res, null);
    }

    /**
     * Creates a new SOCForceEndTurnResult object, with resources gained/lost.
     *
     * @param res Result type, from constants in this class
     *            ({@link #FORCE_ENDTURN_UNPLACE_ROBBER, etc.)
     * @param gainedLost Resources gained (returned to cancel piece
     *            placement) or lost (discarded), or null.
     *            Lost resources are negative values in this set.
     * @throws IllegalArgumentException If res is not in the range
     *            {@link #FORCE_ENDTURN_MIN} to {@link #FORCE_ENDTURN_MAX}.
     */
    public SOCForceEndTurnResult(int res, SOCResourceSet gainedLost)
    {
        if ((res < FORCE_ENDTURN_MIN) || (res > FORCE_ENDTURN_MAX))
            throw new IllegalArgumentException("res out of range: " + res);

        result = res;
        gainLoss = gainedLost;
    }

    /**
     * Creates a new SOCForceEndTurnResult object, with resources gained/lost.
     * If all resource amounts are zero, an empty {@link SOCResourceSet} is created.
     *
     * @param res Result type, from constants in this class
     *            ({@link #FORCE_ENDTURN_UNPLACE_ROBBER, etc.)
     * @param cl  amount of clay resources gained (positive)/lost (negative)
     * @param or  amount of ore resources
     * @param sh  amount of sheep resources
     * @param wh  amount of wheat resources
     * @param wo  amount of wood resources
     * @throws IllegalArgumentException If res is not in the range
     *            {@link #FORCE_ENDTURN_MIN} to {@link #FORCE_ENDTURN_MAX}.
     */
    public SOCForceEndTurnResult(int res, int cl, int or, int sh, int wh, int wo)
    {
        this (res, new SOCResourceSet (cl, or, sh, wh, wo, 0));
    }

    /**
     * Get the force result type.
     * @return Result type, from constants in this class
     *            ({@link #FORCE_ENDTURN_UNPLACE_ROBBER, etc.)
     */
    public int getResult()
    {
        return result;
    }

    /**
     * Get the resources gained (returned to cancel piece
     * placement) or lost (discarded), if any.
     * Lost resources are negative values in this set.
     * @return gained or lost resources, or null
     */
    public SOCResourceSet getResourcesGainedLost()
    {
        return gainLoss;
    }

}
