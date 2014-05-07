package com.socketio.luno;

import java.io.InputStream;
import java.util.*;
import org.apache.commons.codec.binary.Base64;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class LunoServer {

    private ArrayList<LunoGame> games;
    private ArrayList<Player> players;
    private final SocketIOServer server;
    private int gameNumber;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting!");
        LunoServer ls = new LunoServer("162.243.24.168", 10443, "test1234", "/keystore.jks");
    }
 
    public LunoServer(String hostname, int port, String ksPass, String ksLoc) throws InterruptedException {
        Configuration config = new Configuration();
        config.setHostname(hostname);
        config.setPort(port);
        config.setKeyStorePassword(ksPass);
        InputStream stream = LunoServer.class.getResourceAsStream(ksLoc);
        config.setKeyStore(stream);

        games = new ArrayList<LunoGame>();
        players = new ArrayList<Player>();
        server = new SocketIOServer(config);
        this.addListeners();
        this.gameNumber = 1;

        server.start();
        Thread.sleep(Integer.MAX_VALUE);
        server.stop();

    }

    private Player findPlayer(SocketIOClient client) {
        Player player = null;
        for (Player p : players) {
            if (p.getSocket() == client) {
	        player = p;
	        break; 
            }
        }
        return player;
    }
   
    public void addListeners() {

        server.addEventListener("player_connect", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                if (checkUserMessage(data)) {
                    Player newPlayer = null;
                    for (Player p : players) {
                        if (p.getName().equals(data.getUserName())) {
                            newPlayer = p;
                            break;
                        }
                    }     
                    if (newPlayer == null) {
                        players.add(new Player(data.getUserName(), client));
                        System.out.println("Added new player " + data.getUserName());
                    }
                    else {
                        System.out.println("Reassigned new socket to reconnecting player " + data.getUserName());
                        newPlayer.setSocket(client);
                    }
                }
                client.sendEvent("server_response", new ChatObject("Server [Message]", "Welcome!"));
            }
        });

        /*server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                server.getBroadcastOperations().sendEvent("chatevent", new ChatObject("Server [Message]", findPlayer(client).getName() + " socket disconnected!")); 
            }
        });
        */
        server.addEventListener("chatevent", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                if (checkUserMessage(data))
                    server.getBroadcastOperations().sendEvent("chatevent", data);
            }
        });

        server.addEventListener("game_move", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                if (checkUserMessage(data)) {
                    Player player = findPlayer(client);
                    LunoGame game = player.getCurrentGame();
                    // let the game handle it
                    game.handleMove(player, data.getMessage());
                }
            }
        });


        server.addEventListener("join_game", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                if (checkUserMessage(data)) {
                    Player player = findPlayer(client);
                    if (player == null) {
                        client.sendEvent("server_response", new ChatObject("Server [Message]", "Error joining game, this connection has expired. Check to see if you've logged in from another browser."));
                        return;
                    }
                    if (player.getCurrentGame() != null) {
                        client.sendEvent("server_response", new ChatObject("Server [Message]", "Can't join a game, you're currently in one!"));
                        return;
                    }
                    String message = data.getMessage();
                    if (message.split(" ").length != 2) {
                        client.sendEvent("server_response", new ChatObject("Server [Message]", "You need to add a room id number to join! ('join 1')"));
                        return;
                    }
                    if (!message.split(" ")[1].matches("-?\\d+")) {
                        client.sendEvent("server_response", new ChatObject("Server [Message]", "That's not a valid room number."));
                        return;
                    }
                    int gameId = games.size() + 1;
                    try {
                        gameId = Integer.parseInt(message.split(" ")[1]); 
                    } catch (NumberFormatException e) {
                    }

                    for (LunoGame g : games) {
                        if (g.getID() == gameId) {
                            LunoGame game = g;
                            int result = game.addPlayer(player);
                            if (result > 4) {
                                client.sendEvent("server_response", new ChatObject("Server [Message]", "Couldn't join this game, it's full!"));
                                return;
                            }
                            System.out.println(player.getName() + " joined a game.");
                            if (result == 4) {
                                client.sendEvent("server_response", new ChatObject("Server [Message]", "You have successfully joined a game and caused it to start!"));
                            }
                            else
                                client.sendEvent("server_response", new ChatObject("Server [Message]", "You have successfully joined a game and now must wait for the room to fill up."));
                            server.getBroadcastOperations().sendEvent("chatevent", new ChatObject("Server [Broadcast]", data.getUserName() + " has joined a game (Game ID: " + gameId + ")."));
                            if (game.getStatus().equals("playing"))
                                game.startGame();
                            return;
                        }
                    }
                    client.sendEvent("server_response", new ChatObject("Server [Message]", "That room number doesn't have a game."));
                }
            }
        });

        server.addEventListener("game_list", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                if (checkUserMessage(data)) {
                    String msg = "";
                    client.sendEvent("server_response", new ChatObject("Server [Message]", "Game list:"));
                    for (LunoGame g : games) {
                        msg = "Game " + g.getID() + ": " + g.getNumPlayers() + "/4 (" + g.getStatus() + ")";  
                        client.sendEvent("server_response", new ChatObject("Server [Message]", msg));
                    }
                    System.out.println(data.getUserName() + " asked for game list.");
                }
            }
        });


        server.addEventListener("create_game", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                if (checkUserMessage(data)) { 
                    Player player = findPlayer(client);
                    if (player == null) {
                        client.sendEvent("server_response", new ChatObject("Server [Message]", "Error creating game, this connection has expired. Check to see if you logged in from another browser."));
                        return;
                    }
                    if (player.getCurrentGame() != null) {
                        client.sendEvent("server_response", new ChatObject("Server [Message]", "Can't create a game, you're currently in one!"));
                        return;
                    }
                    gameNumber++;
                    LunoGame game = new LunoGame(server, player, gameNumber);
                    games.add(game);
                    System.out.println(player.getName() + " created a game.");
                    server.getBroadcastOperations().sendEvent("chatevent", new ChatObject("Server [Broadcast]", data.getUserName() + " has created a game (Game ID: " + gameNumber + ").")); 
                    client.sendEvent("server_response", new ChatObject("Server [Message]", "You have created a game with ID " + gameNumber + "."));
                }
            }
        });


        server.addEventListener("leave_game", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                if (checkUserMessage(data)) {
                    Player player = findPlayer(client);
                    if (player == null) {
                        client.sendEvent("server_response", new ChatObject("Server [Message]", "Error leaving game, this connection has expired. Check to see if you logged in from another browser."));
                        return;
                    }
                    if (player.getCurrentGame() == null) {
                        client.sendEvent("server_response", new ChatObject("Server [Message]", "Can't leave a game, you're not currently in one!"));
                        return;
                    }
                    LunoGame game = player.getCurrentGame();
                    game.ejectPlayer(player);
                    if (game.getNumPlayers() == 0) {
                       System.out.println("Game " + game.getID() + " destroyed, no players inside.");
                       games.remove(game);
                    }
                    System.out.println(player.getName() + " left a game."); 
                    client.sendEvent("server_response", new ChatObject("Server [Message]", "You have successfully left the game you were in."));
                }
            }
        });
    }

    private static boolean verifySignature(String str, String strSig) {
        try {
	    SecretKeySpec keySpec = new SecretKeySpec(
		"commsecretkey".getBytes("UTF-8"),
		"HmacSHA1");

       	    Mac mac = Mac.getInstance("HmacSHA1");
	    mac.init(keySpec);
	    byte[] result = mac.doFinal(str.getBytes("UTF-8"));

	    String calculatedSig = (new String(Base64.encodeBase64(result)));
	    return calculatedSig.equals(strSig); 
        } catch (Exception e) {
        }
        return false;
    }

    private boolean checkUserMessage(ChatObject data) {
	if (!verifySignature(data.getUserName(), data.getUserNameSigned())) {
	    server.getBroadcastOperations().sendEvent("chatevent", new ChatObject("Server [Broadcast]", data.getUserNameSigned(), data.getUserName() + " can't be trusted!"));
	    return false;
	}
        data.setMessage(sanitize(data.getMessage()));
        return true;
    }

    private String sanitize(String value) {
        if (value != null) {
            value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
            value = value.replaceAll("'", "&#39;");
            value = value.replaceAll("eval\\((.*)\\)", "");
            value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
        }
        return value;
    }
}
