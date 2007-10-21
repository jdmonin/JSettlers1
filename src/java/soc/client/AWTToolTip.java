/*
 * STATE STATE STATE -- JM -- Bad display flicker when mouse ptr tip is in tooltip box
 *   -> adjust showAtMouse to check that bounding-box
 *   -- Need to grab more socboardpanel.BoardToolTip code
 *   -- Need to re-impl mousemotionlistener to move out of the way
 *
 * $Id: ExpandTooltip.java,v 1.1.1.1 2001/02/07 15:23:49 rtfm Exp $
 *
 * (c)2000 IoS Gesellschaft fr innovative Softwareentwicklung mbH
 * http://www.IoS-Online.de    mailto:info@IoS-Online.de
 * Portions (c)2007 Jeremy D Monin <jeremy@nand.net>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.  
 *
 */

// originally de.ios.framework.gui.ExpandTooltip
// JM - using for jsettlers AWT tooltip

package soc.client;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;

/**
 * A short tooltip for a component.
 */
public class AWTToolTip 
  extends Canvas
  implements MouseListener, MouseMotionListener
{
  
  /** The Window is closed after the mouse has been moved
   *  closeAfterMoveX poinntes horizontally or closeAfterMoveY
   *  points vertically away from the point where it has been clicked
   *  (or it has left the table-area (this also happens when the
   *  mouse enters the expanded-windows))
   */
  public int closeAfterMoveX = 100;
  public int closeAfterMoveY = 20;
    
  private String tip;
    
  protected Component parentComp;
  protected Container mainParentComp;
  protected LayoutManager mainParentLayout;
    
  /** Position of parentComp within painParentComp */
  protected int parentX, parentY;

  private int mousePosAtWinShowX;
  private int mousePosAtWinShowY;

  private boolean autoPopup = false;
  
  public static int OFFSET_X = 10;
  public static int OFFSET_Y = 10;

  /** JM add: want shown? If true, must dynamically add us to parentComp. */
  private boolean wantsShown;
  private boolean isShown;

  /** JM add: Our location within parentComp */
  private int boxX, boxY;
  
  /** JM add: Our size */
  private int boxW, boxH;

  /** The background color of the window */
  static Color bgcol = new Color(240, 240, 180); // light yellow
  /** The foreground color of the window */
  static Color fgcol = Color.BLACK;

  /**
   * Constructor.
   * Constructs a Tooltip which is displayed if there is a click with the right mousebutton on the given component.
   * @param _comp the Component which this Tooltip describes.
   * @param _tip Text to show; single line.
   */
  public AWTToolTip(String _tip, Component _comp)
  {
    if (_tip == null)
      throw new IllegalArgumentException("tip null");
    if (_comp == null)
      throw new IllegalArgumentException("comp null");    
    parentComp = _comp;
    autoPopup = true;
    tip = _tip;
    parentComp.addMouseListener( this );
    parentComp.addMouseMotionListener( this );
    setBackground(bgcol);
    wantsShown = true;
    isShown = false;
    // These are set at mouseenter
    mainParentComp = null;
    mainParentLayout = null;
  }
  
  /**
   * @return the tooltip text.
   */
  public String getTip()
  {
    return tip;
  }

  /**
   * Displays the Tooltip at the given Point(x,y).
   * @param x x-coordinate of the point.
   * @param y y-coordinate of the point.
   JM
  protected void show(int x, int y)
  {
    Point p = parentComp.getLocationOnScreen();
    mousePosAtWinShowX = x;
    mousePosAtWinShowY = y;
    p.translate( mousePosAtWinShowX, mousePosAtWinShowY );
    boxX = p.x + 1;
    boxY = p.y + 1;
    setLocation(boxX, boxY );
    super.show();
  }
  */
  
  /**
   * Show tip at appropriate location when mouse
   * is at (x,y) within mainparent (NOT within parentComp). 
   */
  protected void showAtMouse(int x, int y)
  {
      if (mainParentComp == null)
          return;  // Not showing
      
      boxX = OFFSET_X + x;
      boxY = OFFSET_Y + y;
      
      // Goals:
      // - Don't have it extend off the screen
      // - Mouse pointer tip should not be within our bounding box (flickers)
      
      if ( ((x >= boxX) && (x < (boxX + boxW))) 
          || (mainParentComp.getSize().width <= ( boxX + boxW )) )
      {
          // Try to float it to left of mouse pointer
          boxX = x - boxW - OFFSET_X;
          if (boxX < 0)
          {
              // Not enough room, just place flush against right-hand side
              boxX = mainParentComp.getSize().width - boxW;
          }
      }    
      if ( ((y >= boxY) && (y < (boxY + boxH))) 
              || (mainParentComp.getSize().height <= ( boxY + boxH )) )
          {
              // Try to float it to above mouse pointer
              boxY = y - boxH - OFFSET_Y;
              if (boxY < 0)
              {
                  // Not enough room, just place flush against top
                  boxY = 0;
              }
          }    
      setLocation(boxX, boxY);

  }

  public void update(Graphics g)
  {
      paint(g);
  }
  
  public void paint(Graphics g)
  {
    if (! (wantsShown && isShown))
        return;
    g.setColor(getBackground());
    g.fillRect(0, 0, boxW-1, boxH-1);
    g.setColor(fgcol);
    g.drawRect(0, 0, boxW-1, boxH-1);
    g.drawString(tip, 2, boxH -3);
  }

  /**
   * Creates a Panel containing the value.
   */
  protected Panel createValuePanel( String value )
  {
    Panel p = new Panel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    p.setLayout( gbl );
    gbc.gridwidth = gbc.REMAINDER;
    gbc.anchor = gbc.NORTHWEST;
    String excerpt = "";
    for( int i = 0; i < value.length(); i++ ) {
      if( value.charAt(i) != '\n' )
	excerpt += value.charAt(i);
      else {
	Label l = new Label( excerpt );
	l.setForeground( fgcol );
	gbl.setConstraints( l, gbc );
	p.add( l );
	excerpt = "";
      }
    }
    Label l = new Label( excerpt );
    l.setForeground( fgcol );
    gbl.setConstraints( l, gbc );
    p.add( l );
    return p;
  }
  
  protected void removeFromParent()
  {
    if (isShown)
    {
      mainParentComp.remove(0);
      mainParentComp.setLayout(mainParentLayout);
      mainParentComp.validate();
      mainParentComp = null;
      isShown = false;
    }
  }
  
  /** x
   * JM TODO docs
   * 
   * @param x Mouse position within parentComp when adding
   *      (NOT within mainparent) 
   */
  protected void addToParent(int x, int y)
  {
    if (! wantsShown)
        return;
    if (mainParentComp != null)
        return;

    mainParentComp = getParentContainer(parentComp);
    mainParentLayout = mainParentComp.getLayout();
    mainParentComp.setLayout(null);  // Allow free placement
    
    FontMetrics fm = getFontMetrics(parentComp.getFont());
    boxW = fm.stringWidth(tip) + 6;
    boxH = fm.getHeight();
    setSize(boxW, boxH);

    parentX = parentComp.getLocationOnScreen().x - mainParentComp.getLocationOnScreen().x;
    parentY = parentComp.getLocationOnScreen().y - mainParentComp.getLocationOnScreen().y; 
    showAtMouse(x + parentX, y + parentY);          

    mainParentComp.add(this, 0);
    mainParentComp.validate();
    isShown = true;
    repaint();
  }

  /**
   * Gets the top-level container of c.
   * @param c The Component.
   * @return The parent-frame or applet, or null.
   */
  public static Container getParentContainer( Component c )
  {
    while (! ((c instanceof Frame) || c instanceof Applet))
    {
      c = c.getParent();
      if (c == null)
        throw new IllegalStateException("Assert failed, parent should not be null"); 
    }
    return (Container) c;
  }

  /**
   * hides the tooltip.
   */
  private void hideWindow()
  {
    wantsShown = false;
    removeFromParent();
  }

  /**
   * destroys the tooltip.
   */
  public void destroy()
  {
    wantsShown = false;
    removeFromParent();
  }
    
  /**
   * MouseListener-Methods
   */
  public void mouseClicked( MouseEvent e ) {
      removeFromParent();
  }
    
  public void mouseExited( MouseEvent e) {
    removeFromParent();
  }

  public void mouseEntered( MouseEvent e)
  {
    if (autoPopup)
    {
      addToParent(e.getX(), e.getY());
    }
  }
  public void mousePressed( MouseEvent e)
  {
    removeFromParent();
  }
  public void mouseReleased( MouseEvent e) {}

  /**
   * MouseMotionListener-Methods
   */
  
  /**
   * Must keep out of the way of the mouse pointer.
   * On some Win32, flickers if (x,y) of mouse is in our bounding box.
   */
  public void mouseMoved( MouseEvent e)
  {
    int x = e.getX();
    int y = e.getY();
    if ( java.lang.Math.abs( x - mousePosAtWinShowX )> closeAfterMoveX ||
     java.lang.Math.abs( y - mousePosAtWinShowY )> closeAfterMoveY)
    {
      removeFromParent();
    } else {
      showAtMouse(x + parentX, y + parentY);
    }
  }

  public void mouseDragged( MouseEvent e) {}

}

/*
 * $Log: ExpandTooltip.java,v $
 * Revision 1.1.1.1  2001/02/07 15:23:49  rtfm
 * initial
 *
 * Revision 1.5  2000/01/27 13:35:29  ch
 * Added to methods
 *
 * Revision 1.4  2000/01/20 15:19:40  ch
 * de/ios/framework/gui/ILNField.java added; Bugfix in ExpandTooltip.java
 *
 * Revision 1.3  1999/12/29 10:49:00  mm
 * Bugfixing: berprfung der anzuzeigendenen Werte; anstatt Werte zu Strings zu
 * casten wird jetzt toString() aufgerufen.
 *
 * Revision 1.2  1999/12/27 14:10:01  ch
 * Bugfis in ExpandTooltip.java
 *
 * Revision 1.1  1999/12/27 13:55:11  ch
 * de.ios.framework.gui.ExpandTooltip added
 *
 */

