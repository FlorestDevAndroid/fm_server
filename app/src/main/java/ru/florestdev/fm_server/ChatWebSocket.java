package ru.florestdev.fm_server;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ChatWebSocket extends NanoWSD.WebSocket {
    private final ClientManager manager;
    private String nick = "Anonymous";
    private boolean joined = false;

    public ChatWebSocket(NanoHTTPD.IHTTPSession handshake, ClientManager manager) {
        super(handshake);
        this.manager = manager;
    }

    @Override
    protected void onOpen() {
        System.out.println("[WebSocket] Opened");
        manager.addClient(this);
        sendHistory();
    }

    private void sendHistory() {
        for (Message msg : manager.getHistory()) {
            sendMessage(msg.toJson());
        }
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        System.out.println("[WebSocket] Closed: " + reason);
        manager.removeClient(this);
        if (joined) {
            manager.broadcastSystem("leave", nick);
        }
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {
        // Ответ на ping получен — соединение живое
    }

    @Override
    protected void onException(IOException exception) {
        System.err.println("[WebSocket] Exception: " + exception.getMessage());
        manager.removeClient(this);
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame frame) {
        String message = frame.getTextPayload();
        System.out.println("[WebSocket] Received: " + message);
        handleMessage(message);
    }

    private void handleMessage(String raw) {
        try {
            JSONObject json = new JSONObject(raw);
            String type = json.optString("type", "");

            switch (type) {
                case "join":
                    this.nick = json.optString("nick", "Anonymous");
                    this.joined = true;
                    manager.broadcastSystem("join", nick);
                    manager.broadcastSystem("system", nick + " присоединился к чату");
                    break;

                case "typing":
                    manager.broadcastSystem("typing", json.optString("nick", nick));
                    break;

                case "stop_typing":
                    manager.broadcastSystem("stop_typing", json.optString("nick", nick));
                    break;

                case "message":
                default:
                    String msgNick = json.optString("nick", this.nick);
                    String text = json.optString("text", "");
                    if (!text.isEmpty()) {
                        manager.broadcast(msgNick, text);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("[WebSocket] JSON parse error: " + e.getMessage());
        }
    }

    public void sendPing() {
        try {
            ping(new byte[0]);
        } catch (IOException e) {
            System.err.println("[WebSocket] Ping error: " + e.getMessage());
            manager.removeClient(this);
        }
    }

    public void sendMessage(String json) {
        try {
            send(json);
        } catch (Exception e) {
            System.err.println("[WebSocket] Send error: " + e.getMessage());
        }
    }
}