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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import soc.game.SOCGame;


/**
 * Popup window for the user to browse and choose a face icon.
 *
 * To adjust size, set FaceChooserList.rowFacesWidth and .faceRowsHeight .
 * @see soc.client.FaceChooserFrame.FaceChooserList#rowFacesWidth
 * @see soc.client.FaceChooserFrame.FaceChooserList#faceRowsHeight
 * @see soc.client.SOCFaceButton
 *
 * @author Jeremy D Monin <jeremy@nand.net>
 */
public class FaceChooserFrame extends Frame implements ActionListener, WindowListener, KeyListener
{
    /** Face button that launched us.  Passed to constructor, not null. */
    protected SOCFaceButton fb;

    /** Player client.  Passed to constructor, not null */
    protected SOCPlayerClient pcli;

    /** Player interface.  Passed to constructor, not null */
    protected SOCPlayerInterface pi;

    /** Player number. Needed for bg color. */
    protected int pNumber;

    /** Width,height of one face, in pixels. Assumes icon is square. */
    protected int faceWidthPx;

    /** Scrolling choice of faces */
    protected FaceChooserList fcl;

    /** Button for confirm change */
    protected Button changeFaceBut;

    /** Button for cancel */
    protected Button cancelBut;

    /** Is this still visible and interactive? (vs already dismissed)
     * 
     * @see #isStillAvailable()
     */
    private boolean stillAvailable;

    /**
     * Creates a new FaceChooserFrame.
     *
     * @param fbutton  Face button in player's handpanel
     * @param cli      Player client interface
     * @param gamePI   Current game's player interface
     * @param pnum     Player number in game
     * @param faceWidth Width and height of one face button, in pixels. Assumes icon is square.
     *
     * @throws IllegalArgumentException If fbutton, cli, or gamePI is null, or faceWidth is 0 or negative,
     *    or pnum is negative or more than SOCGame.MAXPLAYERS.
     */
    public FaceChooserFrame(SOCFaceButton fbutton, SOCPlayerClient cli,
            SOCPlayerInterface gamePI, int pnum, int faceID, int faceWidth)
        throws IllegalArgumentException
    {
        super("Choose Face Icon: "
                + gamePI.getGame().getName() + " ["
                + cli.getNickname() + "]");

        if (fbutton == null)
            throw new IllegalArgumentException("fbutton cannot be null");
        if (cli == null)
            throw new IllegalArgumentException("cli cannot be null");
        if (gamePI == null)
            throw new IllegalArgumentException("gamePI cannot be null");
        if ((pnum < 0) || (pnum >= SOCGame.MAXPLAYERS))
            throw new IllegalArgumentException("pnum out of range: " + pnum);
    	if (faceWidth <= 0)
            throw new IllegalArgumentException("faceWidth must be positive, not " + faceWidth);

        fb = fbutton;
        pcli = cli;
        pi = gamePI;
        pNumber = pnum;
        faceWidthPx = faceWidth;
        stillAvailable = true;

        setBackground(new Color(255, 230, 162));  // Actual face-icon backgrounds will match player.
        setForeground(Color.black);
        setFont(new Font("Dialog", Font.PLAIN, 12));

        changeFaceBut = new Button("Change");
        cancelBut = new Button("Cancel");
        setLayout (new BorderLayout());

        Label msg = new Label("Choose your face icon.", Label.LEFT);
        add(msg, BorderLayout.NORTH);

    	fcl = new FaceChooserList(this, faceID);
        add(fcl, BorderLayout.CENTER);

        setLocation(150, 100);

        Panel pBtns = new Panel();
        pBtns.setLayout(new FlowLayout(FlowLayout.CENTER));

        pBtns.add(changeFaceBut);
        changeFaceBut.addActionListener(this);

        pBtns.add(cancelBut);
        cancelBut.addActionListener(this);

        add(pBtns, BorderLayout.SOUTH);

        // Now that we've added buttons to the dialog layout,
        // we can get their font and adjust style of default button.
        AskDialog.styleAsDefault(changeFaceBut);            

        addWindowListener(this);  // To handle close-button
        addKeyListener(this);     // To handle Enter, Esc keys.
        changeFaceBut.addKeyListener(this);
        cancelBut.addKeyListener(this);
    }

    /**
     * Face selected (clicked) by user.  If already-selected, and player has chosen
     * a new face in this window, consider double-click: change face and close window.
     *
     * @param id  face ID
     * @param alreadySelected  Was the face currently selected, when clicked? 
     */
    public void selectFace(int id, boolean alreadySelected)
    {
        if (! alreadySelected)
            fcl.selectFace(id);
        else if (id != fcl.initialFaceId)
        {
            dispose();
            changeButtonChosen();
        }
    }

    /**
     * @return Player color in game (background color for face icons)
     */
    public Color getPlayerColor()
    {
        return pi.getPlayerColor(pNumber);
    }

    /**
     * Is this chooser still visible and interactive?
     * 
     * @return True if still interactive (vs already dismissed).
     */
    public boolean isStillAvailable()
    {
        return stillAvailable;
    }

    /**
     * Dispose of this window. Overrides to clear stillAvailable flag,
     * and call faceButton.clearFacePopupPreviousChooser.
     */
    public void dispose()
    {
        stillAvailable = false;
        fb.clearFacePopupPreviousChooser();
        super.dispose();
    }

    /**
     * set focus to the default button (if any).
     */
    protected void checkSizeAndFocus()
    {
        changeFaceBut.requestFocus();
    }

    /**
     * Change or Cancel button has been chosen by the user.
     * Call changeButtonChosen or cancelButtonChosen, and dispose of this dialog.
     */
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            Object target = e.getSource();

            if (target == changeFaceBut)
            {
                dispose();
                changeButtonChosen();  // <--- Callback for button 1 ---
            }
            else if (target == cancelBut)
            {
                dispose();
                cancelButtonChosen();  // <--- Callback for button 2 ---
            }
        } catch (Throwable th) {
            pi.chatPrintStackTrace(th);
        }
    }

    /**
     * Change-face button has been chosen by the user. React accordingly.
     * actionPerformed has already called dialog.dispose().
     */
    public void changeButtonChosen()
    {
        pcli.changeFace(pi.getGame(), fcl.currentFaceId);
    }

    /**
     * Cancel button has been chosen by the user. React accordingly.
     * Also called if user closes window or hits Escape key.
     * actionPerformed has already called dialog.dispose().
     */
    public void cancelButtonChosen() { }

    /**
     * Dialog close requested by user. Dispose and call windowCloseChosen.
     */
    public void windowClosing(WindowEvent e)
    {
        dispose();
        cancelButtonChosen();  // <--- Callback for close/ESC ---
    }

    /** Window is appearing - check the size and the default button keyboard focus */
    public void windowOpened(WindowEvent e)
    {
        checkSizeAndFocus();
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

    /** Handle Enter or Esc key */
    public void keyPressed(KeyEvent e)
    {
        if (e.isConsumed())
            return;

        switch (e.getKeyCode())
        {
        case KeyEvent.VK_ENTER:
    	    dispose();                
    	    e.consume();
    	    changeButtonChosen();
            break;

        case KeyEvent.VK_CANCEL:
        case KeyEvent.VK_ESCAPE:
            dispose();                
            e.consume();
            cancelButtonChosen();
            break;
        }
    }

    /** Stub required by KeyListener */
    public void keyReleased(KeyEvent arg0) { }

    /** Stub required by KeyListener */
    public void keyTyped(KeyEvent arg0) { }

    /**
     * FaceChooserList holds face icons (in rows and columns) and an optional scrollbar. 
     * Custom layout.
     */
    protected static class FaceChooserList extends Container
        implements AdjustmentListener
    {
    	/**
    	 *  How many faces per row?  Default 7.
    	 *  Do not change after creating an instance.
    	 */
    	protected static int rowFacesWidth = 7;

    	/**
    	 *  How many rows to show?  Default 6.
    	 *  Do not change after creating an instance.
         *  If all faces (SOCFaceButton.NUM_FACES) fit in fewer than
         *  faceRowsHeight rows, the first instance's constructor will
         *  reduce faceRowsHeight to the proper value.
    	 */
    	protected static int faceRowsHeight = 6;

        protected FaceChooserFrame fcf;
    	private int currentRow;     // upper-left row #, first row is 0
    	private int currentOffset;  // upper-left, from faceid==0
    	private int rowCount;       // how many rows total
    	private int rowWidthPx;     // how wide (pixels)
    	private int currentFaceId;  // currently selected faceId in this window
        private int initialFaceId;  // faceId of player in game

    	/**
         * Will contain all faces.
         * Length is rowCount.  Each row contains rowFacesWidth faces. 
         * Some elements initially null, if the scrollbar is needed
         * (if rowCount > faceRowsHeight).
         * Contents are references to same objects as in visibleFaceGrid.
    	 */
        private FaceChooserRow[] faceGrid;

        /**
         * Contains only visible faces (based on scrollbar).
         * Length is faceRowsHeight.  Each row contains rowFacesWidth faces.
         * Contents are references to same objects as in faceGrid.
         */
        private FaceChooserRow[] visibleFaceGrid;

    	private boolean needsScroll;  // Scrollbar required?
        private Scrollbar faceSB;

        /** Desired size (visible size inside of insets; not incl scrollW) **/
        protected int wantW, wantH;

        /** Desired size */
        protected Dimension wantSize;

        /** Padding beyond desired size; not known until doLayout() **/
        protected int padW, padH;

        /** faceSB pixel width; not known until doLayout */
        protected int scrollW;

    	protected FaceChooserList(FaceChooserFrame fcf, int selectedFaceId)
    	{
            this.fcf = fcf;
            initialFaceId = selectedFaceId;
    	    currentFaceId = selectedFaceId;

    	    // Padding between faces is done by SOCFaceButton.FACE_BORDERED_WIDTH_PX.
            rowWidthPx = fcf.faceWidthPx * rowFacesWidth;  
    
    	    rowCount = (int) Math.ceil
    	        ((SOCFaceButton.NUM_FACES - 1) / (float) rowFacesWidth);
            if (rowCount < faceRowsHeight)
                faceRowsHeight = rowCount;  // Reduce if number of "visible rows" would be too many.

            // If possible, place the selectedFaceId in the
            // middle row of the frame.
            currentRow = ((selectedFaceId - 1) / rowFacesWidth) - (faceRowsHeight / 2);
            if (currentRow < 0)
            {
                // Near the top
                currentRow = 0;
            }
            else if (currentRow + faceRowsHeight >= rowCount)
            {
                // Near the end
                currentRow = rowCount - faceRowsHeight;
            }
            currentOffset = 1 + (currentRow * rowFacesWidth);  // id's 0 and below are for robot

    	    needsScroll = (rowCount > faceRowsHeight);

    	    faceGrid = new FaceChooserRow[rowCount];
            visibleFaceGrid = new FaceChooserRow[faceRowsHeight];

    	    setLayout(null);  // Custom due to visibleFaceGrid - see doLayout()

    	    int nextId = currentOffset;
    	    for (int r = currentRow, visR = 0; visR < faceRowsHeight; ++r, ++visR)
    	    {
                FaceChooserRow fcr = new FaceChooserRow(nextId);
                    // FaceChooserRow constructor will also set the
                    // hilight if current face is within its row.
                faceGrid[r] = fcr;
                visibleFaceGrid[visR] = fcr;
    		    add(fcr);
    		    nextId += rowFacesWidth;
    	    }
            if (needsScroll)
            {
                faceSB = new Scrollbar(Scrollbar.VERTICAL, currentRow,
                        /* number-rows-visible */ faceRowsHeight,
                        0, rowCount );
                // Range 0 to rowCount per API note: "actual maximum value is max minus visible"
                add(faceSB);
                faceSB.addAdjustmentListener(this);
                faceSB.addKeyListener(fcf);  // Handle Enter, Esc keys on window's behalf
            }

            wantW = rowFacesWidth * SOCFaceButton.FACE_BORDERED_WIDTH_PX;
            wantH = faceRowsHeight * SOCFaceButton.FACE_BORDERED_WIDTH_PX;
            scrollW = 0;  // unknown before is visible
            padW = 10;  padH = 30;  // assumes. Will get actual at doLayout.
            wantSize = new Dimension (wantW + padW, wantH + padH);
    	}

        /**
         * Face chosen (clicked), by user or otherwise.
         * Select it and show the hilight border.
         * If the new face isn't currently visible, scroll to show it.
         *
         * @param id  Face ID to select
         *
         * @throws IllegalArgumentException if id <= 0 or id >= SOCFaceButton.NUM_FACES
         */
        public void selectFace(int id)
        {
            if ((id <= 0) || (id >= SOCFaceButton.NUM_FACES))
                throw new IllegalArgumentException("id not within range: " + id);

            int prevFaceId = currentFaceId;
            int r;

            // Clear hilight of prev face-id
            r = (prevFaceId - 1) / rowFacesWidth;
            faceGrid[r].setFaceHilightBorder(prevFaceId, false);

            // Set hilight of new face-id
            r = (id - 1) / rowFacesWidth;
            scrollToRow(r);
            faceGrid[r].setFaceHilightBorder(id, true);

            currentFaceId = id;
        }

        /** Ensure this row of faces is visible.  Calls repaint if needed.
         *  Number of rows visible at a time is faceRowsHeight.
         *  
         * @param r  Row number, counting from 0.
         *   The row number can be determined from the faceID
         *   by r = (faceId - 1) / rowFacesWidth.
         *
         * @throws IllegalArgumentException if newRow < 0 or newRow >= rowCount
         */
        public void scrollToRow(int newRow)
        {
            if ((newRow < 0) || (newRow >= rowCount))
                throw new IllegalArgumentException
                ("newRow not in range (0 to " + (rowCount-1) + "): " + newRow);
            if ((newRow >= currentRow) && (newRow < (currentRow + faceRowsHeight)))
            {
                return;  // <--- Early return: Already showing ---
            }

            boolean createdRows = false;  // Any objects instantiated? (Need to pack their layout)
            int numNewRows;    // How many not currently showing?
            int newCurRow;     // New first-showing row number
            int newCurOffset;  // new upper-left corner face ID            

            if (newRow < currentRow)
            {
                numNewRows = currentRow - newRow;
                newCurRow = newRow;
            }
            else
            {
                numNewRows = 1 + (newRow - (currentRow + faceRowsHeight));
                newCurRow = newRow - faceRowsHeight + 1;
            }
            newCurOffset = newCurRow * rowFacesWidth + 1;
            if (numNewRows > faceRowsHeight)
                numNewRows = faceRowsHeight;

            int r;
            if ((numNewRows == faceRowsHeight) || (newRow < currentRow))
            {
                // Scroll up, or completely replace visible.
                if (numNewRows == faceRowsHeight)
                {
                    // Completely replace current visible face grid.
                    for (r = faceRowsHeight - 1; r >= 0; --r)
                    {
                        visibleFaceGrid[r].setVisible(false);
                        visibleFaceGrid[r] = null;
                    }
                } else {
                    // newRow < currentRow:
                    // Remove current bottom, scroll up
                    for (r = faceRowsHeight - numNewRows; r < faceRowsHeight; ++r)
                    {
                        visibleFaceGrid[r].setVisible(false);
                        visibleFaceGrid[r] = null;                   
                    }
                    for (r = faceRowsHeight - numNewRows - 1; r >= 0; --r)
                        visibleFaceGrid[r + numNewRows] = visibleFaceGrid[r];
                }

                // Add newly-visible
                int nextId = newCurOffset;  // face ID to add
                int visR = 0;  // Visual row number to add
                // in this loop, r = row number in faceGrid to add
                for (r = newRow; r < (newRow + numNewRows); ++r, ++visR)
                {
                    if (faceGrid[r] == null)
                    {
                        faceGrid[r] = new FaceChooserRow(nextId);
                        add(faceGrid[r]);
                        createdRows = true;
                    }
                    visibleFaceGrid[visR] = faceGrid[r];
                    visibleFaceGrid[visR].setVisible(true);
                    nextId += rowFacesWidth;
                }
            }
            else  // (newRow >= currentRow + faceRowsHeight)
            {
                // Remove current top, scroll down
                for (r = 0; r < numNewRows; ++r)
                {
                    visibleFaceGrid[r].setVisible(false);
                    visibleFaceGrid[r] = null;                   
                }
                for (r = 0; r < faceRowsHeight - numNewRows; ++r)
                    visibleFaceGrid[r] = visibleFaceGrid[r + numNewRows];

                // Add newly-visible
                int visR = faceRowsHeight - numNewRows;   // Visual row number to add
                int nextId = newCurOffset + (visR * rowFacesWidth);  // face ID to add
                r = newCurRow + visR;   // Row number in faceGrid to add
                for ( ; visR < faceRowsHeight ; ++r, ++visR )
                {
                    if (faceGrid[r] == null)
                    {
                        faceGrid[r] = new FaceChooserRow(nextId);
                        add(faceGrid[r]);
                        createdRows = true;
                    }
                    visibleFaceGrid[visR] = faceGrid[r];
                    visibleFaceGrid[visR].setVisible(true);
                    nextId += rowFacesWidth;
                }
            }
            currentRow = newCurRow;
            currentOffset = newCurOffset;
            if (createdRows)
                fcf.pack();
            doLayout();  // for setLocation, setSize of visibleFaceGrid members
            repaint();
            if (faceSB != null)
                faceSB.setValue(newCurRow);  // Update scrollbar if needed (we don't know our caller)
        }

        /**
         * Now that insets and scrollbar size are known, check our padding.
         *
         * @param  i  Insets
         * @return True if dimensions were updated and setSize was called.
         */
        protected boolean checkInsetsPadding(Insets i)
        {
            int iw = (i.left + i.right);
            int ih = (i.top + i.bottom);
            int sw;
            if (needsScroll)
            {
                sw = faceSB.getWidth();
                if (sw == 0)
                {
                    sw = faceSB.getPreferredSize().width;
                    if (sw == 0)
                        sw = 12;  // Guessing, just to not be zero
                }
            }
            else
                sw = 0;
            if ((padW < iw) || (padH < ih) || (scrollW < sw))
            {
                padW = iw;
                padH = ih;
                scrollW = sw;
                wantSize = new Dimension (wantW + scrollW + padW, wantH + padH);
                setSize (wantSize);
                fcf.pack();
                return true;
            }
            return false;
        }

        /**
         *  Custom layout for this list, which makes things easier
         *  because visibleFaceGrid changes frequently.
         */
        public void doLayout()
        {
            Insets i = getInsets();
            int x = i.left;
            int y = i.top;
            int width = getSize().width - i.left - i.right;
            int height = getSize().height - i.top - i.bottom;

            if (checkInsetsPadding(i))
            {
                width = getSize().width - padW;
                height = getSize().height - padH;                
            }

            if (needsScroll)
            {
                if (scrollW == 0)
                {
                    IllegalStateException e = new IllegalStateException("scrollW==0");
                    fcf.pi.chatPrintStackTrace(e);
                    scrollW = faceSB.getPreferredSize().width;
                    if (scrollW == 0)
                        scrollW = 12;
                    wantSize = new Dimension (wantW + scrollW + padW, wantH + padH);
                    setSize (wantSize);
                    fcf.pack();
                    width = getSize().width - padW;
                    height = getSize().height - padH;                
                }
                faceSB.setLocation(x + width - scrollW, y);
                faceSB.setSize(scrollW, height);
            }
            
            int rowHeightPx = SOCFaceButton.FACE_BORDERED_WIDTH_PX;
            for (int r = 0; r < faceRowsHeight; ++r)
            {
                visibleFaceGrid[r].setLocation(x, y);
                visibleFaceGrid[r].setSize(wantW, rowHeightPx);
                y += rowHeightPx;
            }
        }

        public Dimension getMinimumSize() { return wantSize; }

        public Dimension getPreferredSize() { return wantSize; }

        /**
         * Within FaceChooserList, one row of faces.
         * Takes its width (number of faces) from FaceChooserList.rowFacesWidth.
         * 
         */
        private class FaceChooserRow extends Container
        {
            private int startFaceId;
            
            /** Will not go past SOCFaceButton.NUM_FACES */
            private SOCFaceButton[] faces;

            /**
             * If our FaceChooserList.currentFaceId is within our row, calls that
             * face id's setHilightBorder when adding it.
             * If we're at the end of the range, some gridlayout members may be left blank.
             * 
             * @param startId  Starting face ID (ID of first face in row)
             * @throws IllegalArgumentException if startId<=0 or startId >= SOCFaceButton.NUM_FACES
             */
            public FaceChooserRow (int startId)
                throws IllegalArgumentException
            {
                if ((startId <= 0) || (startId >= SOCFaceButton.NUM_FACES))
                    throw new IllegalArgumentException("startId not within range: " + startId);

                startFaceId = startId;

                int numFaces = FaceChooserList.rowFacesWidth;
                if ((startId + numFaces) >= SOCFaceButton.NUM_FACES)
                    numFaces = SOCFaceButton.NUM_FACES - startId;

                faces = new SOCFaceButton[numFaces];  // At least 1 due to startId check above

                GridLayout glay = new GridLayout(1, FaceChooserList.rowFacesWidth, 0, 0);
                setLayout (glay);

                for (int i = 0; i < numFaces; ++i)
                {
                    SOCFaceButton fb = new SOCFaceButton(fcf.pi, fcf, startId + i);
                    faces[i] = fb;
                    if (startId + i == currentFaceId)
                        fb.setHilightBorder(true);
                    fb.addKeyListener(fcf);
                    add (fb);
                }
                if (numFaces < FaceChooserList.rowFacesWidth)
                {
                    // The grid will be left-justified by FaceChooserList's layout.
                    // Fill blanks with the right background color.
                    for (int i = numFaces; i < FaceChooserList.rowFacesWidth; ++i)
                    {
                        Label la = new Label("");
                        la.setBackground(faces[0].getBackground());
                        add (la);
                    }
                }
            }

            /**
             * If this faceId is within our row, call its setHilightBorder.
             * 
             * @param faceId     Face ID - If outside our range, do nothing.
             * @param borderSet  Set or clear?
             *
             * @see soc.client.SOCFaceButton#setHilightBorder(boolean)
             */
            public void setFaceHilightBorder (int faceId, boolean borderSet)
            {
                faceId = faceId - startFaceId;
                if ((faceId < 0) || (faceId >= faces.length))
                    return;
                faces[faceId].setHilightBorder(borderSet);
            }

            /**
             * setVisible - overrides to call each face's setVisible
             * 
             * @param vis  Make visible?
             */
            public void setVisible (boolean vis)
            {
                for (int i = faces.length - 1; i >= 0; --i)
                    faces[i].setVisible(vis);
                super.setVisible(vis);
            }

        }  /* inner class FaceChooserRow */

        /**
         * Scrollbar adjust when changed
         */
        public void adjustmentValueChanged(AdjustmentEvent e)
        {
            if (e.getSource() != faceSB)
                return;
            int i = e.getValue();
            scrollToRow(i);  // Top of window
            scrollToRow(i + faceRowsHeight - 1);  // Bottom of window 
        }

    }  /* static nested class FaceChooserList */

}
