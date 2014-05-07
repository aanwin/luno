package com.socketio.luno;

import java.io.InputStream;
import org.apache.commons.codec.binary.Base64;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;

import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import com.socketio.luno.Card.Color;
import com.socketio.luno.Card.Special;


public final class LunoGame {

    private SocketIOServer server;
    private Player players[]={null, null, null, null};
    private ArrayList<Card> deck;
    private ArrayList<Card> discard; 
    private Random randomGenerator;
    private ArrayList<Move> moves;
    private int playerCount;
    private int gameID;
    private String status;
    private Card currentCard;
    private Time timeout;
    private int totalBet;
    private int drawNum;
    private int currentPlayer;
    private boolean ifChangeColor;
    private boolean ifSkip;
    private boolean ifReverse;
    private int increaser;
    private boolean ifWin;
    private boolean readyToUno;
    private boolean clockwise;

    private class Move{
        String playerName;
        String action;
        String cardName;
        
        public Move(String pName, String action, String cName){
            playerName=pName;
            this.action=action;
            cardName=cName;
        }
    }

    public LunoGame (SocketIOServer server, Player creator, int gameID) {
       this.server = server;
       this.deck = new ArrayList<Card>();
       this.discard = new ArrayList<Card>();
       this.moves = new ArrayList<Move>();
       this.randomGenerator = new Random();
       this.gameID = gameID;
       this.status = "waiting";
       this.players[0] = creator;
       this.players[0].setCurrentGame(this);
       this.playerCount = 1;
       this.totalBet = 0;
       this.drawNum = 0;
       this.currentPlayer = -1;
       this.ifChangeColor=false;
       this.ifSkip=false;
       this.ifReverse=false;
       this.increaser=1;
       this.ifWin=false;
       this.readyToUno=false;
       this.clockwise = true;

    }
    
    public int getID() {
        return gameID;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int ejectPlayer(Player p) {
       for (int i=0; i < players.length; i++) {
           if (players[i] == p) {
               players[i].quitGame();
               players[i] = null;
               playerCount--;
               
               return i;
           }
       }
       throw new ArrayIndexOutOfBoundsException("Player wasn't in here!");
    }

    public int addPlayer(Player p) {
        for(int i=0;i < players.length; i++){
            if (players[i] == null){
                p.setBet(50);
                players[i]=p;
                playerCount++;
                if (playerCount == 4) {
                    this.status = "playing";
                }
                p.setCurrentGame(this);
                return i;
            }
        }
        return 5;
    }
    public int getNumPlayers() {
        return playerCount;
    }

    public void announce(String message) {
        for(int i=0;i < players.length; i++){
           players[i].getSocket().sendEvent("server_response", new ChatObject("Server [Game]", message));
        }
    }

    public void announce(String message, boolean warning) {
        for(int i=0;i < players.length; i++){
           players[i].getSocket().sendEvent("server_response", new ChatObject("Server [Game]", message, true));
        }
    }

    public void startGame() {
        announce("Your game is starting!");
        initDeck();
        
        // Deal cards randomly from deck, equiv to shuffle then deal for practical purposes
        for(int j=0;j<4;j++){
            for (int i=0; i<7; i++){
                int index = randomGenerator.nextInt(deck.size());
                Card card=deck.remove(index);
                players[j].handCards.add(card);
            }
            // Get all bets from players
            totalBet+=players[j].getBet();
        }
        // draw and play the first card
        currentCard=deck.remove(randomGenerator.nextInt(deck.size()));
        if(currentCard.spec==Special.Draw)
                drawNum+=2;
            if(currentCard.spec==Special.WildDraw){
                drawNum+=4;
                ifChangeColor=true;
            }
            if(currentCard.spec==Special.Reverse)
                ifReverse=true;
            if(currentCard.spec==Special.Skip)
                ifSkip=true;
            if(currentCard.spec==Special.Wildcards)
                ifChangeColor=true;
        moves.add(new Move("Initial", "Discard", currentCard.getName()));
        discard.add(currentCard);
        currentPlayer=randomGenerator.nextInt(4);
        announce("Players: " + players[0].getName() + " -> " + players[1].getName() + " -> " + players[2].getName() + " -> " + players[3].getName() + " -> "); 
        announce(players[currentPlayer].getName() + " gets the first turn, going forward/clockwise.");      
        announce("Initial card is " + currentCard.getNameColorized());
        
        for(int i=0;i<players.length; i++){
           String hand = "";
           ArrayList<String> handNames = players[i].getHandCardsNameColorized();
           int k = 1;
           for (String cardName : handNames) {
               hand += k+":[" + cardName + "] ";
               k++;
           }
           players[i].getSocket().sendEvent("server_response", new ChatObject("Server [Game]", "Your hand is: <b>" + hand + "</b>", true));
        }

 
        // And now we wait for currentPlayer to move
       // moves.add(new Move(players[currentPlayer].getName(), "Discard",currentCard.getName()));
    }
    
    public void handleMove(Player player, String msg) {
        announce(player.getName() + " " + msg); 
    }

    public void endGameLeaver() {
        for(int i=0;i < players.length; i++){
           players[i].getSocket().sendEvent("server_response", new ChatObject("Server [Message]", "Your game has ended, someone left."));
           ejectPlayer(players[i]);
        }
    }

    private void initDeck(){
        for(int c=0;c<4;c++){
            switch(c){     
                case 0:
                    deck.add(new Card(0,Color.Red));
                    for(int j=0;j<2;j++){
                        for(int i=1;i<10;i++)
                            deck.add(new Card(i,Color.Red));
                        deck.add(new Card(Special.Draw, Color.Red));
                        deck.add(new Card(Special.Reverse, Color.Red));
                        deck.add(new Card(Special.Skip,Color.Red));
                    }                       
                    break;
                case 1:
                    deck.add(new Card(0,Color.Blue));
                    for(int j=0;j<2;j++){
                        for(int i=1;i<10;i++)
                            deck.add(new Card(i,Color.Blue));
                        deck.add(new Card(Special.Draw, Color.Blue));
                        deck.add(new Card(Special.Reverse, Color.Blue));
                        deck.add(new Card(Special.Skip,Color.Blue));
                    }  
                    break;
                case 2:
                    deck.add(new Card(0,Color.Green));
                    for(int j=0;j<2;j++){
                        for(int i=1;i<10;i++)
                            deck.add(new Card(i,Color.Green));
                        deck.add(new Card(Special.Draw, Color.Green));
                        deck.add(new Card(Special.Reverse, Color.Green));
                        deck.add(new Card(Special.Skip,Color.Green));
                    }
                    break;
                case 3:
                   deck.add(new Card(0,Color.Yellow));
                    for(int j=0;j<2;j++){
                        for(int i=1;i<10;i++)
                            deck.add(new Card(i,Color.Yellow));
                        deck.add(new Card(Special.Draw, Color.Yellow));
                        deck.add(new Card(Special.Reverse, Color.Yellow));
                        deck.add(new Card(Special.Skip,Color.Yellow));
                    }  
                    break;
                default:
                     break;
            }
            deck.add(new Card(Special.Wildcards, Color.All));
            deck.add(new Card(Special.WildDraw, Color.All));
        }
    }


}
