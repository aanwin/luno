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
        this.message = sanitize(message);
        this.userNameSigned = userNameSigned;
    }

   public ChatObject(String userName, String message) {
        super();
        this.userName = userName;
        this.message = sanitize(message);
    }

   // Only use this if you're sure about safety of output and need to format(i.e. no db or client input possible, necessary for colors)
   public ChatObject(String userName, String message, boolean color) {
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
        this.message = sanitize(message);
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
