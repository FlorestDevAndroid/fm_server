package ru.florestdev.fm_server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class ChatService extends Service {
    private static FMServer server;
    private WifiManager.WifiLock wifiLock;
    private static final String CHANNEL_ID = "FMServerChannel";
    private static final int NOTIFICATION_ID = 1;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private Runnable statusRunnable;

    public static FMServer getServerInstance() {
        return server;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Блокировка от засыпания Wi-Fi
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "FMServer:WifiLock");
            wifiLock.acquire();
        }

        try {
            server = new FMServer();
            Toast.makeText(this, "Сервер запущен", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка запуска сервера", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = createNotification("Сервер запущен на порту 3550");
        startForeground(NOTIFICATION_ID, notification);

        startStatusUpdates();
        return START_STICKY;
    }

    private Notification createNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("FM Server")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void startStatusUpdates() {
        statusRunnable = new Runnable() {
            @Override
            public void run() {
                if (server != null) {
                    Notification notification = createNotification(
                            "Клиентов: " + server.getClientCount() + " | IP: " + server.getLocalIpAddress()
                    );
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (manager != null) {
                        manager.notify(NOTIFICATION_ID, notification);
                    }
                }
                statusHandler.postDelayed(this, 10000);
            }
        };
        statusHandler.post(statusRunnable);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FM Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Уведомления о работе сервера");

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (statusRunnable != null) {
            statusHandler.removeCallbacks(statusRunnable);
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
        if (server != null) {
            server.stopServer();
            server = null;
        }
        Toast.makeText(this, "Сервер остановлен", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}