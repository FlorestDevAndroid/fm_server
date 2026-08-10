package ru.florestdev.fm_server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public class ClientManager {
    private final CopyOnWriteArrayList<ChatWebSocket> clients = new CopyOnWriteArrayList<>();
    private final List<Message> messages = new CopyOnWriteArrayList<>();
    private static final int MAX_MESSAGES = 200;
    private ScheduledExecutorService pingScheduler;

    public ClientManager() {
        startHeartbeat();
    }

    private void startHeartbeat() {
        pingScheduler = Executors.newSingleThreadScheduledExecutor();
        pingScheduler.scheduleAtFixedRate(() -> {
            for (ChatWebSocket client : clients) {
                if (client.isOpen()) {
                    client.sendPing();
                } else {
                    removeClient(client);
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void stopHeartbeat() {
        if (pingScheduler != null && !pingScheduler.isShutdown()) {
            pingScheduler.shutdown();
        }
    }

    public void addClient(ChatWebSocket client) {
        clients.add(client);
        System.out.println("[Server] Client connected. Total: " + clients.size());
    }

    public void removeClient(ChatWebSocket client) {
        if (clients.remove(client)) {
            System.out.println("[Server] Client disconnected. Total: " + clients.size());
        }
    }

    public void broadcast(String nick, String text) {
        Message msg = new Message(nick, text);
        messages.add(msg);

        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }

        String json = msg.toJson();
        for (ChatWebSocket client : clients) {
            client.sendMessage(json);
        }
        System.out.println("[Server] Broadcast: " + json);
    }

    public void broadcastSystem(String type, String nick) {
        try {
            JSONObject json = new JSONObject();
            json.put("type", type);
            json.put("nick", nick);

            String jsonStr = json.toString();
            for (ChatWebSocket client : clients) {
                client.sendMessage(jsonStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Message> getHistory() {
        return new CopyOnWriteArrayList<>(messages);
    }

    public int getClientCount() {
        return clients.size();
    }
}