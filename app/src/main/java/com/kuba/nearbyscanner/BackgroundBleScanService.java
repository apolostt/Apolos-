package com.kuba.nearbyscanner;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BackgroundBleScanService extends Service {
    static final String ACTION_RESULT = "com.kuba.nearbyscanner.BACKGROUND_BLE_RESULT";
    static final String ACTION_STOP = "com.kuba.nearbyscanner.STOP_BACKGROUND_BLE";
    static final String EXTRA_ADDRESS = "address";
    static final String EXTRA_NAME = "name";
    static final String EXTRA_RSSI = "rssi";
    static final String EXTRA_RECORD = "record";

    private static final String PREFS = "kuba_nearby_prefs";
    private static final String PREF_WATCHED = "watched_devices";
    private static final String PREF_ALERTS = "watch_alerts";
    private static final String PREF_BACKGROUND_SCAN = "background_scan_enabled";
    private static final String PREF_APP_VISIBLE = "app_visible";
    private static final String PREF_BG_LAST_SCAN = "background_last_scan_at";
    private static final String PREF_BG_LAST_RESULT = "background_last_result_at";
    private static final String PREF_BG_RESULT_COUNT = "background_result_count";
    private static final String PREF_BG_LAST_ERROR = "background_last_error";
    private static final String CHANNEL_RUNNING = "kuba_nearby_background";
    private static final String CHANNEL_ALERTS = "kuba_nearby_watch";
    private static final int RUNNING_NOTIFICATION_ID = 62001;
    private static final long WINDOW_MS = 11000L;
    private static final long PAUSE_MS = 7000L;
    private static final long ALERT_COOLDOWN_MS = 60000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Long> alertTimes = new HashMap<>();
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private Runnable cycle;
    private long resultCount;
    private String latestName = "čekám na signál";

    @Override public void onCreate() {
        super.onCreate();
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
        createChannels();
        SharedPreferences prefs = prefs();
        resultCount = prefs.getLong(PREF_BG_RESULT_COUNT, 0L);
        startForeground(RUNNING_NOTIFICATION_ID, runningNotification("Spouštím hlídání Bluetooth"));
        if (!prefs.getBoolean(PREF_BACKGROUND_SCAN, true)) {
            stopSelf();
            return;
        }
        cycle = this::startWindow;
        handler.post(cycle);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            prefs().edit().putBoolean(PREF_BACKGROUND_SCAN, false).apply();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!prefs().getBoolean(PREF_BACKGROUND_SCAN, true)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void startWindow() {
        stopActiveScan();
        if (!hasScanPermission()) {
            recordError("Chybí oprávnění Bluetooth scan");
            scheduleNext(30000L);
            return;
        }
        if (adapter == null || !adapter.isEnabled()) {
            recordError("Bluetooth je vypnuté nebo nedostupné");
            scheduleNext(30000L);
            return;
        }
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            recordError("BLE scanner není dostupný");
            scheduleNext(30000L);
            return;
        }
        prefs().edit().putLong(PREF_BG_LAST_SCAN, System.currentTimeMillis())
                .putString(PREF_BG_LAST_ERROR, "").apply();
        startScan(false);
    }

    private void startScan(boolean compatible) {
        final ScanCallback callback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult result) {
                handleResult(result);
            }
            @Override public void onBatchScanResults(List<ScanResult> results) {
                if (results == null) return;
                for (ScanResult result : results) handleResult(result);
            }
            @Override public void onScanFailed(int errorCode) {
                if (scanCallback != this) return;
                stopActiveScan();
                if (!compatible) {
                    recordError("Rozšířený BLE režim selhal, přepínám na kompatibilní (" + errorCode + ")");
                    handler.postDelayed(() -> startScan(true), 700L);
                } else {
                    recordError("BLE hlídání selhalo, kód " + errorCode);
                    scheduleNext(30000L);
                }
            }
        };
        scanCallback = callback;
        try {
            ScanSettings.Builder settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            if (!compatible) settings.setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED);
            scanner.startScan(null, settings.build(), callback);
            updateRunningNotification();
            handler.postDelayed(() -> {
                if (scanCallback != callback) return;
                stopActiveScan();
                scheduleNext(PAUSE_MS);
            }, WINDOW_MS);
        } catch (SecurityException | IllegalArgumentException | IllegalStateException ex) {
            if (scanCallback == callback) scanCallback = null;
            if (!compatible) handler.postDelayed(() -> startScan(true), 500L);
            else {
                recordError("BLE scan nelze spustit: " + ex.getClass().getSimpleName());
                scheduleNext(30000L);
            }
        }
    }

    private void handleResult(ScanResult result) {
        if (result == null || result.getDevice() == null) return;
        BluetoothDevice device = result.getDevice();
        String address;
        String name = "";
        try {
            address = device.getAddress();
            ScanRecord record = result.getScanRecord();
            if (record != null) name = record.getDeviceName();
            if (TextUtils.isEmpty(name) && hasConnectPermission()) name = device.getName();
        } catch (SecurityException ex) {
            return;
        }
        if (TextUtils.isEmpty(address)) return;
        if (TextUtils.isEmpty(name)) name = "Neznámé Bluetooth zařízení";
        latestName = name;
        resultCount++;
        long now = System.currentTimeMillis();
        prefs().edit().putLong(PREF_BG_LAST_RESULT, now)
                .putLong(PREF_BG_RESULT_COUNT, resultCount).apply();

        Intent update = new Intent(ACTION_RESULT).setPackage(getPackageName());
        update.putExtra(EXTRA_ADDRESS, address);
        update.putExtra(EXTRA_NAME, name);
        update.putExtra(EXTRA_RSSI, result.getRssi());
        ScanRecord record = result.getScanRecord();
        if (record != null && record.getBytes() != null) update.putExtra(EXTRA_RECORD, record.getBytes());
        sendBroadcast(update);
        maybeNotifyWatched("BT:" + address, name, result.getRssi(), now);
        if (resultCount % 8 == 0) updateRunningNotification();
    }

    private void maybeNotifyWatched(String key, String name, int rssi, long now) {
        SharedPreferences prefs = prefs();
        if (!prefs.getBoolean(PREF_ALERTS, true) || prefs.getBoolean(PREF_APP_VISIBLE, false)) return;
        String watched = prefs.getString(PREF_WATCHED, "");
        if (watched == null || !containsLine(watched, key)) return;
        Long last = alertTimes.get(key);
        if (last != null && now - last < ALERT_COOLDOWN_MS) return;
        alertTimes.put(key, now);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, key.hashCode(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL_ALERTS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Sledované zařízení je poblíž")
                .setContentText(name + " • " + rssi + " dBm")
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(Math.abs(key.hashCode()), notification);
    }

    private boolean containsLine(String text, String wanted) {
        for (String line : text.split("\\n")) if (wanted.equals(line.trim())) return true;
        return false;
    }

    private void scheduleNext(long delayMs) {
        if (cycle != null) handler.postDelayed(cycle, delayMs);
    }

    private void stopActiveScan() {
        if (scanner != null && scanCallback != null) {
            try { scanner.stopScan(scanCallback); } catch (Exception ignored) { }
        }
        scanCallback = null;
    }

    private void recordError(String message) {
        prefs().edit().putString(PREF_BG_LAST_ERROR, message).apply();
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(RUNNING_NOTIFICATION_ID, runningNotification(message));
    }

    private void updateRunningNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(RUNNING_NOTIFICATION_ID,
                runningNotification("Nálezů " + resultCount + " • " + latestName));
    }

    private Notification runningNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, BackgroundBleScanService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_RUNNING)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("KUBA Scanner • hlídání na pozadí")
                .setContentText(text)
                .setContentIntent(openPending)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,
                        "Zastavit", stopPending).build())
                .build();
    }

    private void createChannels() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel running = new NotificationChannel(CHANNEL_RUNNING,
                "KUBA skenování na pozadí", NotificationManager.IMPORTANCE_LOW);
        running.setDescription("Trvalé BLE hlídání, dokud je služba zapnutá");
        manager.createNotificationChannel(running);
        NotificationChannel alerts = new NotificationChannel(CHANNEL_ALERTS,
                "KUBA Nearby sledování", NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(alerts);
    }

    private boolean hasScanPermission() {
        return Build.VERSION.SDK_INT < 31
                ? checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                : checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasConnectPermission() {
        return Build.VERSION.SDK_INT < 31
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private SharedPreferences prefs() { return getSharedPreferences(PREFS, MODE_PRIVATE); }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopActiveScan();
        super.onDestroy();
    }
}
