package ru.florestdev.fm_server;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private TextView statusText;
    private TextView infoText;
    private Button toggleButton;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        infoText = findViewById(R.id.infoText);
        toggleButton = findViewById(R.id.toggleButton);

        checkPermissions();
        updateUiState();

        toggleButton.setOnClickListener(v -> {
            if (ChatService.getServerInstance() != null) {
                stopServerService();
            } else {
                startServerService();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUiState();
    }

    private void startServerService() {
        Intent intent = new Intent(this, ChatService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        statusText.setText("🔍 Запуск службы сервера...");
        toggleButton.setEnabled(false);

        // Ждем пару секунд инициализации службы
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updateUiState();
            toggleButton.setEnabled(true);
        }, 1500);
    }

    private void stopServerService() {
        Intent intent = new Intent(this, ChatService.class);
        stopService(intent);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updateUiState();
            toggleButton.setEnabled(true);
        }, 500);
    }

    private void updateUiState() {
        FMServer server = ChatService.getServerInstance();
        if (server != null) {
            toggleButton.setText("Остановить сервер");
            String detectedIp = server.getLocalIpAddress();
            statusText.setText("✅ Сервер запущен!\n" +
                    "📡 IP: " + detectedIp + "\n" +
                    "🔌 Порт: 3550\n" +
                    "📱 Подключись к моему хотспоту");

            infoText.setText("📋 Передай друзьям:\n" +
                    "IP: " + detectedIp + "\n" +
                    "Порт: 3550");
            infoText.setVisibility(View.VISIBLE);
        } else {
            toggleButton.setText("Запустить сервер");
            statusText.setText("Сервер остановлен");
            infoText.setText("");
            infoText.setVisibility(View.GONE);
        }
    }

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.INTERNET
            };
        }

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Необходимы разрешения для работы службы!", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
    }
}