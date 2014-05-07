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

public final class LunoGame {

    private SocketIOServer server;
    private Player players[]={null, null, null, null};
    private ArrayList<Card> deck;
    private ArrayList<Card> discard; 
    private Random randomGenerator;
    private ArrayList<Move> moves;

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

    public LunoGame (SocketIOServer server, Player creator) {
       this.server = server;
       this.deck = new ArrayList<Card>();
       this.discard = new ArrayList<Card>();
       this.moves = new ArrayList<Move>();
       this.randomGenerator = new Random();
       players[0] = creator;
    }

}
