package com.socketio.luno;

import com.socketio.luno.Card.Special;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;

/**
 *
 * @author zjy
 */
final public class Player {
    int bet;
    int leftCoins;
    ArrayList<Card> handCards;
    String user;
    boolean ifUno;
    boolean unoble;
    //boolean ifLost;
    int points;
    private SocketIOClient socket; 
    private LunoGame currentGame;    
    
    public Player(String username, SocketIOClient socket){
        this.user = username;
        leftCoins = getCoins(user);
        handCards = new ArrayList<Card>();
        bet = 0;
        ifUno = false;
        //ifLost=false;
        points= 0;
        this.socket = socket;
        this.currentGame = null;
    }
    
    public void setCurrentGame(LunoGame game) {
        this.currentGame = game;
    }

    public LunoGame getCurrentGame() {
        return this.currentGame;
    }

    public void quitGame() {
        this.currentGame = null;
        points = 0;
        handCards = new ArrayList<Card>(); 
    }

    public void joinGame() {
    }

    public void setSocket(SocketIOClient socket) {
        this.socket = socket;
    } 

    public SocketIOClient getSocket() {
        return socket;
    }

    public void addHandCard(Card card) throws IllegalArgumentException{
        if(handCards.size()<=108)    
            handCards.add(card);
        else
            throw new IllegalArgumentException("Hand cards number cannot exceed deck cards number"); 
    }
    public Card getHandCard(int i) throws ArrayIndexOutOfBoundsException{
        if(i>=0&&i<handCards.size()){ 
            Card card=handCards.get(i);
            return card;
        }
        else
            throw new ArrayIndexOutOfBoundsException("Invalid card");
    }
    public Card removeHandCard(int i) throws ArrayIndexOutOfBoundsException{
        if(i>=0&&i<handCards.size()){ 
            Card card=handCards.remove(i);
            return card;
        }
        else
            throw new ArrayIndexOutOfBoundsException("Invalid card");
    }
    
    public void setBet(int bet) throws IllegalArgumentException{
        if(bet<=leftCoins+this.bet){
            leftCoins=leftCoins+this.bet-bet;
            this.bet=bet;            
        }
        else
            throw new IllegalArgumentException("Coins are not enough.");
    }
    public int getBet(){
        return bet;
    }
    public int getHandCardsCount(){
        return handCards.size();
    }
    public ArrayList getHandCardsName(){
        ArrayList<String> names=new ArrayList<String>();
        for(Card card:handCards)
            names.add(card.getName());
        return names;
    }
    public ArrayList getHandCardsNameColorized(){
        ArrayList<String> names=new ArrayList<String>();
        for(Card card:handCards)
            names.add(card.getNameColorized());
        return names;
    }

    public int handPoints(){
        for(Card card:handCards)
            points+=card.point;
        return points;
    }
    
//    public boolean readyToUno(){
//        if(handCards.size()==2)
//            unoble=true;
//        else
//            unoble=false;
//        return unoble;
//    }
    
    public void setUno(){
        if(handCards.size()==1)
            ifUno=true;
        else
            ifUno=false;
    }
    public boolean isUno(){
            return ifUno;
    }
    public String getName(){
        return user;
    }
//    public boolean isLost(){
//        return ifLost;
//    }
//    public void setLost(){
//        ifLost=true;
//    }
    
    public void settleCoins(int bet, String state){
            int coins=bet+leftCoins;
            if(bet>0)
                updatePlayer(user, coins, state);
            else
                updatePlayer(user, coins, "lost");    
    }
    public Set getValidDiscard(Card currentCard){
        Set<Integer> validDiscard=new HashSet<Integer>();
        if(currentCard.spec==Special.Draw||currentCard.spec==Special.WildDraw){
            for(int i=0;i<handCards.size();i++){
                Card card=handCards.get(i);
                if(card.spec==Special.Draw||card.spec==Special.WildDraw)
                    validDiscard.add(new Integer(i));
            }          
        }
        else{
            ArrayList<Integer> sameNumOrSpec=new ArrayList<Integer>();
            ArrayList<Integer> sameColor=new ArrayList<Integer>();
            ArrayList<Integer> wildDraw=new ArrayList<Integer>();
            for(int i=0;i<handCards.size();i++){
                Card card=handCards.get(i);
                
                if(currentCard.num!=-1&&card.num==currentCard.num)
                    sameNumOrSpec.add(new Integer(i));
                else if(currentCard.spec!=null&&card.spec==currentCard.spec)
                    sameNumOrSpec.add(new Integer(i));
                else if(card.spec==Special.Wildcards)
                    sameNumOrSpec.add(new Integer(i));
                if(card.color==currentCard.color)
                    sameColor.add(new Integer(i));
                if(card.spec==Special.WildDraw)
                    wildDraw.add(new Integer(i));
            }
            validDiscard.addAll(sameNumOrSpec);
            if(sameColor.size()>0)
                validDiscard.addAll(sameColor);
            else
                validDiscard.addAll(wildDraw);
        }
        return validDiscard;
    }
    
    ////Private Methods/////////////
    //Database query and update////////
    private int getCoins(String user){
        return 1000;
    }
    private void updatePlayer(String user, int coins, String state){
    }
}

