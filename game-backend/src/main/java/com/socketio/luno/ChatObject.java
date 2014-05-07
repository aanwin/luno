package com.socketio.luno;

public class ChatObject {

    private String userName;
    private String message;
    private String userNameSigned;

    public ChatObject() {
    }

    public ChatObject(String userName, String userNameSigned, String message) {
        super();
        this.userName = userName;
        this.message = message;
        this.userNameSigned = userNameSigned;
    }

   public ChatObject(String userName, String message) {
        super();
        this.userName = userName;
        this.message = message;
    }


    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }
    
    public String getUserNameSigned () {
        return userNameSigned;
    }
    public void setMessage(String message) {
        this.message = message;
    }

}
