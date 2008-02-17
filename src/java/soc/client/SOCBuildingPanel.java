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
import soc.game.SOCPlayingPiece;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This class is a panel that shows how much it costs
 * to build things, and it allows the player to build.
 */
public class SOCBuildingPanel extends Panel implements ActionListener
{
    static final String ROAD = "road";
    static final String STLMT = "stlmt";
    static final String CITY = "city";
    static final String CARD = "card";
    Label title;
    Button roadBut;
    Button settlementBut;
    Button cityBut;
    Button cardBut;
    Label roadT;
    Label roadV;
    Label roadC;
    ColorSquare roadWood;
    ColorSquare roadClay;
    Label settlementT;
    Label settlementV;
    Label settlementC;
    ColorSquare settlementWood;
    ColorSquare settlementClay;
    ColorSquare settlementWheat;
    ColorSquare settlementSheep;
    Label cityT;
    Label cityV;
    Label cityC;
    ColorSquare cityWheat;
    ColorSquare cityOre;
    Label cardT;
    Label cardV;
    Label cardC;
    Label cardCountLab;
    ColorSquare cardWheat;
    ColorSquare cardSheep;
    ColorSquare cardOre;
    ColorSquare cardCount;
    SOCPlayerInterface pi;

    /**
     * Client's player data.  Initially null; call setPlayer once seat is chosen.
     *
     * @see #setPlayer()
     */
    SOCPlayer player;

    /**
     * make a new building panel
     *
     * @param pi  the player interface that this panel is in
     */
    public SOCBuildingPanel(SOCPlayerInterface pi)
    {
        super();
        setLayout(null);

        this.player = null;
        this.pi = pi;

        setBackground(new Color(156, 179, 94));
        setForeground(Color.black);
        setFont(new Font("Helvetica", Font.PLAIN, 10));

        /*
           title = new Label("Building Costs:");
           title.setAlignment(Label.CENTER);
           add(title);
         */
        roadT = new Label("Road: ");
        add(roadT);
        roadV = new Label("0 VP  (longest road = 2 VP) ");
        roadV.setAlignment(Label.LEFT);
        add(roadV);
        roadC = new Label("Cost: ");
        add(roadC);
        roadWood = new ColorSquare(ColorSquare.WOOD, 1);
        add(roadWood);
        roadClay = new ColorSquare(ColorSquare.CLAY, 1);
        add(roadClay);
        roadBut = new Button("---");
        roadBut.setEnabled(false);
        add(roadBut);
        roadBut.setActionCommand(ROAD);
        roadBut.addActionListener(this);

        settlementT = new Label("Settlement: ");
        add(settlementT);
        settlementV = new Label("1 VP ");
        settlementV.setAlignment(Label.LEFT);
        add(settlementV);
        settlementC = new Label("Cost: ");
        add(settlementC);
        settlementWood = new ColorSquare(ColorSquare.WOOD, 1);
        add(settlementWood);
        settlementClay = new ColorSquare(ColorSquare.CLAY, 1);
        add(settlementClay);
        settlementWheat = new ColorSquare(ColorSquare.WHEAT, 1);
        add(settlementWheat);
        settlementSheep = new ColorSquare(ColorSquare.SHEEP, 1);
        add(settlementSheep);
        settlementBut = new Button("---");
        settlementBut.setEnabled(false);
        add(settlementBut);
        settlementBut.setActionCommand(STLMT);
        settlementBut.addActionListener(this);

        cityT = new Label("City Upgrade: ");
        add(cityT);
        cityV = new Label("2 VP  (receives 2x rsrc.) ");
        cityV.setAlignment(Label.LEFT);
        add(cityV);
        cityC = new Label("Cost: ");
        add(cityC);
        cityWheat = new ColorSquare(ColorSquare.WHEAT, 2);
        add(cityWheat);
        cityOre = new ColorSquare(ColorSquare.ORE, 3);
        add(cityOre);
        cityBut = new Button("---");
        cityBut.setEnabled(false);
        add(cityBut);
        cityBut.setActionCommand(CITY);
        cityBut.addActionListener(this);

        cardT = new Label("Card: ");
        add(cardT);
        cardV = new Label("? VP  (largest army = 2 VP) ");
        cardV.setAlignment(Label.LEFT);
        add(cardV);
        cardC = new Label("Cost: ");
        add(cardC);
        cardWheat = new ColorSquare(ColorSquare.WHEAT, 1);
        add(cardWheat);
        cardSheep = new ColorSquare(ColorSquare.SHEEP, 1);
        add(cardSheep);
        cardOre = new ColorSquare(ColorSquare.ORE, 1);
        add(cardOre);
        cardBut = new Button("---");
        cardBut.setEnabled(false);
        add(cardBut);
        cardBut.setActionCommand(CARD);
        cardBut.addActionListener(this);
        cardCountLab = new Label("available");
        cardCountLab.setAlignment(Label.LEFT);
        add(cardCountLab);
        cardCount = new ColorSquare(ColorSquare.GREY, 0);        
        cardCount.setTooltipText("Development cards available to buy");
        cardCount.setTooltipLowWarningLevel("Almost out of development cards to buy", 3);
        cardCount.setTooltipZeroText("No more development cards available to buy");
        add(cardCount);
    }

    /**
     * DOCUMENT ME!
     */
    public void doLayout()
    {
        Dimension dim = getSize();
        int curY = 0;
        int curX;
        FontMetrics fm = this.getFontMetrics(this.getFont());
        int lineH = ColorSquare.HEIGHT;
        int rowSpaceH = (dim.height - (8 * lineH)) / 3;
        int halfLineH = lineH / 2;
        int costW = fm.stringWidth("Cost:_");    //Bug in stringWidth does not give correct size for ' ' so use '_'
        int butW = 50;
        int margin = 2;

        /*
           title.setSize(dim.width, lineH);
           title.setLocation(0, 0);
           curY += lineH;
         */
        roadT.setSize(fm.stringWidth(roadT.getText()), lineH);
        roadT.setLocation(margin, curY);

        int roadVW = fm.stringWidth(roadV.getText());
        roadV.setSize(roadVW, lineH);
        roadV.setLocation(dim.width - (roadVW + margin), curY);
        curY += lineH;
        roadC.setSize(costW, lineH);
        roadC.setLocation(margin, curY);
        curX = 1 + costW + 3;
        roadWood.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        roadWood.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        roadClay.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        roadClay.setLocation(curX, curY);
        roadBut.setSize(butW, lineH);
        roadBut.setLocation(dim.width - (butW + margin), curY);
        curY += (rowSpaceH + lineH);

        settlementT.setSize(fm.stringWidth(settlementT.getText()), lineH);
        settlementT.setLocation(margin, curY);

        int settlementVW = fm.stringWidth(settlementV.getText());
        settlementV.setSize(settlementVW, lineH);
        settlementV.setLocation(dim.width - (settlementVW + margin), curY);
        curY += lineH;
        settlementC.setSize(costW, lineH);
        settlementC.setLocation(margin, curY);
        curX = 1 + costW + 3;
        settlementWood.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        settlementWood.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        settlementClay.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        settlementClay.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        settlementWheat.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        settlementWheat.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        settlementSheep.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        settlementSheep.setLocation(curX, curY);
        settlementBut.setSize(butW, lineH);
        settlementBut.setLocation(dim.width - (butW + margin), curY);
        curY += (rowSpaceH + lineH);

        cityT.setSize(fm.stringWidth(cityT.getText()), lineH);
        cityT.setLocation(margin, curY);

        int cityVW = fm.stringWidth(cityV.getText());
        cityV.setSize(cityVW, lineH);
        cityV.setLocation(dim.width - (cityVW + margin), curY);
        curY += lineH;
        cityC.setSize(costW, lineH);
        cityC.setLocation(margin, curY);
        curX = 1 + costW + 3;
        cityWheat.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cityWheat.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        cityOre.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cityOre.setLocation(curX, curY);
        cityBut.setSize(butW, lineH);
        cityBut.setLocation(dim.width - (butW + margin), curY);
        curY += (rowSpaceH + lineH);

        cardT.setSize(fm.stringWidth(cardT.getText()), lineH);
        cardT.setLocation(margin, curY);

        int cardVW = fm.stringWidth(cardV.getText());
        cardV.setSize(cardVW, lineH);
        cardV.setLocation(dim.width - (cardVW + margin), curY);
        curY += lineH;
        cardC.setSize(costW, lineH);
        cardC.setLocation(margin, curY);
        curX = 1 + costW + 3;
        cardWheat.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cardWheat.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        cardSheep.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cardSheep.setLocation(curX, curY);
        curX += (ColorSquare.WIDTH + 3);
        cardOre.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cardOre.setLocation(curX, curY);
        cardBut.setSize(butW, lineH);
        cardBut.setLocation(dim.width - (butW + margin), curY);
        curX = dim.width - butW - margin;
        int cardCLabW = fm.stringWidth(cardCountLab.getText());
        curX -= (6 + cardCLabW);
        cardCountLab.setLocation(curX, curY);
        cardCountLab.setSize(cardCLabW + 2, lineH);
        curX -= (ColorSquare.WIDTH + 3);
        // cardCount.setSize(ColorSquare.WIDTH, ColorSquare.HEIGHT);
        cardCount.setLocation(curX, curY);
    }

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     */
    public void actionPerformed(ActionEvent e)
    {
        try {
        String target = e.getActionCommand();
        SOCGame game = pi.getGame();

        if (player != null)
        {
            if (game.getCurrentPlayerNumber() == player.getPlayerNumber())
            {
                clickBuildingButton(game, pi.getClient(), target, false);
            }
        }
        } catch (Throwable th) {
            pi.chatPrintStackTrace(th);
        }
    }
    
    /** Handle a click on a building-panel button.
     * 
     * @param game   The game, for status
     * @param client The client, for sending build or cancel request
     * @param target Button clicked, as returned by ActionEvent.getActionCommand
     * @param doNotClearPopup Do not call SOCBoardPanel.popupClearBuildRequest()
     * 
     * @see SOCBoardPanel.popupClearBuildRequest()
     */
    public void clickBuildingButton(SOCGame game, SOCPlayerClient client, String target, boolean doNotClearPopup)
    {
        if (! doNotClearPopup)
            pi.getBoardPanel().popupClearBuildRequest();  // Just in case
        
        if (target == ROAD)
        {
            if ((game.getGameState() == SOCGame.PLAY1) && (roadBut.getLabel().equals("Buy")))
            {
                client.buildRequest(game, SOCPlayingPiece.ROAD);
            }
            else if (roadBut.getLabel().equals("Cancel"))
            {
                client.cancelBuildRequest(game, SOCPlayingPiece.ROAD);
            }
        }
        else if (target == STLMT)
        {
            if ((game.getGameState() == SOCGame.PLAY1) && (settlementBut.getLabel().equals("Buy")))
            {
                client.buildRequest(game, SOCPlayingPiece.SETTLEMENT);
            }
            else if (settlementBut.getLabel().equals("Cancel"))
            {
                client.cancelBuildRequest(game, SOCPlayingPiece.SETTLEMENT);
            }
        }
        else if (target == CITY)
        {
            if ((game.getGameState() == SOCGame.PLAY1) && (cityBut.getLabel().equals("Buy")))
            {
                client.buildRequest(game, SOCPlayingPiece.CITY);
            }
            else if (cityBut.getLabel().equals("Cancel"))
            {
                client.cancelBuildRequest(game, SOCPlayingPiece.CITY);
            }
        }
        else if (target == CARD)
        {
            if ((game.getGameState() == SOCGame.PLAY1) && (cardBut.getLabel().equals("Buy")))
            {
                client.buyDevCard(game);
            }
        }        
    }

    /**
     * update the status of the buttons
     */
    public void updateButtonStatus()
    {
        SOCGame game = pi.getGame();

        if (player != null)
        {
            int pnum = player.getPlayerNumber();
            boolean isCurrent = (game.getCurrentPlayerNumber() == pnum);
            int gstate = game.getGameState();
            boolean currentCanBuy = isCurrent && (gstate == SOCGame.PLAY1);

            if (isCurrent && (gstate == SOCGame.PLACING_ROAD))
            {
                roadBut.setEnabled(true);
                roadBut.setLabel("Cancel");
            }
            else if (game.couldBuildRoad(pnum))
            {
                roadBut.setEnabled(currentCanBuy);
                roadBut.setLabel("Buy");
            }
            else
            {
                roadBut.setEnabled(false);
                roadBut.setLabel("---");
            }

            if (isCurrent &&
                ((gstate == SOCGame.PLACING_SETTLEMENT) || (gstate == SOCGame.START1B)
                 || (gstate == SOCGame.START2B)))
            {
                settlementBut.setEnabled(true);
                settlementBut.setLabel("Cancel");
            }
            else if (game.couldBuildSettlement(pnum))
            {
                settlementBut.setEnabled(currentCanBuy);
                settlementBut.setLabel("Buy");
            }
            else
            {
                settlementBut.setEnabled(false);
                settlementBut.setLabel("---");
            }

            if (isCurrent && (gstate == SOCGame.PLACING_CITY))
            {
                cityBut.setEnabled(true);
                cityBut.setLabel("Cancel");
            }
            else if (game.couldBuildCity(pnum))
            {
                cityBut.setEnabled(currentCanBuy);
                cityBut.setLabel("Buy");
            }
            else
            {
                cityBut.setEnabled(false);
                cityBut.setLabel("---");
            }

            if (game.couldBuyDevCard(pnum))
            {
                cardBut.setEnabled(currentCanBuy);
                cardBut.setLabel("Buy");
            }
            else
            {
                cardBut.setEnabled(false);
                cardBut.setLabel("---");
            }
        }
    }

    /**
     * The game's count of development cards remaining has changed.
     * Update the display.
     */
    public void updateDevCardCount()
    {
        int newCount = pi.getGame().getNumDevCards();
        cardCount.setIntValue(newCount);
    }

    /**
     * Set our game and player data based on client's nickname,
     * via game.getPlayer(client.getNickname()).
     *
     * @throws IllegalStateException If the player data has already been set,
     *    and this isn't a new game (a board reset).
     */
    public void setPlayer()
        throws IllegalStateException
    {
        SOCGame game = pi.getGame();
        if ((player != null) && ! game.isBoardReset())
            throw new IllegalStateException("Player data is already set");

        player = game.getPlayer(pi.getClient().getNickname());
    }

}
