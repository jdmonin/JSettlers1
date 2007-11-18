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

import soc.game.SOCGame;
import soc.game.SOCPlayer;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;


/**
 * This is a component that can display a face.
 * When you click on the face, it changes to another face.
 *
 * @author Robert S. Thomas
 */
public class SOCFaceButton extends Canvas
{
    public static final int DEFAULT_FACE = 1;  // Human face # 1 (face1.gif)

    private static final String IMAGEDIR = "/soc/client/images";

    /**
     * number of /numbered/ face images, /plus 1/ for indexing
     */
    public static final int NUM_FACES = 74;

    /**
     * number of robot faces, which are separately numbered.
     * Robot face 0 is just robot.gif, otherwise robot1.gif, robot2.gif, etc.
     * Internally, robot faces are negative faceIds.
     */
    public static final int NUM_ROBOT_FACES = 2;

    /** Shared images */
    private static Image[] images;
    private static Image[] robotImages;

    /** For status in drawFace */
    private static MediaTracker tracker;

    /**
     * Human face images are positive numbers, 1-based (range 1 to NUM_FACES).
     * Robot face images are negative, 0-based (range -(NUM_ROBOT_FACES-1) to 0).
     */
    private int currentImageNum = DEFAULT_FACE;
    private int panelx;
    private int panely;
    private int pNumber;
    private SOCGame game;
    private SOCPlayerClient client;

    /**
     * offscreen buffer
     */
    private Image buffer;

    private static synchronized void loadImages(Component c)
    {
        if (images == null)
        {
            tracker = new MediaTracker(c);
            Toolkit tk = c.getToolkit();
            Class clazz = c.getClass();
        
            images = new Image[NUM_FACES];
            robotImages = new Image[NUM_ROBOT_FACES];

            /**
             * load the images
             */
            robotImages[0] = tk.getImage(clazz.getResource(IMAGEDIR + "/robot.gif"));
            tracker.addImage(robotImages[0], 0);
            
            for (int i = 1; i < NUM_FACES; i++)
            {
                images[i] = tk.getImage(clazz.getResource(IMAGEDIR + "/face" + i + ".gif"));
                tracker.addImage(images[i], 0);
            }

            for (int i = 1; i < NUM_ROBOT_FACES; i++)
            {
                // Client possibly only has robot.gif.
                // Check getResource vs null, and use MediaTracker;
                // drawFace can check tracker.statusID vs MediaTracker.COMPLETE.
                URL imgSrc = clazz.getResource(IMAGEDIR + "/robot" + i + ".gif");
                if (imgSrc != null)
                {
                    robotImages[i] = tk.getImage(imgSrc);
                    tracker.addImage(robotImages[i], i);
                }
            }

            try
            {
                tracker.waitForID(0);
            }
            catch (InterruptedException e) {}

            if (tracker.isErrorID(0))
            {
                System.out.println("Error loading Face images");
            }
        }
    }
    
    /**
     * create a new SOCFaceButton
     *
     * @param pi  the interface that this button is attached to
     * @param pn  the number of the player that owns this button
     */
    public SOCFaceButton(SOCPlayerInterface pi, int pn)
    {
        super();

        client = pi.getClient();
        game = pi.getGame();
        pNumber = pn;

        setBackground(pi.getPlayerColor(pn));

        panelx = 40;
        panely = 40;

        // load the static images
        loadImages(this);

        this.addMouseListener(new MyMouseAdapter());
    }

    /**
     * set which image is shown
     *
     * @param id  the id for the image
     */
    public void setFace(int id)
    {
        if (id >= NUM_FACES)            
            id = DEFAULT_FACE;
        else if (id <= (-NUM_ROBOT_FACES))
            id = 0;
        currentImageNum = id;
        repaint();
    }

    /**
     * Reset to the default face.
     */
    public void setDefaultFace()
    {
        setFace(DEFAULT_FACE);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Dimension getPreferedSize()
    {
        return new Dimension(panelx, panely);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Dimension getMinimumSize()
    {
        return new Dimension(panelx, panely);
    }

    /**
     * Redraw the board using double buffering. Don't call this directly, use
     * {@link Component#repaint()} instead.
     */
    public void paint(Graphics g)
    {
        if (buffer == null)
        {
            buffer = this.createImage(panelx, panely);
        }
        drawFace(buffer.getGraphics());
        buffer.flush();
        g.drawImage(buffer, 0, 0, this);
    }

    /**
     * Overriden so the peer isn't painted, which clears background. Don't call
     * this directly, use {@link Component#repaint()} instead.
     */
    public void update(Graphics g)
    {
        paint(g);
    }

    /**
     * draw the face
     */
    private void drawFace(Graphics g)
    {
        Image fimage;

        /**
         * before drawing, ensure this face number is loaded
         */
        int findex;
        if (currentImageNum > 0)
        {
            findex = currentImageNum;
            if ((findex >= NUM_FACES) || (null == images[findex]))
            {
                findex = DEFAULT_FACE;
                currentImageNum = findex;
            }
            fimage = images[findex];
        }
        else
        {
            findex = -currentImageNum;
            if ((findex >= NUM_ROBOT_FACES) || (null == robotImages[findex])
                || (0 != (tracker.statusID(findex, false) & (MediaTracker.ABORTED | MediaTracker.ERRORED))))
            {
                findex = 0;
                currentImageNum = -findex;
            }
            fimage = robotImages[findex];
        }

        g.clearRect(0, 0, WIDTH, HEIGHT);
        g.drawImage(fimage, 0, 0, getBackground(), this);
    }

    /*********************************
     * Handle Events
     *********************************/
    private class MyMouseAdapter extends MouseAdapter
    {
        public void mousePressed(MouseEvent evt)
        {
            /**
             * only change the face if it's the owners button
             */
            if (game.getPlayer(pNumber).getName().equals(client.getNickname()))
            {
                if (evt.getX() < 20)
                {
                    // if the click is on the left side, decrease the number
                    currentImageNum--;
                    
                    if (currentImageNum <= 0)
                    {
                        currentImageNum = NUM_FACES - 1;
                    }
                }
                else
                {
                    // if the click is on the right side, increase the number
                    currentImageNum++;

                    if (currentImageNum == NUM_FACES)
                    {
                        currentImageNum = 1;
                    }
                }
                
                client.changeFace(game, currentImageNum);
                repaint();
            }
        }
    }
}
