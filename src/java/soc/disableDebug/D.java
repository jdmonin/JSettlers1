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
package soc.disableDebug;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision: 1.1 $
 */
public class D
{
    static public final boolean ebugOn = false;
    static private boolean enabled = true;

    /**
     * DOCUMENT ME!
     */
    public static final void ebug_enable() {}

    /**
     * DOCUMENT ME!
     */
    public static final void ebug_disable() {}

    /**
     * Is debug currently enabled?
     * */
    public static final boolean ebugIsEnabled()
    {
        return false;
    }

   /**
     * DOCUMENT ME!
     *
     * @param text DOCUMENT ME!
     */
    public static final void ebugPrintln(String text) {}

    /**
     * DOCUMENT ME!
     */
    public static final void ebugPrintln() {}

    /**
     * If debug is enabled, print the stack trace of this exception
     * @param ex Exception or other Throwable
     * @param prefixMsg Message for {@link #ebugPrintln(String)} above the exception,
     *                  or null
     */
    public static final void ebugPrintStackTrace(Throwable ex, String prefixMsg) {}

    /**
     * DOCUMENT ME!
     *
     * @param text DOCUMENT ME!
     */
    public static final void ebugPrint(String text) {}
}
