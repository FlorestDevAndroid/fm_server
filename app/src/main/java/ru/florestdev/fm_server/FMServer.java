package ru.florestdev.fm_server;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class FMServer extends NanoWSD {
    private final ClientManager manager;
    private String localIpAddress;

    public FMServer() throws IOException {
        super(3550);
        this.manager = new ClientManager();
        detectLocalIpAddress();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("[FMServer] Started on port 3550");
        System.out.println("[FMServer] Local IP: " + localIpAddress);
    }

    public int getClientCount() {
        return manager.getClientCount();
    }

    private void detectLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaces)) {
                if (iface.getName().startsWith("wlan") || iface.getName().startsWith("ap")) {
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    for (InetAddress addr : Collections.list(addresses)) {
                        if (!addr.isLoopbackAddress() && addr.getAddress().length == 4) {
                            localIpAddress = addr.getHostAddress();
                            return;
                        }
                    }
                }
            }
            localIpAddress = "192.168.43.1";
        } catch (SocketException e) {
            e.printStackTrace();
            localIpAddress = "192.168.43.1";
        }
    }

    @Override
    protected WebSocket openWebSocket(NanoHTTPD.IHTTPSession handshake) {
        System.out.println("[FMServer] WebSocket handshake from: " + handshake.getRemoteIpAddress());
        return new ChatWebSocket(handshake, manager);
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        String uri = session.getUri();

        if ("/ws".equals(uri) || "/chat".equals(uri)) {
            return super.serve(session);
        }

        if ("/status".equals(uri)) {
            String status = "{\"clients\":" + manager.getClientCount() +
                    ",\"ip\":\"" + localIpAddress + "\"}";
            return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", status);
        }

        if ("/history".equals(uri)) {
            List<Message> history = manager.getHistory();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < history.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(history.get(i).toJson());
            }
            sb.append("]");
            return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", sb.toString());
        }

        if ("/".equals(uri)) {
            String html = "<html><body>" +
                    "<h1>FM Server</h1>" +
                    "<p><b>IP:</b> " + localIpAddress + "</p>" +
                    "<p><b>Port:</b> 3550</p>" +
                    "<p><b>WebSocket endpoint:</b> ws://" + localIpAddress + ":3550/ws</p>" +
                    "<p><b>Clients online:</b> " + manager.getClientCount() + "</p>" +
                    "</body></html>";
            return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", html);
        }

        return newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Not found");
    }

    public void stopServer() {
        if (manager != null) {
            manager.stopHeartbeat();
        }
        stop();
        System.out.println("[FMServer] Stopped");
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }
}