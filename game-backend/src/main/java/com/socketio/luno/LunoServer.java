package com.socketio.luno;

import java.io.InputStream;
import java.util.*;
import org.apache.commons.codec.binary.Base64;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;

import com.alasershark.itsdangerouser.*;
import com.alasershark.itsdangerouser.exceptions.BadSignatureException;

import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class LunoServer {

    private ArrayList<LunoGame> games;
    private final SocketIOServer server;

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

        server = new SocketIOServer(config);
        this.addListeners();

        server.start();
        Thread.sleep(Integer.MAX_VALUE);
        server.stop();

    }


    public void addListeners() {

        server.addEventListener("chatevent", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
	        if (verifySignature(data.getUserName(), data.getUserNameSigned())) {	
                    server.getBroadcastOperations().sendEvent("chatevent", data);
                }
                else {
                    server.getBroadcastOperations().sendEvent("chatevent", new ChatObject("Server", data.getUserNameSigned(), data.getUserName() + " can't be trusted!"));

                }
            }
        });

        server.addEventListener("join_game", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                client.sendEvent("server_response", new ChatObject("Server", "Searching for game..."));
            }
        });

        server.addEventListener("create_game", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                
                server.getBroadcastOperations().sendEvent("chatevent", new ChatObject("Server", data.getUserName() + " has created a game.")); 
                client.sendEvent("server_response", new ChatObject("Server", "You have created a game."));
            }
        });


        server.addEventListener("leave_game", ChatObject.class, new DataListener<ChatObject>() {
            @Override
            public void onData(SocketIOClient client, ChatObject data, AckRequest ackRequest) {
                client.sendEvent("server_response", new ChatObject("Server", "Successfully left."));
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

}
