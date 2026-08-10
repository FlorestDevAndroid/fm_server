package ru.florestdev.fm_server;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {
    private final String nick;
    private final String text;
    private final long timestamp;

    public Message(String nick, String text) {
        this.nick = nick;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
    }

    public String getNick() { return nick; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }

    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("nick", nick);
            json.put("text", text);
            json.put("time", timestamp);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }
}