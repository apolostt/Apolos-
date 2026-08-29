package com.kuba.nearbyscanner;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GnssStatus;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 42;
    private static final int REQUEST_GNSS_PERMISSION = 43;
    private static final String PREFS = "kuba_nearby_prefs";
    private static final String PREF_WATCHED = "watched_devices";
    private static final String PREF_ALERTS = "watch_alerts";
    private static final String PREF_BACKGROUND_SCAN = "background_scan_enabled";
    private static final String PREF_APP_VISIBLE = "app_visible";
    private static final String PREF_BG_LAST_SCAN = "background_last_scan_at";
    private static final String PREF_BG_LAST_RESULT = "background_last_result_at";
    private static final String PREF_BG_RESULT_COUNT = "background_result_count";
    private static final String PREF_BG_LAST_ERROR = "background_last_error";
    private static final String NOTIFY_CHANNEL = "kuba_nearby_watch";
    private static final int BG = Color.rgb(7, 16, 24);
    private static final int SURFACE = Color.rgb(16, 29, 40);
    private static final int SURFACE_2 = Color.rgb(24, 42, 55);
    private static final int TEXT = Color.rgb(244, 248, 250);
    private static final int MUTED = Color.rgb(157, 177, 190);
    private static final int ACCENT = Color.rgb(73, 214, 178);
    private static final int CAMERA = Color.rgb(255, 103, 116);
    private static final int AUDIO = Color.rgb(151, 123, 255);
    private static final int WIFI = Color.rgb(77, 166, 255);
    private static final int BLUETOOTH = Color.rgb(78, 205, 255);
    private static final int TRACKER = Color.rgb(255, 184, 77);
    private static final int IOT = Color.rgb(76, 225, 135);
    private static final int WEARABLE = Color.rgb(255, 122, 205);
    private static final int SMART_HOME = Color.rgb(119, 222, 255);
    private static final int VEHICLE = Color.rgb(255, 213, 90);
    private static final int COMPUTER = Color.rgb(167, 188, 255);
    private static final int PHONE = Color.rgb(99, 230, 190);
    private static final int SCAN_DURATION_MS = 60000;
    private static final long BLUETOOTH_STALE_MS = 45000;
    private static final long BLUETOOTH_REMOVE_MS = 90000;
    private static final long RADIO_STALE_MS = 75000;
    private static final long RADIO_REMOVE_MS = 120000;
    private static final long WIFI_RESULT_MAX_AGE_MS = 45000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Map<String, DeviceInfo> devices = new LinkedHashMap<>();
    private final Map<String, TextView> filterViews = new HashMap<>();
    private final Set<String> watchedKeys = new HashSet<>();
    private final Map<String, Long> watchAlertTimes = new HashMap<>();
    private final Set<String> scanBtKeys = new HashSet<>();
    private final Set<String> scanWifiKeys = new HashSet<>();
    private final Set<String> scanNetworkKeys = new HashSet<>();
    private final Set<String> scanHiddenKeys = new HashSet<>();
    private final Set<String> scanWarnings = new HashSet<>();
    private final List<GnssSatelliteInfo> gnssSatellites = new ArrayList<>();

    private DeviceCatalog catalog;
    private MdnsScanner mdnsScanner;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private WifiManager wifiManager;
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private Sensor rotationSensor;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private ScanCallback bleCallback;
    private ScanCallback trackingBleCallback;
    private ScanCallback presenceBleCallback;
    private boolean receiverRegistered;
    private boolean backgroundReceiverRegistered;
    private boolean scanning;
    private boolean appVisible;
    private boolean backgroundScanEnabled = true;
    private boolean headingRegistered;
    private boolean sortByDistance = true;
    private boolean watchAlertsEnabled = true;
    private long scanStartedAt;
    private String trackingKey = "";
    private int trackingLastRssi = Integer.MIN_VALUE;
    private int trackingBestRssi = Integer.MIN_VALUE;
    private float trackingBestHeadingDeg = -1f;
    private float gpsBestBearingDeg = -1f;
    private int gpsBestRssi = Integer.MIN_VALUE;
    private int gpsLastRssi = Integer.MIN_VALUE;
    private Location previousTrackingLocation;
    private Location currentTrackingLocation;
    private float currentHeadingDeg = -1f;
    private final float[] accelValues = new float[3];
    private final float[] magnetValues = new float[3];
    private boolean haveAccel;
    private boolean haveMagnet;
    private Runnable trackingWifiTick;
    private Runnable bluetoothPresenceTick;
    private Runnable watchedWifiTick;
    private Runnable rangePruneTick;
    private GnssStatus.Callback gnssStatusCallback;
    private Location gnssLastLocation;
    private boolean gnssRunning;
    private boolean gnssEngineStarted;
    private int gnssFirstFixMs = -1;
    private String activeFilter = "ALL";
    private final Runnable scanProgressTick = new Runnable() {
        @Override public void run() {
            if (!scanning) return;
            updateScanProgress();
            handler.postDelayed(this, 500);
        }
    };
    private final SensorEventListener headingListener = new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                float[] matrix = new float[9];
                float[] orientation = new float[3];
                SensorManager.getRotationMatrixFromVector(matrix, event.values);
                SensorManager.getOrientation(matrix, orientation);
                updateSmoothedHeading(normalizeDegrees((float) Math.toDegrees(orientation[0])));
            } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                System.arraycopy(event.values, 0, accelValues, 0, accelValues.length);
                haveAccel = true;
                updateFallbackHeading();
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                System.arraycopy(event.values, 0, magnetValues, 0, magnetValues.length);
                haveMagnet = true;
                updateFallbackHeading();
            }
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };
    private final LocationListener trackingLocationListener = new LocationListener() {
        @Override public void onLocationChanged(Location location) {
            if (location == null || TextUtils.isEmpty(trackingKey)) return;
            if (currentTrackingLocation != null && currentTrackingLocation.distanceTo(location) >= 0.6f) {
                previousTrackingLocation = currentTrackingLocation;
            }
            currentTrackingLocation = location;
        }
    };
    private final LocationListener gnssLocationListener = new LocationListener() {
        @Override public void onLocationChanged(Location location) {
            if (location == null) return;
            gnssLastLocation = location;
            renderGnssPanel();
        }
    };

    private LinearLayout deviceList;
    private TextView statusView;
    private TextView sortChip;
    private TextView totalCount;
    private TextView cameraCount;
    private TextView audioCount;
    private Button scanButton;
    private LinearLayout drawer;
    private LinearLayout drawerBrandItems;
    private View drawerScrim;
    private ScrollView deviceScroll;
    private LinearLayout radarSection;
    private RadarView radarView;
    private RadarView fullscreenRadarView;
    private ScrollView securityScroll;
    private LinearLayout securityContent;
    private ScrollView databaseScroll;
    private LinearLayout databaseContent;
    private ScrollView settingsScroll;
    private LinearLayout settingsContent;
    private ScrollView gnssScroll;
    private LinearLayout gnssContent;
    private TextView drawerBrandToggle;
    private boolean drawerBrandsExpanded;
    private final DeviceSecurityScanner securityScanner = new DeviceSecurityScanner();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = parcelable(intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                BluetoothClass clazz = parcelable(intent, BluetoothDevice.EXTRA_CLASS, BluetoothClass.class);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                if (device != null) addBluetoothDevice(device, clazz, null, rssi, "Bluetooth Classic");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setStatus("Bluetooth dokončen • probíhá síťová detekce");
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                collectWifiResults();
            }
        }
    };
    private final BroadcastReceiver backgroundResultReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || !BackgroundBleScanService.ACTION_RESULT.equals(intent.getAction())) return;
            String address = intent.getStringExtra(BackgroundBleScanService.EXTRA_ADDRESS);
            if (TextUtils.isEmpty(address) || bluetoothAdapter == null) return;
            int rssi = intent.getIntExtra(BackgroundBleScanService.EXTRA_RSSI, 0);
            byte[] raw = intent.getByteArrayExtra(BackgroundBleScanService.EXTRA_RECORD);
            try {
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                addBluetoothDevice(device, device.getBluetoothClass(), null, rssi,
                        "Bluetooth LE • služba na pozadí", intent.getStringExtra(BackgroundBleScanService.EXTRA_NAME), raw);
            } catch (IllegalArgumentException | SecurityException ignored) { }
        }
    };

    @SuppressWarnings("deprecation")
    private static <T> T parcelable(Intent intent, String key, Class<T> type) {
        if (Build.VERSION.SDK_INT >= 33) return intent.getParcelableExtra(key, type);
        return (T) intent.getParcelableExtra(key);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        loadWatchSettings();
        createNotificationChannel();
        catalog = DeviceCatalog.load(getApplicationContext());
        mdnsScanner = new MdnsScanner(getApplicationContext(), new MdnsScanner.Callback() {
            @Override public void onService(String name, String type, String host, int port) {
                addMdnsDevice(name, type, host, port);
            }
            @Override public void onInfo(String message) { addSystemInfo(message); }
        });
        setupRadios();
        registerScannerReceiver();
        registerBackgroundResultReceiver();
        startRangePruneLoop();
        handler.postDelayed(this::ensurePermissionsAndScan, 350);
    }

    private void setupRadios() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = manager == null ? null : manager.getAdapter();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
    }

    private void registerScannerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_EXPORTED);
        else registerReceiver(receiver, filter);
        receiverRegistered = true;
    }

    private void registerBackgroundResultReceiver() {
        IntentFilter filter = new IntentFilter(BackgroundBleScanService.ACTION_RESULT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(backgroundResultReceiver, filter, RECEIVER_NOT_EXPORTED);
        else registerReceiver(backgroundResultReceiver, filter);
        backgroundReceiverRegistered = true;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));
        root.setBackgroundColor(BG);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu = label("☰", 27, TEXT, Typeface.NORMAL);
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription("Otevřít sekce");
        menu.setOnClickListener(v -> openDrawer());
        titleRow.addView(menu, new LinearLayout.LayoutParams(dp(40), dp(48)));
        TextView logo = label("◉", 28, ACCENT, Typeface.BOLD);
        titleRow.addView(logo, new LinearLayout.LayoutParams(dp(36), dp(48)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = label("KUBA Nearby Scanner", 22, TEXT, Typeface.BOLD);
        TextView subtitle = label("Bluetooth • BLE • Wi‑Fi • hodinky • trackery", 12, MUTED, Typeface.NORMAL);
        titles.addView(title);
        titles.addView(subtitle);
        titleRow.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        scanButton = new Button(this);
        scanButton.setText("SKEN");
        scanButton.setTextColor(BG);
        scanButton.setTextSize(13);
        scanButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        scanButton.setAllCaps(false);
        scanButton.setBackground(round(ACCENT, 14, 0, 0));
        scanButton.setOnClickListener(v -> ensurePermissionsAndScan());
        titleRow.addView(scanButton, new LinearLayout.LayoutParams(dp(84), dp(44)));
        root.addView(titleRow);

        statusView = label("Připraveno ke skenování", 13, MUTED, Typeface.NORMAL);
        statusView.setPadding(dp(14), dp(10), dp(14), dp(10));
        statusView.setBackground(round(SURFACE, 13, Color.rgb(39, 64, 79), 1));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(statusView, statusParams);

        LinearLayout counters = new LinearLayout(this);
        counters.setWeightSum(3);
        totalCount = addCounter(counters, "0", "ZAŘÍZENÍ", ACCENT);
        cameraCount = addCounter(counters, "0", "MOŽNÉ KAMERY", CAMERA);
        audioCount = addCounter(counters, "0", "ZVUK", AUDIO);
        root.addView(counters);

        HorizontalScrollView filters = new HorizontalScrollView(this);
        filters.setHorizontalScrollBarEnabled(false);
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setPadding(0, dp(14), 0, dp(12));
        addFilter(filterRow, "ALL", "Vše");
        addFilter(filterRow, "TRACKER", "Trackery");
        addFilter(filterRow, "WEARABLE", "Hodinky");
        addFilter(filterRow, "MI_BAND", "Mi Band");
        addFilter(filterRow, "APPLE", "Apple");
        addFilter(filterRow, "SAMSUNG", "Samsung");
        addFilter(filterRow, "GARMIN", "Garmin");
        addFilter(filterRow, "FITBIT", "Fitbit");
        addFilter(filterRow, "AMAZFIT", "Amazfit");
        addFilter(filterRow, "HUAWEI", "Huawei");
        addFilter(filterRow, "ASUS", "Asus");
        addFilter(filterRow, "OTHER_WEARABLE", "Další hodinky");
        addFilter(filterRow, "CAMERA", "Kamery");
        addFilter(filterRow, "SPY", "Spy / sledování");
        addFilter(filterRow, "AUDIO", "Zvuk");
        addFilter(filterRow, "SCOOTER", "Koloběžky");
        addFilter(filterRow, "SMART_HOME", "Chytrá domácnost");
        addFilter(filterRow, "COMPUTER", "PC");
        addFilter(filterRow, "PHONE", "Telefony");
        addFilter(filterRow, "WIFI", "Wi‑Fi");
        addFilter(filterRow, "BLUETOOTH", "Bluetooth");
        addFilter(filterRow, "HIDDEN", "Skryté názvy");
        addFilter(filterRow, "WATCHED", "Sledované");
        addFilter(filterRow, "IOT", "IoT moduly");
        addFilter(filterRow, "NETWORK", "Síť");
        addBrandMenu(filterRow);
        addSortToggle(filterRow);
        filters.addView(filterRow);
        root.addView(filters);

        deviceScroll = new ScrollView(this);
        deviceScroll.setFillViewport(true);
        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        deviceScroll.addView(deviceList, new ScrollView.LayoutParams(-1, -2));
        root.addView(deviceScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        radarSection = buildRadarSection();
        radarSection.setVisibility(View.GONE);
        root.addView(radarSection, new LinearLayout.LayoutParams(-1, 0, 1));

        securityScroll = buildSecuritySection();
        securityScroll.setVisibility(View.GONE);
        root.addView(securityScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        databaseScroll = buildDatabaseSection();
        databaseScroll.setVisibility(View.GONE);
        root.addView(databaseScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        settingsScroll = buildSettingsSection();
        settingsScroll.setVisibility(View.GONE);
        root.addView(settingsScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        gnssScroll = buildGnssSection();
        gnssScroll.setVisibility(View.GONE);
        root.addView(gnssScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView foot = label("Klepnutím otevřeš úplný detail. Vzdálenost i označení trackeru jsou odhady podle vysílaných dat; náhodná MAC může skrýt výrobce.", 11, MUTED, Typeface.NORMAL);
        foot.setPadding(dp(3), dp(9), dp(3), dp(3));
        root.addView(foot);

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BG);
        frame.addView(root, new FrameLayout.LayoutParams(-1, -1));

        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(Color.argb(165, 0, 0, 0));
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(v -> closeDrawer());
        frame.addView(drawerScrim, new FrameLayout.LayoutParams(-1, -1));

        drawer = buildDrawer();
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(dp(310), -1, Gravity.START);
        frame.addView(drawer, drawerParams);
        drawer.setVisibility(View.GONE);
        setContentView(frame);
        showEmptyState();
    }

    private TextView addCounter(LinearLayout parent, String value, String caption, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(6), dp(11), dp(6), dp(10));
        box.setBackground(round(SURFACE, 14, 0, 0));
        TextView number = label(value, 22, color, Typeface.BOLD);
        TextView text = label(caption, 9, MUTED, Typeface.BOLD);
        text.setGravity(Gravity.CENTER);
        box.addView(number);
        box.addView(text);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(70), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(box, params);
        return number;
    }

    private LinearLayout buildRadarSection() {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        TextView heading = label("3D MAPA OKOLNÍCH SIGNÁLŮ", 15, TEXT, Typeface.BOLD);
        heading.setPadding(dp(2), dp(12), 0, dp(8));
        section.addView(heading);

        radarView = new RadarView(this);
        LinearLayout modes = new LinearLayout(this);
        addRadarMode(modes, radarView, "Vše", RadarView.MODE_ALL, true);
        addRadarMode(modes, radarView, "Bluetooth", RadarView.MODE_BLUETOOTH, false);
        addRadarMode(modes, radarView, "Wi‑Fi", RadarView.MODE_WIFI, false);
        addRadarMode(modes, radarView, "Skryté", RadarView.MODE_HIDDEN, false);
        section.addView(modes, new LinearLayout.LayoutParams(-1, dp(43)));

        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(-1, 0, 1);
        mapParams.setMargins(0, dp(9), 0, dp(8));
        section.addView(radarView, mapParams);
        Button fullscreen = new Button(this);
        fullscreen.setText("ROZTÁHNOUT MAPU NA CELOU OBRAZOVKU");
        fullscreen.setTextColor(BG);
        fullscreen.setTextSize(11);
        fullscreen.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        fullscreen.setBackground(round(ACCENT, 13, 0, 0));
        fullscreen.setOnClickListener(v -> showFullscreenRadar());
        section.addView(fullscreen, new LinearLayout.LayoutParams(-1, dp(44)));
        TextView note = label("Barva ukazuje kategorii, poloha kolem středu je stabilní vizualizace podle identifikátoru. Poloměr vychází z RSSI a není skutečný směr zařízení.", 11, MUTED, Typeface.NORMAL);
        note.setPadding(dp(3), dp(4), dp(3), dp(8));
        section.addView(note);
        return section;
    }

    private void addRadarMode(LinearLayout parent, RadarView target, String text, int mode, boolean selected) {
        TextView chip = label(text, 11, selected ? BG : MUTED, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setTag(mode);
        chip.setBackground(round(selected ? ACCENT : SURFACE, 14, 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(38), 1);
        params.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> {
            int chosen = (Integer) v.getTag();
            target.setMode(chosen);
            for (int i = 0; i < parent.getChildCount(); i++) {
                TextView item = (TextView) parent.getChildAt(i);
                boolean active = ((Integer) item.getTag()) == chosen;
                item.setTextColor(active ? BG : MUTED);
                item.setBackground(round(active ? ACCENT : SURFACE, 14, 0, 0));
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void showFullscreenRadar() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_NoActionBar);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(8));
        root.setBackgroundColor(BG);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(label("3D SKENER • CELOOBRAZOVKOVÁ MAPA", 15, TEXT, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, dp(45), 1));
        Button reset = new Button(this);
        reset.setText("RESET");
        reset.setTextColor(TEXT);
        reset.setTextSize(10);
        reset.setBackground(round(SURFACE_2, 12, 0, 0));
        top.addView(reset, new LinearLayout.LayoutParams(dp(72), dp(40)));
        Button close = new Button(this);
        close.setText("×");
        close.setTextColor(TEXT);
        close.setTextSize(20);
        close.setBackground(round(SURFACE_2, 12, 0, 0));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(dp(52), dp(40));
        closeParams.setMargins(dp(6), 0, 0, 0);
        top.addView(close, closeParams);
        root.addView(top);

        RadarView fullRadar = new RadarView(this);
        fullRadar.copyDevicesFrom(radarView);
        fullscreenRadarView = fullRadar;
        LinearLayout modes = new LinearLayout(this);
        int selected = radarView.getMode();
        addRadarMode(modes, fullRadar, "Vše", RadarView.MODE_ALL, selected == RadarView.MODE_ALL);
        addRadarMode(modes, fullRadar, "Bluetooth", RadarView.MODE_BLUETOOTH, selected == RadarView.MODE_BLUETOOTH);
        addRadarMode(modes, fullRadar, "Wi‑Fi", RadarView.MODE_WIFI, selected == RadarView.MODE_WIFI);
        addRadarMode(modes, fullRadar, "Skryté", RadarView.MODE_HIDDEN, selected == RadarView.MODE_HIDDEN);
        root.addView(modes, new LinearLayout.LayoutParams(-1, dp(43)));
        root.addView(fullRadar, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView help = label("Táhni mapu jedním prstem • přibliž/oddal dvěma prsty • dvojklik vrátí výchozí pohled", 11, MUTED, Typeface.NORMAL);
        help.setGravity(Gravity.CENTER);
        help.setPadding(0, dp(6), 0, dp(4));
        root.addView(help);

        reset.setOnClickListener(v -> fullRadar.resetView());
        close.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(root);
        dialog.setOnDismissListener(ignored -> fullscreenRadarView = null);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setStatusBarColor(BG);
            window.setNavigationBarColor(BG);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    private ScrollView buildSecuritySection() {
        ScrollView scroll = new ScrollView(this);
        securityContent = new LinearLayout(this);
        securityContent.setOrientation(LinearLayout.VERTICAL);
        securityContent.setPadding(0, dp(12), 0, dp(12));
        securityContent.addView(label("Kontrola se načte po otevření sekce.", 13, MUTED, Typeface.NORMAL));
        scroll.addView(securityContent, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private ScrollView buildDatabaseSection() {
        ScrollView scroll = new ScrollView(this);
        databaseContent = new LinearLayout(this);
        databaseContent.setOrientation(LinearLayout.VERTICAL);
        databaseContent.setPadding(0, dp(12), 0, dp(12));
        scroll.addView(databaseContent, new ScrollView.LayoutParams(-1, -2));
        refreshDatabasePanel("");
        return scroll;
    }

    private ScrollView buildSettingsSection() {
        ScrollView scroll = new ScrollView(this);
        settingsContent = new LinearLayout(this);
        settingsContent.setOrientation(LinearLayout.VERTICAL);
        settingsContent.setPadding(0, dp(12), 0, dp(12));
        scroll.addView(settingsContent, new ScrollView.LayoutParams(-1, -2));
        refreshSettingsPanel();
        return scroll;
    }

    private ScrollView buildGnssSection() {
        ScrollView scroll = new ScrollView(this);
        gnssContent = new LinearLayout(this);
        gnssContent.setOrientation(LinearLayout.VERTICAL);
        gnssContent.setPadding(0, dp(12), 0, dp(12));
        scroll.addView(gnssContent, new ScrollView.LayoutParams(-1, -2));
        renderGnssPanel();
        return scroll;
    }

    private void startGnssMonitor() {
        if (gnssRunning || locationManager == null) {
            renderGnssPanel();
            return;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            renderGnssPanel();
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_GNSS_PERMISSION);
            return;
        }
        gnssStatusCallback = new GnssStatus.Callback() {
            @Override public void onStarted() {
                gnssEngineStarted = true;
                renderGnssPanel();
            }
            @Override public void onStopped() {
                gnssEngineStarted = false;
                renderGnssPanel();
            }
            @Override public void onFirstFix(int ttffMillis) {
                gnssFirstFixMs = ttffMillis;
                renderGnssPanel();
            }
            @Override public void onSatelliteStatusChanged(GnssStatus status) {
                updateGnssSatellites(status);
            }
        };
        try {
            gnssRunning = locationManager.registerGnssStatusCallback(gnssStatusCallback, handler);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f,
                    gnssLocationListener, Looper.getMainLooper());
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) gnssLastLocation = last;
        } catch (SecurityException | IllegalArgumentException ignored) {
            gnssRunning = false;
        }
        renderGnssPanel();
    }

    private void stopGnssMonitor() {
        if (locationManager == null) return;
        try {
            if (gnssStatusCallback != null) locationManager.unregisterGnssStatusCallback(gnssStatusCallback);
            locationManager.removeUpdates(gnssLocationListener);
        } catch (Exception ignored) { }
        gnssStatusCallback = null;
        gnssRunning = false;
        gnssEngineStarted = false;
    }

    private void updateGnssSatellites(GnssStatus status) {
        List<GnssSatelliteInfo> fresh = new ArrayList<>();
        if (status != null) {
            for (int i = 0; i < status.getSatelliteCount(); i++) {
                float carrierMhz = status.hasCarrierFrequencyHz(i)
                        ? status.getCarrierFrequencyHz(i) / 1000000f : -1f;
                float baseband = Build.VERSION.SDK_INT >= 30 && status.hasBasebandCn0DbHz(i)
                        ? status.getBasebandCn0DbHz(i) : -1f;
                fresh.add(new GnssSatelliteInfo(status.getConstellationType(i), status.getSvid(i),
                        status.getCn0DbHz(i), baseband, status.getElevationDegrees(i),
                        status.getAzimuthDegrees(i), carrierMhz, status.usedInFix(i),
                        status.hasAlmanacData(i), status.hasEphemerisData(i)));
            }
        }
        fresh.sort(Comparator
                .comparing((GnssSatelliteInfo satellite) -> !satellite.usedInFix)
                .thenComparing((GnssSatelliteInfo satellite) -> -satellite.cn0));
        synchronized (gnssSatellites) {
            gnssSatellites.clear();
            gnssSatellites.addAll(fresh);
        }
        renderGnssPanel();
    }

    private void renderGnssPanel() {
        if (gnssContent == null) return;
        handler.post(() -> {
            if (gnssContent == null || isFinishing()) return;
            gnssContent.removeAllViews();
            gnssContent.addView(label("GPS / GNSS SATELITY", 18, TEXT, Typeface.BOLD));
            TextView note = label("Telefon satelity pouze přijímá; k jednotlivému satelitu se ručně nepřipojuje. Seznam ukazuje družice viditelné přijímačem a označí ty, které Android použil pro výpočet polohy.", 12, MUTED, Typeface.NORMAL);
            note.setPadding(0, dp(6), 0, dp(9));
            gnssContent.addView(note);

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                addGnssInfoCard("CHYBÍ OPRÁVNĚNÍ", "Povol přesnou polohu, aby Android zpřístupnil stav GNSS satelitů.", TRACKER);
                addGnssRefreshButton("POVOLIT PŘESNOU POLOHU", this::startGnssMonitor);
                return;
            }
            if (!isLocationEnabled()) {
                addGnssInfoCard("POLOHA JE VYPNUTÁ", "Zapni určování polohy. Pro první fix vyjdi k oknu nebo ven s volným výhledem na oblohu.", TRACKER);
            }

            List<GnssSatelliteInfo> satellites;
            synchronized (gnssSatellites) { satellites = new ArrayList<>(gnssSatellites); }
            int used = 0;
            for (GnssSatelliteInfo satellite : satellites) if (satellite.usedInFix) used++;
            String fix = gnssLastLocation == null ? "čekám na polohu"
                    : "přesnost ±" + Math.round(gnssLastLocation.getAccuracy()) + " m • "
                    + ageText(gnssLastLocation.getTime());
            addGnssInfoCard("STAV PŘIJÍMAČE", (gnssEngineStarted ? "GNSS aktivní" : "GNSS čeká")
                    + " • viditelné " + satellites.size() + " • použité pro fix " + used + "\n" + fix
                    + (gnssFirstFixMs >= 0 ? " • první fix " + gnssFirstFixMs + " ms" : ""), ACCENT);

            if (satellites.isEmpty()) {
                addGnssInfoCard("ČEKÁM NA SATELITY", "První data mohou trvat desítky sekund. Uvnitř budovy může být GNSS signál příliš slabý.", MUTED);
            } else {
                for (GnssSatelliteInfo satellite : satellites) gnssContent.addView(gnssSatelliteCard(satellite));
            }
            addGnssRefreshButton("OBNOVIT GNSS", () -> {
                stopGnssMonitor();
                startGnssMonitor();
            });
        });
    }

    private void addGnssInfoCard(String title, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(SURFACE, 8, color, 1));
        card.addView(label(title, 10, color, Typeface.BOLD));
        TextView body = label(value, 13, TEXT, Typeface.NORMAL);
        body.setPadding(0, dp(5), 0, 0);
        card.addView(body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(7), 0, 0);
        gnssContent.addView(card, params);
    }

    private void addGnssRefreshButton(String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(BG);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(round(ACCENT, 8, 0, 0));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.setMargins(0, dp(11), 0, dp(8));
        gnssContent.addView(button, params);
    }

    private View gnssSatelliteCard(GnssSatelliteInfo satellite) {
        int color = satellite.usedInFix ? ACCENT : signalColorForCn0(satellite.cn0);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(11), dp(14), dp(11));
        card.setBackground(round(SURFACE, 8, color, 1));
        card.addView(label(constellationName(satellite.constellation) + " • SVID " + satellite.svid,
                14, TEXT, Typeface.BOLD));
        card.addView(label(String.format(Locale.forLanguageTag("cs-CZ"),
                "%s • %.1f dB-Hz • elevace %.0f° • azimut %.0f°",
                satellite.usedInFix ? "POUŽIT PRO FIX" : "VIDITELNÝ",
                satellite.cn0, satellite.elevation, satellite.azimuth), 11, color, Typeface.BOLD));
        if (satellite.carrierMhz > 0) card.addView(label(carrierBand(satellite.carrierMhz), 10, MUTED, Typeface.NORMAL));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(7), 0, 0);
        card.setLayoutParams(params);
        card.setOnClickListener(v -> showGnssSatelliteDetails(satellite));
        return card;
    }

    private void showGnssSatelliteDetails(GnssSatelliteInfo satellite) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(18), dp(20), dp(18));
        panel.setBackground(round(SURFACE, 8, signalColorForCn0(satellite.cn0), 1));
        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));
        panel.addView(label(constellationName(satellite.constellation) + " • SVID " + satellite.svid, 22, TEXT, Typeface.BOLD));
        addDetail(panel, "STAV", satellite.usedInFix ? "Použitý pro poslední výpočet polohy" : "Viditelný, ale nepoužitý pro poslední fix");
        addDetail(panel, "KONSTELACE", constellationName(satellite.constellation));
        addDetail(panel, "IDENTIFIKÁTOR SVID", String.valueOf(satellite.svid));
        addDetail(panel, "C/N0 NA ANTÉNĚ", String.format(Locale.forLanguageTag("cs-CZ"), "%.1f dB-Hz • %s", satellite.cn0, cn0Quality(satellite.cn0)));
        if (satellite.basebandCn0 >= 0) addDetail(panel, "BASEBAND C/N0", String.format(Locale.forLanguageTag("cs-CZ"), "%.1f dB-Hz", satellite.basebandCn0));
        addDetail(panel, "ELEVACE", String.format(Locale.forLanguageTag("cs-CZ"), "%.1f° nad horizontem", satellite.elevation));
        addDetail(panel, "AZIMUT", String.format(Locale.forLanguageTag("cs-CZ"), "%.1f° • %s", satellite.azimuth, compassPoint(satellite.azimuth)));
        if (satellite.carrierMhz > 0) addDetail(panel, "NOSNÁ FREKVENCE", carrierBand(satellite.carrierMhz));
        addDetail(panel, "ALMANACH", satellite.hasAlmanac ? "Ano" : "Ne / Android nehlásí");
        addDetail(panel, "EFEMERIDY", satellite.hasEphemeris ? "Ano" : "Ne / Android nehlásí");
        addDetail(panel, "PŘIPOJENÍ", "GNSS je jednosměrný příjem navigačního signálu. Telefon satelitu neposílá data a nelze vybrat ruční připojení.");
        Button close = new Button(this);
        close.setText("ZAVŘÍT DETAIL");
        close.setTextColor(BG);
        close.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        close.setBackground(round(ACCENT, 8, 0, 0));
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(48));
        closeParams.setMargins(0, dp(12), 0, 0);
        panel.addView(close, closeParams);
        dialog.setContentView(scroll);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(getResources().getDisplayMetrics().widthPixels - dp(24),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.88f));
        }
    }

    private int signalColorForCn0(float cn0) {
        if (cn0 >= 35f) return ACCENT;
        if (cn0 >= 25f) return WIFI;
        if (cn0 >= 15f) return TRACKER;
        return CAMERA;
    }

    private String cn0Quality(float cn0) {
        if (cn0 >= 40f) return "velmi silný signál";
        if (cn0 >= 30f) return "dobrý signál";
        if (cn0 >= 20f) return "použitelný/slabší signál";
        return "slabý signál";
    }

    private String constellationName(int constellation) {
        switch (constellation) {
            case GnssStatus.CONSTELLATION_GPS: return "GPS (USA)";
            case GnssStatus.CONSTELLATION_GLONASS: return "GLONASS";
            case GnssStatus.CONSTELLATION_GALILEO: return "Galileo (EU)";
            case GnssStatus.CONSTELLATION_BEIDOU: return "BeiDou";
            case GnssStatus.CONSTELLATION_QZSS: return "QZSS";
            case GnssStatus.CONSTELLATION_SBAS: return "SBAS";
            case GnssStatus.CONSTELLATION_IRNSS: return "NavIC / IRNSS";
            default: return "Neznámá GNSS konstelace";
        }
    }

    private String carrierBand(float mhz) {
        String band = "";
        if (Math.abs(mhz - 1575.42f) < 3f) band = "L1 / E1 / B1C";
        else if (Math.abs(mhz - 1227.60f) < 3f) band = "L2";
        else if (Math.abs(mhz - 1176.45f) < 3f) band = "L5 / E5a / B2a";
        else if (Math.abs(mhz - 1207.14f) < 3f) band = "E5b / B2";
        else if (Math.abs(mhz - 1278.75f) < 3f) band = "E6";
        else if (Math.abs(mhz - 1561.10f) < 3f) band = "B1I";
        else if (mhz >= 1597f && mhz <= 1607f) band = "GLONASS G1";
        return String.format(Locale.forLanguageTag("cs-CZ"), "%.3f MHz%s", mhz,
                band.isEmpty() ? "" : " • " + band);
    }

    private String compassPoint(float azimuth) {
        String[] points = {"sever", "severovýchod", "východ", "jihovýchod", "jih", "jihozápad", "západ", "severozápad"};
        return points[Math.round(normalizeDegrees(azimuth) / 45f) % 8];
    }

    private void refreshDatabasePanel(String resultMessage) {
        if (databaseContent == null) return;
        databaseContent.removeAllViews();
        databaseContent.addView(label("DATABÁZE ZAŘÍZENÍ", 18, TEXT, Typeface.BOLD));
        TextView intro = label("Vestavěný katalog funguje offline. Online aktualizace stáhne veřejné registry přímo od IEEE a uloží je jen do telefonu.", 12, MUTED, Typeface.NORMAL);
        intro.setPadding(0, dp(6), 0, dp(9));
        databaseContent.addView(intro);
        addDatabaseCard("VESTAVĚNÁ DATABÁZE", "39 902 IEEE MA‑L výrobců\n3 997 Bluetooth SIG firem\n783 Bluetooth služeb\n287 Bluetooth typů zařízení\nRozšířený slovník hodinek, náramků, tagů, audio, kamer, spy profilů, koloběžek, smart-home, telefonů, PC/NAS a IoT modulů", ACCENT);
        addDatabaseCard("ONLINE IEEE MA‑L + MA‑M + MA‑S", OnlineCatalogUpdater.status(getApplicationContext()), WIFI);
        if (!TextUtils.isEmpty(resultMessage)) addDatabaseCard("VÝSLEDEK AKTUALIZACE", resultMessage,
                resultMessage.startsWith("Online databáze") ? ACCENT : CAMERA);
        addDatabaseCard("SOUKROMÍ", "Aplikace stahuje celý veřejný seznam. MAC adresy nalezených zařízení neposílá IEEE ani jiné službě.", BLUETOOTH);
        addDatabaseCard("PŘESNOST", "Databáze určuje držitele MAC prefixu nebo výrobce modulu. Neexistuje veřejná databáze, která by spolehlivě znala přesný model každého výrobku.", TRACKER);

        Button update = new Button(this);
        update.setText("AKTUALIZOVAT Z OFICIÁLNÍHO IEEE");
        update.setTextColor(BG);
        update.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        update.setBackground(round(ACCENT, 14, 0, 0));
        update.setOnClickListener(v -> updateOnlineCatalog(update));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.setMargins(0, dp(10), 0, dp(8));
        databaseContent.addView(update, params);

        Button demo = new Button(this);
        demo.setText("PŘIDAT DEMO / FIKTIVNÍ ZAŘÍZENÍ");
        demo.setTextColor(BG);
        demo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        demo.setBackground(round(TRACKER, 14, 0, 0));
        demo.setOnClickListener(v -> showDemoDeviceChooser());
        LinearLayout.LayoutParams demoParams = new LinearLayout.LayoutParams(-1, dp(50));
        demoParams.setMargins(0, dp(4), 0, dp(8));
        databaseContent.addView(demo, demoParams);
    }

    private void refreshSettingsPanel() {
        if (settingsContent == null) return;
        settingsContent.removeAllViews();
        settingsContent.addView(label("NASTAVENÍ SKENERU", 18, TEXT, Typeface.BOLD));
        TextView intro = label("Sledovaná zařízení se ukládají lokálně podle BT/Wi‑Fi klíče. Upozornění vyskočí, když se označené zařízení znovu objeví v dosahu.", 12, MUTED, Typeface.NORMAL);
        intro.setPadding(0, dp(6), 0, dp(9));
        settingsContent.addView(intro);
        addSettingsCard("HLÍDANÁ ZAŘÍZENÍ", watchedKeys.size() + " položek", ACCENT);
        addSettingsCard("UPOZORNĚNÍ", watchAlertsEnabled ? "Zapnuto" : "Vypnuto", watchAlertsEnabled ? ACCENT : MUTED);
        addSettingsCard("BLE HLÍDÁNÍ NA POZADÍ", backgroundScanStatus(), backgroundScanEnabled ? ACCENT : MUTED);
        addSettingsCard("DŮKLADNÝ SKEN", (SCAN_DURATION_MS / 1000) + " s • BLE všech podporovaných PHY + Wi‑Fi + ONVIF/SSDP/mDNS", BLUETOOTH);
        addSettingsCard("WI‑FI BĚHEM OTEVŘENÉ APLIKACE", "Android omezuje skeny na pozadí; okolní Wi‑Fi se proto průběžně obnovuje při běhu aplikace", WIFI);
        addSettingsCard("DIAGNOSTIKA POSLEDNÍHO SKENU", scanSummary() + "\nVarování: " + scanWarnings.size(), scanWarnings.isEmpty() ? ACCENT : TRACKER);
        addSettingsButton(watchAlertsEnabled ? "VYPNOUT UPOZORNĚNÍ" : "ZAPNOUT UPOZORNĚNÍ", ACCENT, () -> {
            watchAlertsEnabled = !watchAlertsEnabled;
            saveWatchSettings();
            refreshSettingsPanel();
        });
        addSettingsButton(backgroundScanEnabled ? "VYPNOUT BLE HLÍDÁNÍ NA POZADÍ" : "ZAPNOUT BLE HLÍDÁNÍ NA POZADÍ", BLUETOOTH, () -> {
            backgroundScanEnabled = !backgroundScanEnabled;
            saveWatchSettings();
            updateBackgroundServiceState();
            refreshSettingsPanel();
        });
        addSettingsButton("OTEVŘÍT BLUETOOTH PÁROVÁNÍ", BLUETOOTH, () -> {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
            catch (Exception ignored) { Toast.makeText(this, "Bluetooth nastavení nejde otevřít", Toast.LENGTH_SHORT).show(); }
        });
        addSettingsButton("OTEVŘÍT WI‑FI NASTAVENÍ", WIFI, () -> openSettingsAction(Settings.ACTION_WIFI_SETTINGS, "Wi‑Fi nastavení nejde otevřít"));
        addSettingsButton("OTEVŘÍT NASTAVENÍ POLOHY", TRACKER, () -> openSettingsAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Nastavení polohy nejde otevřít"));
        addSettingsButton("OTEVŘÍT OPRÁVNĚNÍ APLIKACE", SURFACE_2, () -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ignored) {
                Toast.makeText(this, "Oprávnění aplikace nejdou otevřít", Toast.LENGTH_SHORT).show();
            }
        });
        addSettingsButton("PŘIDAT DEMO ZAŘÍZENÍ", TRACKER, this::showDemoDeviceChooser);
        addSettingsButton("SMAZAT DEMO ZAŘÍZENÍ", CAMERA, this::clearDemoDevices);
        addSettingsButton("VYČISTIT SLEDOVANÉ", SURFACE_2, () -> {
            watchedKeys.clear();
            watchAlertTimes.clear();
            saveWatchSettings();
            renderDevices();
            refreshSettingsPanel();
        });
    }

    private void addSettingsCard(String title, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(SURFACE, 14, color, 1));
        card.addView(label(title, 10, color, Typeface.BOLD));
        TextView body = label(value, 14, TEXT, Typeface.BOLD);
        body.setPadding(0, dp(5), 0, 0);
        card.addView(body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(7), 0, 0);
        settingsContent.addView(card, params);
    }

    private void addSettingsButton(String text, int color, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(color == SURFACE_2 ? TEXT : BG);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(round(color, 14, 0, 0));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.setMargins(0, dp(9), 0, 0);
        settingsContent.addView(button, params);
    }

    private void openSettingsAction(String action, String failMessage) {
        try { startActivity(new Intent(action)); }
        catch (Exception ignored) { Toast.makeText(this, failMessage, Toast.LENGTH_SHORT).show(); }
    }

    private void showDemoDeviceChooser() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        panel.setBackground(round(SURFACE, 18, TRACKER, 1));
        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));
        panel.addView(label("PŘIDAT DEMO ZAŘÍZENÍ", 18, TEXT, Typeface.BOLD));
        TextView note = label("Demo je jen uvnitř aplikace pro test databáze, filtrů a detailu. Nevysílá falešný Bluetooth signál do okolí.", 12, MUTED, Typeface.NORMAL);
        note.setPadding(0, dp(6), 0, dp(10));
        panel.addView(note);
        addDemoOption(panel, dialog, "Apple Watch / Find My", "APPLE_WATCH");
        addDemoOption(panel, dialog, "Samsung Galaxy Watch", "SAMSUNG_WATCH");
        addDemoOption(panel, dialog, "Xiaomi Mi Band", "MI_BAND");
        addDemoOption(panel, dialog, "Garmin sportovní senzor", "GARMIN");
        addDemoOption(panel, dialog, "Fitbit Charge", "FITBIT");
        addDemoOption(panel, dialog, "Amazfit / Zepp", "AMAZFIT");
        addDemoOption(panel, dialog, "Huawei / Honor Band", "HUAWEI");
        addDemoOption(panel, dialog, "Asus / ROG zařízení", "ASUS");
        addDemoOption(panel, dialog, "Bluetooth sluchátka", "HEADPHONES");
        addDemoOption(panel, dialog, "Xiaomi / Ninebot koloběžka", "SCOOTER");
        addDemoOption(panel, dialog, "Chytrá domácnost / Tuya / Matter", "SMART_HOME");
        addDemoOption(panel, dialog, "Telefon / tablet", "PHONE");
        addDemoOption(panel, dialog, "Počítač / NAS / Raspberry Pi", "COMPUTER");
        addDemoOption(panel, dialog, "Spy tracker BLE", "SPY_TRACKER");
        addDemoOption(panel, dialog, "Wi‑Fi kamera / ONVIF", "WIFI_CAMERA");
        Button close = new Button(this);
        close.setText("ZAVŘÍT");
        close.setTextColor(TEXT);
        close.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        close.setBackground(round(SURFACE_2, 14, 0, 0));
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(46));
        closeParams.setMargins(0, dp(10), 0, 0);
        panel.addView(close, closeParams);
        dialog.setContentView(scroll);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(getResources().getDisplayMetrics().widthPixels - dp(28),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void addDemoOption(LinearLayout panel, Dialog dialog, String title, String type) {
        TextView row = label(title, 14, TEXT, Typeface.BOLD);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(round(Color.rgb(12, 25, 35), 13, Color.rgb(43, 66, 82), 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(6), 0, 0);
        panel.addView(row, params);
        row.setOnClickListener(v -> {
            addDemoDevice(type);
            dialog.dismiss();
            selectSection("ALL");
        });
    }

    private void addDemoDevice(String type) {
        long now = System.currentTimeMillis();
        DeviceInfo info;
        switch (type) {
            case "APPLE_WATCH":
                info = demoBt("DEMO:APPLE:" + now, "Apple Watch Demo", "DE:MO:A1:00:00:01", "Apple, Inc.",
                        "Apple Watch / Find My profil", "WEARABLE", -61, "0x004C – Apple, Inc.",
                        "Complete Name: Apple Watch; Manufacturer Data: Apple Nearby/Find My; Services: Current Time, Battery, Device Information");
                break;
            case "SAMSUNG_WATCH":
                info = demoBt("DEMO:SAMSUNG:" + now, "Galaxy Watch Demo", "DE:MO:5A:00:00:02", "Samsung Electronics",
                        "Samsung Galaxy Watch", "WEARABLE", -64, "Samsung service FD5A/FD4B",
                        "Complete Name: Galaxy Watch; Services: Battery, Heart Rate, Device Information; Manufacturer Data: Samsung");
                break;
            case "MI_BAND":
                info = demoBt("DEMO:MIBAND:" + now, "Mi Band Demo", "DE:MO:46:00:00:03", "Xiaomi / Huami",
                        "Xiaomi Smart Band / Mi Band", "WEARABLE", -67, "Xiaomi/Huami BLE profil",
                        "Complete Name: Mi Smart Band; Services: Battery, Heart Rate, Fitness Machine; Manufacturer Data: Xiaomi");
                break;
            case "GARMIN":
                info = demoBt("DEMO:GARMIN:" + now, "Garmin Sensor Demo", "DE:MO:6A:00:00:04", "Garmin",
                        "Garmin hodinky nebo sportovní senzor", "WEARABLE", -70, "Garmin sportovní BLE služby",
                        "Services: Heart Rate, Running Speed, Cycling Speed, Cycling Power, Location and Navigation");
                break;
            case "FITBIT":
                info = demoBt("DEMO:FITBIT:" + now, "Fitbit Charge Demo", "DE:MO:F1:00:00:05", "Google Fitbit",
                        "Fitbit Charge / Sense / Versa", "WEARABLE", -69, "Fitbit zdravotní BLE profil",
                        "Services: Battery, Heart Rate, Body Composition, Device Information");
                break;
            case "AMAZFIT":
                info = demoBt("DEMO:AMAZFIT:" + now, "Amazfit Zepp Demo", "DE:MO:ZE:00:00:06", "Amazfit / Zepp / Huami",
                        "Amazfit/Zepp hodinky nebo náramek", "WEARABLE", -66, "Amazfit/Zepp BLE název",
                        "Complete Name: Amazfit GTR; Services: Battery, Heart Rate, Device Information");
                break;
            case "HUAWEI":
                info = demoBt("DEMO:HUAWEI:" + now, "Huawei Band Demo", "DE:MO:HU:00:00:07", "Huawei / Honor",
                        "Huawei/Honor Watch nebo Band", "WEARABLE", -68, "Huawei/Honor wearable profil",
                        "Complete Name: HUAWEI Band; Services: Battery, Heart Rate, Device Information");
                break;
            case "ASUS":
                info = demoBt("DEMO:ASUS:" + now, "ROG Device Demo", "DE:MO:AS:00:00:08", "ASUSTek Computer Inc.",
                        "Asus / ROG telefon, periferie nebo audio", "BLUETOOTH", -63, "Asus/ROG název nebo výrobce",
                        "Complete Name: ROG Phone Audio; Services: Battery, HID, Device Information");
                break;
            case "HEADPHONES":
                info = demoBt("DEMO:AUDIO:" + now, "Bluetooth Headphones Demo", "DE:MO:AU:00:00:09", "Audio",
                        "Bluetooth sluchátka / headset", "AUDIO", -58, "Audio BLE a Bluetooth profil",
                        "Services: Audio Stream Control, Media Control, Volume Control, Battery");
                break;
            case "SCOOTER":
                info = demoBt("DEMO:SCOOTER:" + now, "Mi Electric Scooter Demo", "DE:MO:SC:00:00:11", "Xiaomi / Ninebot / Segway",
                        "Elektrokoloběžka nebo e-bike BLE modul", "SCOOTER", -73, "Xiaomi/Ninebot BLE servis",
                        "Complete Name: Mi Scooter; Services: Battery, Device Information, custom telemetry; Manufacturer Data: motor/battery module");
                break;
            case "SMART_HOME":
                info = new DeviceInfo("DEMO:SMARTHOME:" + now, "Tuya Matter Sensor Demo", "192.168.1.55",
                        "DEMO • Wi‑Fi + mDNS", "SMART_HOME", -62, wifiDistance(-62, 2462),
                        94, "Matter/HomeKit/Tuya smart-home služba", "WPA2");
                info.vendor = "Tuya / Smart Life";
                info.model = "Chytrý senzor, zásuvka, světlo nebo gateway";
                info.services = "_matter._tcp • _hap._tcp • HTTP API";
                info.frequency = "2462 MHz • kanál 11";
                info.security = "WPA2";
                info.protocolData = "mDNS: _matter._tcp, _hap._tcp; lokální smart-home profil";
                info.transport = "WIFI";
                break;
            case "PHONE":
                info = demoBt("DEMO:PHONE:" + now, "Galaxy Phone Demo", "DE:MO:PH:00:00:12", "Samsung / Android",
                        "Telefon nebo tablet podle Bluetooth třídy a názvu", "PHONE", -60, "Bluetooth transport telefonu",
                        "Complete Name: Galaxy Z; Class: Phone; Services: Battery, Device Information, LE Audio");
                break;
            case "COMPUTER":
                info = new DeviceInfo("DEMO:COMPUTER:" + now, "Raspberry Pi / NAS Demo", "192.168.1.44",
                        "DEMO • mDNS / SMB", "COMPUTER", -57, -1,
                        92, "mDNS/SMB služba odpovídá počítači nebo NAS", "Porty a služby v lokální síti");
                info.vendor = "Raspberry Pi / Synology / QNAP";
                info.model = "Počítač, NAS nebo malý embedded Linux";
                info.services = "_smb._tcp • _workstation._tcp • _http._tcp";
                info.protocolData = "mDNS služby pracovního zařízení";
                info.transport = "NETWORK";
                break;
            case "SPY_TRACKER":
                info = demoBt("DEMO:SPY:" + now, "Skrytý BLE tracker Demo", "DE:MO:SP:00:00:10", "Neurčeno",
                        "Možný BLE tracker / sledovací tag", "TRACKER", -74, "Skrytý název + tracker služby",
                        "No complete name; Service Data: locator/tracker profile; Manufacturer Data: krátký anonymní beacon");
                info.hiddenName = true;
                break;
            case "WIFI_CAMERA":
                info = new DeviceInfo("DEMO:WIFICAM:" + now, "Wi‑Fi ONVIF kamera Demo", "192.168.1.88",
                        "DEMO • Wi‑Fi + ONVIF", "CAMERA", -65, wifiDistance(-65, 2412),
                        96, "ONVIF / RTSP / NetworkVideoTransmitter profil", "WPA2");
                info.vendor = "Kamerový modul";
                info.model = "IP kamera / NVR profil";
                info.services = "ONVIF WS‑Discovery • RTSP • HTTP";
                info.frequency = "2412 MHz • kanál 1";
                info.security = "WPA2";
                info.protocolData = "Scopes: onvif://www.onvif.org/Profile/Streaming; rtsp://192.168.1.88/live";
                info.transport = "WIFI";
                info.hiddenName = false;
                break;
            default:
                return;
        }
        info.demo = true;
        info.inRange = true;
        addOrUpdate(info);
    }

    private DeviceInfo demoBt(String key, String name, String address, String vendor, String model,
                              String category, int rssi, String manufacturer, String decoded) {
        DeviceInfo info = new DeviceInfo(key, name, address, "DEMO • virtuální párování v aplikaci",
                category, rssi, distanceFromRssi(rssi, -59, 2.2), 94,
                "Demo profil pro test rozpoznání značky, sekce a detailu", "");
        info.vendor = vendor;
        info.model = model;
        info.manufacturerIds = manufacturer;
        info.deviceClass = "BLE profil / simulovaná zařízení v okolí";
        info.services = decoded;
        info.decodedName = name;
        info.decodedData = decoded;
        info.rawAdvertisement = "DEMO DATA - simulovaná BLE reklama uvnitř aplikace";
        info.addressType = "DEMO adresa - není reálná MAC v okolí";
        info.transport = "BLUETOOTH";
        return info;
    }

    private void clearDemoDevices() {
        synchronized (devices) {
            List<String> remove = new ArrayList<>();
            for (DeviceInfo device : devices.values()) if (device.demo) remove.add(device.key);
            for (String key : remove) devices.remove(key);
        }
        renderDevices();
        Toast.makeText(this, "Demo zařízení smazána", Toast.LENGTH_SHORT).show();
    }

    private void addDatabaseCard(String title, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(SURFACE, 14, color, 1));
        card.addView(label(title, 10, color, Typeface.BOLD));
        TextView body = label(value, 12, TEXT, Typeface.NORMAL);
        body.setPadding(0, dp(5), 0, 0);
        card.addView(body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(7), 0, 0);
        databaseContent.addView(card, params);
    }

    private void updateOnlineCatalog(Button button) {
        button.setEnabled(false);
        button.setText("STAHUJI REGISTRY IEEE…");
        executor.submit(() -> {
            OnlineCatalogUpdater.Result result = OnlineCatalogUpdater.update(getApplicationContext());
            if (result.success) {
                catalog = DeviceCatalog.load(getApplicationContext());
                applyUpdatedCatalog();
            }
            handler.post(() -> refreshDatabasePanel(result.message));
        });
    }

    private void applyUpdatedCatalog() {
        DeviceCatalog current = catalog;
        if (current == null) return;
        synchronized (devices) {
            for (DeviceInfo device : devices.values()) {
                if (TextUtils.isEmpty(device.address) || !("WIFI".equals(device.transport) || "BLUETOOTH".equals(device.transport))) continue;
                String vendor = current.vendorFromMac(device.address);
                if (!vendor.isEmpty()) {
                    device.vendor = vendor;
                    if (device.model.isEmpty() || device.model.contains("přesný model") || device.model.contains("Wi‑Fi modul"))
                        device.model = current.modelHintForVendor(vendor, "Komunikační modul výrobce " + vendor);
                }
            }
        }
        handler.post(this::renderDevices);
    }

    private void loadWatchSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        watchAlertsEnabled = prefs.getBoolean(PREF_ALERTS, true);
        backgroundScanEnabled = prefs.getBoolean(PREF_BACKGROUND_SCAN, true);
        watchedKeys.clear();
        String raw = prefs.getString(PREF_WATCHED, "");
        if (raw == null || raw.trim().isEmpty()) return;
        for (String key : raw.split("\\n")) {
            String clean = key.trim();
            if (!clean.isEmpty()) watchedKeys.add(clean);
        }
    }

    private void saveWatchSettings() {
        StringBuilder raw = new StringBuilder();
        for (String key : watchedKeys) raw.append(key).append('\n');
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(PREF_WATCHED, raw.toString())
                .putBoolean(PREF_ALERTS, watchAlertsEnabled)
                .putBoolean(PREF_BACKGROUND_SCAN, backgroundScanEnabled)
                .apply();
    }

    private void toggleWatched(DeviceInfo device) {
        if (device == null) return;
        if (watchedKeys.contains(device.key)) watchedKeys.remove(device.key);
        else watchedKeys.add(device.key);
        saveWatchSettings();
        renderDevices();
        refreshSettingsPanel();
        if (!scanning) startBluetoothPresenceWatch();
        if (!appVisible) updateBackgroundServiceState();
        Toast.makeText(this, watchedKeys.contains(device.key)
                ? "Zařízení je ve sledovaných" : "Zařízení odebráno ze sledovaných", Toast.LENGTH_SHORT).show();
    }

    private void notifyWatchedDevice(DeviceInfo device) {
        long now = System.currentTimeMillis();
        Long last = watchAlertTimes.get(device.key);
        if (last != null && now - last < 45000) return;
        watchAlertTimes.put(device.key, now);
        String body = device.name + " • " + (device.distance >= 0 ? formatDistance(device.distance) : device.source);
        setStatus("Sledované zařízení v blízkosti: " + body);
        handler.post(() -> Toast.makeText(this, "Sledované zařízení v blízkosti: " + device.name, Toast.LENGTH_LONG).show());
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        try {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                    ? new Notification.Builder(this, NOTIFY_CHANNEL)
                    : new Notification.Builder(this);
            Notification notification = builder
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("KUBA Scanner: sledované zařízení")
                    .setContentText(body)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.notify(Math.abs(device.key.hashCode()), notification);
        } catch (Exception ignored) { }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(NOTIFY_CHANNEL, "KUBA Nearby sledování", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Upozornění na označená Bluetooth a Wi‑Fi zařízení v okolí");
        manager.createNotificationChannel(channel);
    }

    private void updateBackgroundServiceState() {
        if (backgroundScanEnabled && !appVisible && !scanning && hasBluetoothScanPermission()) {
            try {
                Intent service = new Intent(this, BackgroundBleScanService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
                else startService(service);
            } catch (Exception ignored) { }
        } else {
            try { stopService(new Intent(this, BackgroundBleScanService.class)); }
            catch (Exception ignored) { }
        }
    }

    private boolean hasBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT < 31)
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }

    private String backgroundScanStatus() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        long lastScan = prefs.getLong(PREF_BG_LAST_SCAN, 0L);
        long lastResult = prefs.getLong(PREF_BG_LAST_RESULT, 0L);
        long count = prefs.getLong(PREF_BG_RESULT_COUNT, 0L);
        String error = prefs.getString(PREF_BG_LAST_ERROR, "");
        StringBuilder out = new StringBuilder(backgroundScanEnabled ? "Zapnuto" : "Vypnuto");
        if (lastScan > 0) out.append(" • poslední cyklus ").append(ageText(lastScan));
        if (lastResult > 0) out.append("\nPoslední nález ").append(ageText(lastResult)).append(" • celkem paketů ").append(count);
        if (!TextUtils.isEmpty(error)) out.append("\nDiagnostika: ").append(error);
        return out.toString();
    }

    private void refreshSecurity() {
        if (securityContent == null) return;
        securityContent.removeAllViews();
        TextView loading = label("Kontroluji telefon, síť a nainstalované aplikace…", 14, MUTED, Typeface.BOLD);
        loading.setPadding(dp(4), dp(25), dp(4), dp(25));
        securityContent.addView(loading);
        executor.submit(() -> {
            DeviceSecurityScanner.Result result = securityScanner.scan(getApplicationContext());
            handler.post(() -> showSecurityResult(result));
        });
    }

    private void showSecurityResult(DeviceSecurityScanner.Result result) {
        if (isFinishing() || securityContent == null) return;
        securityContent.removeAllViews();
        TextView title = label("BEZPEČNOST MÉHO ZAŘÍZENÍ", 18, TEXT, Typeface.BOLD);
        securityContent.addView(title);
        TextView summary = label(result.riskCount == 0 ? "Bez zjevných varování" : result.riskCount + " položek ke kontrole", 13,
                result.riskCount == 0 ? ACCENT : TRACKER, Typeface.BOLD);
        summary.setPadding(0, dp(5), 0, dp(8));
        securityContent.addView(summary);
        addSecurityCard("TELEFON", result.device, ACCENT);
        addSecurityCard("AKTIVNÍ SÍŤ A OCHRANA", result.network + "\n" + result.protection, WIFI);
        addSecurityCard("ODESLANÁ A PŘIJATÁ DATA", result.traffic, BLUETOOTH);
        addSecurityCard("APLIKACE", result.userApps + " uživatelských • " + result.systemApps + " systémových", AUDIO);
        for (String finding : result.findings) addSecurityCard("KONTROLA", finding, TRACKER);
        addSecurityCard("CO ANDROID NEDOVOLÍ", result.limitation, MUTED);

        Button refresh = new Button(this);
        refresh.setText("OBNOVIT KONTROLU A PROVOZ");
        refresh.setTextColor(BG);
        refresh.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        refresh.setBackground(round(ACCENT, 14, 0, 0));
        refresh.setOnClickListener(v -> refreshSecurity());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(50));
        params.setMargins(0, dp(11), 0, dp(8));
        securityContent.addView(refresh, params);
    }

    private void addSecurityCard(String caption, String value, int color) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(round(SURFACE, 14, color, 1));
        card.addView(label(caption, 9, color, Typeface.BOLD));
        TextView content = label(value, 12, TEXT, Typeface.NORMAL);
        content.setPadding(0, dp(5), 0, 0);
        content.setTextIsSelectable(true);
        card.addView(content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(7), 0, 0);
        securityContent.addView(card, params);
    }

    private void selectSection(String id) {
        if ("SORT_DISTANCE".equals(id)) {
            sortByDistance = true;
            activeFilter = "ALL";
            updateSortChip();
            updateFilterStyles();
            renderDevices();
            return;
        }
        if ("SORT_CATEGORY".equals(id)) {
            sortByDistance = false;
            activeFilter = "ALL";
            updateSortChip();
            updateFilterStyles();
            renderDevices();
            return;
        }
        activeFilter = id;
        boolean radar = "RADAR".equals(id);
        boolean security = "SECURITY".equals(id);
        boolean database = "DATABASE".equals(id);
        boolean settings = "SETTINGS".equals(id);
        boolean gnss = "GNSS".equals(id);
        deviceScroll.setVisibility(!radar && !security && !database && !settings && !gnss ? View.VISIBLE : View.GONE);
        radarSection.setVisibility(radar ? View.VISIBLE : View.GONE);
        securityScroll.setVisibility(security ? View.VISIBLE : View.GONE);
        databaseScroll.setVisibility(database ? View.VISIBLE : View.GONE);
        settingsScroll.setVisibility(settings ? View.VISIBLE : View.GONE);
        gnssScroll.setVisibility(gnss ? View.VISIBLE : View.GONE);
        if (gnss) startGnssMonitor();
        else stopGnssMonitor();
        updateFilterStyles();
        renderDevices();
        if (security) refreshSecurity();
        if (database) refreshDatabasePanel("");
        if (settings) refreshSettingsPanel();
    }

    private void addFilter(LinearLayout parent, String id, String text) {
        TextView chip = label(text, 13, id.equals("ALL") ? BG : MUTED, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(15), dp(8), dp(15), dp(8));
        chip.setBackground(round(id.equals("ALL") ? ACCENT : SURFACE, 18, 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(38));
        params.setMargins(0, 0, dp(8), 0);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> selectSection(id));
        filterViews.put(id, chip);
    }

    private void addSortToggle(LinearLayout parent) {
        sortChip = label(sortByDistance ? "Řadit: metry" : "Řadit: kategorie", 13,
                sortByDistance ? BG : MUTED, Typeface.BOLD);
        sortChip.setGravity(Gravity.CENTER);
        sortChip.setPadding(dp(15), dp(8), dp(15), dp(8));
        sortChip.setBackground(round(sortByDistance ? ACCENT : SURFACE_2, 18, 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(38));
        params.setMargins(dp(4), 0, dp(8), 0);
        parent.addView(sortChip, params);
        sortChip.setOnClickListener(v -> {
            sortByDistance = !sortByDistance;
            updateSortChip();
            renderDevices();
        });
    }

    private void addBrandMenu(LinearLayout parent) {
        TextView chip = label("Značky ▾", 13, TEXT, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(15), dp(8), dp(15), dp(8));
        chip.setBackground(round(SURFACE_2, 18, 0, 0));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(38));
        params.setMargins(dp(4), 0, dp(8), 0);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> showBrandMenu());
    }

    private void showBrandMenu() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(16), dp(18), dp(16));
        panel.setBackground(round(SURFACE, 18, ACCENT, 1));
        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));
        panel.addView(label("ZNAČKY A TYPY ZAŘÍZENÍ", 18, TEXT, Typeface.BOLD));
        TextView note = label("Vyber značku nebo skupinu. Seznam můžeš zároveň řadit podle metrů tlačítkem Řadit.", 12, MUTED, Typeface.NORMAL);
        note.setPadding(0, dp(6), 0, dp(8));
        panel.addView(note);
        addBrandMenuItem(panel, dialog, "Vše", "ALL");
        addBrandMenuItem(panel, dialog, "Nejbližší nahoře", "SORT_DISTANCE");
        addBrandMenuItem(panel, dialog, "Trackery / tagy", "TRACKER");
        addBrandMenuItem(panel, dialog, "Apple Watch / AirTag / AirPods", "APPLE");
        addBrandMenuItem(panel, dialog, "Samsung Galaxy telefony / Watch / Buds / SmartTag", "SAMSUNG");
        addBrandMenuItem(panel, dialog, "Xiaomi / Mi Band / Redmi", "MI_BAND");
        addBrandMenuItem(panel, dialog, "Garmin", "GARMIN");
        addBrandMenuItem(panel, dialog, "Fitbit", "FITBIT");
        addBrandMenuItem(panel, dialog, "Amazfit / Zepp / Huami", "AMAZFIT");
        addBrandMenuItem(panel, dialog, "Huawei / Honor", "HUAWEI");
        addBrandMenuItem(panel, dialog, "Asus / ROG", "ASUS");
        addBrandMenuItem(panel, dialog, "Google Pixel / Nest", "GOOGLE");
        addBrandMenuItem(panel, dialog, "Sony Xperia / audio", "SONY");
        addBrandMenuItem(panel, dialog, "Motorola / Lenovo", "MOTOROLA");
        addBrandMenuItem(panel, dialog, "Oppo / OnePlus / Realme / Vivo", "BBK");
        addBrandMenuItem(panel, dialog, "Nokia / HMD", "NOKIA");
        addBrandMenuItem(panel, dialog, "Nothing / CMF", "NOTHING");
        addBrandMenuItem(panel, dialog, "JBL / Bose / Sennheiser / Jabra", "AUDIO_BRANDS");
        addBrandMenuItem(panel, dialog, "Další hodinky a náramky", "OTHER_WEARABLE");
        addBrandMenuItem(panel, dialog, "Kamery / ONVIF / RTSP", "CAMERA");
        addBrandMenuItem(panel, dialog, "Spy / skryté / sledování", "SPY");
        addBrandMenuItem(panel, dialog, "Sluchátka a audio", "AUDIO");
        addBrandMenuItem(panel, dialog, "Koloběžky / e-bike / vozidla", "SCOOTER");
        addBrandMenuItem(panel, dialog, "Chytrá domácnost", "SMART_HOME");
        addBrandMenuItem(panel, dialog, "IoT moduly", "IOT");
        addBrandMenuItem(panel, dialog, "Počítače / NAS / Raspberry Pi", "COMPUTER");
        addBrandMenuItem(panel, dialog, "Telefony / tablety", "PHONE");
        addBrandMenuItem(panel, dialog, "Wi‑Fi", "WIFI");
        addBrandMenuItem(panel, dialog, "Bluetooth", "BLUETOOTH");
        Button close = new Button(this);
        close.setText("ZAVŘÍT");
        close.setTextColor(TEXT);
        close.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        close.setBackground(round(SURFACE_2, 14, 0, 0));
        close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(46));
        closeParams.setMargins(0, dp(10), 0, 0);
        panel.addView(close, closeParams);
        dialog.setContentView(scroll);
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(getResources().getDisplayMetrics().widthPixels - dp(28),
                    WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void addBrandMenuItem(LinearLayout panel, Dialog dialog, String title, String filter) {
        TextView row = label(title + "  •  " + countForFilter(filter), 14, TEXT, Typeface.BOLD);
        row.setPadding(dp(14), dp(11), dp(14), dp(11));
        row.setBackground(round(Color.rgb(12, 25, 35), 13, colorForType(filter), 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(6), 0, 0);
        panel.addView(row, params);
        row.setOnClickListener(v -> {
            selectSection(filter);
            dialog.dismiss();
        });
    }

    private int countForFilter(String filter) {
        if (filter.startsWith("SORT_")) {
            synchronized (devices) {
                int visible = 0;
                for (DeviceInfo device : devices.values()) if (shouldDisplayDevice(device)) visible++;
                return visible;
            }
        }
        String previous = activeFilter;
        int count = 0;
        synchronized (devices) {
            activeFilter = filter;
            for (DeviceInfo device : devices.values()) {
                if (shouldDisplayDevice(device) && matchesFilter(device)) count++;
            }
            activeFilter = previous;
        }
        return count;
    }

    private void updateSortChip() {
        if (sortChip == null) return;
        sortChip.setText(sortByDistance ? "Řadit: metry" : "Řadit: kategorie");
        sortChip.setTextColor(sortByDistance ? BG : MUTED);
        sortChip.setBackground(round(sortByDistance ? ACCENT : SURFACE_2, 18, 0, 0));
    }

    private LinearLayout buildDrawer() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(28), dp(18), dp(12));
        panel.setBackgroundColor(Color.rgb(9, 20, 29));

        TextView close = label("KUBA SCANNER                         ×", 17, TEXT, Typeface.BOLD);
        close.setGravity(Gravity.CENTER_VERTICAL);
        close.setPadding(0, 0, 0, dp(6));
        close.setOnClickListener(v -> closeDrawer());
        panel.addView(close, new LinearLayout.LayoutParams(-1, dp(48)));
        panel.addView(label("SEKCE", 10, MUTED, Typeface.BOLD));

        ScrollView itemScroll = new ScrollView(this);
        itemScroll.setVerticalScrollBarEnabled(false);
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(LinearLayout.VERTICAL);
        items.setPadding(0, dp(5), 0, dp(10));
        addDrawerItem(items, "ALL", "◉", "Přehled", "Všechna nalezená zařízení");
        addDrawerItem(items, "RADAR", "⌁", "3D mapa skeneru", "Radar Wi‑Fi, Bluetooth a skrytých názvů");
        addDrawerItem(items, "GNSS", "+", "GPS / GNSS satelity", "GPS, Galileo, GLONASS, BeiDou a stav fixu");
        addDrawerItem(items, "SECURITY", "◇", "Bezpečnost telefonu", "Data, síť, aplikace a oprávnění");
        addDrawerItem(items, "DATABASE", "▤", "Databáze zařízení", "Offline katalog + online aktualizace IEEE");
        addDrawerItem(items, "HIDDEN", "?", "Skryté názvy", "Anonymní Bluetooth a Wi‑Fi dohromady");
        addDrawerItem(items, "NAMED", "A", "Pojmenovaná zařízení", "Zařízení, která svůj název vysílají");
        addDrawerItem(items, "TRACKER", "⌖", "Sledovací zařízení", "AirTag, SmartTag, Tile a další");
        addDrawerItem(items, "WEARABLE", "◌", "Hodinky a náramky", "Galaxy Watch, Apple Watch, Fitbit, Garmin, Mi Band");
        addDrawerBrandGroup(items);
        addDrawerItem(items, "SPY", "!", "Spy / sledování", "Trackery, skryté názvy, kamery a podezřelé profily");
        addDrawerItem(items, "WATCHED", "★", "Sledované BT/Wi‑Fi", "Upozornění, když se označené zařízení objeví");
        addDrawerItem(items, "SORT_DISTANCE", "m", "Seřadit podle metrů", "Nejbližší zařízení nahoře");
        addDrawerItem(items, "SORT_CATEGORY", "≡", "Seřadit podle sekcí", "Trackery, hodinky, kamery, audio, Wi‑Fi");
        addDrawerItem(items, "BLUETOOTH", "ᛒ", "Bluetooth", "Classic a Low Energy");
        addDrawerItem(items, "WIFI", "≋", "Wi‑Fi", "Přístupové body a výrobci modulů");
        addDrawerItem(items, "CAMERA", "●", "Kamery", "ONVIF a známé kamerové profily");
        addDrawerItem(items, "AUDIO", "♪", "Zvuk", "Sluchátka, reproduktory a TV");
        addDrawerItem(items, "SCOOTER", "↯", "Koloběžky / vozidla", "Xiaomi, Ninebot, Segway, e-bike a BLE moduly");
        addDrawerItem(items, "SMART_HOME", "⌂", "Chytrá domácnost", "Tuya, Matter, HomeKit, zásuvky, světla, senzory");
        addDrawerItem(items, "COMPUTER", "▣", "Počítače", "Notebooky, PC, Raspberry Pi, NAS");
        addDrawerItem(items, "PHONE", "▯", "Telefony", "Mobily a tablety podle BT/Wi‑Fi dat");
        addDrawerItem(items, "IOT", "▦", "IoT a malé moduly", "Raspberry Pi, ESP, Nordic, Arduino, Tuya");
        addDrawerItem(items, "NETWORK", "⇄", "Lokální síť", "SSDP a odpovídající zařízení");
        addDrawerItem(items, "SETTINGS", "⚙", "Nastavení", "Sledování, upozornění a demo zařízení");
        TextView database = label("OFFLINE DATABÁZE\n39 902 IEEE výrobců MAC\n3 997 Bluetooth SIG firem\n783 Bluetooth služeb\n287 typů zařízení\nslovník hodinek, tagů, audio, kamer, koloběžek, smart-home, PC a IoT", 11, MUTED, Typeface.NORMAL);
        database.setPadding(dp(13), dp(12), dp(13), dp(12));
        database.setBackground(round(SURFACE, 13, Color.rgb(39, 64, 79), 1));
        items.addView(database, new LinearLayout.LayoutParams(-1, -2));
        itemScroll.addView(items, new ScrollView.LayoutParams(-1, -2));
        panel.addView(itemScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return panel;
    }

    private void addDrawerItem(LinearLayout panel, String id, String icon, String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(9), dp(9), dp(9));
        row.setBackground(round(id.equals("ALL") ? SURFACE_2 : Color.TRANSPARENT, 13, 0, 0));
        TextView symbol = label(icon, 21, colorForType(id), Typeface.BOLD);
        symbol.setGravity(Gravity.CENTER);
        row.addView(symbol, new LinearLayout.LayoutParams(dp(39), dp(42)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(label(title, 14, TEXT, Typeface.BOLD));
        texts.addView(label(subtitle, 10, MUTED, Typeface.NORMAL));
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(61));
        params.setMargins(0, dp(3), 0, 0);
        panel.addView(row, params);
        row.setOnClickListener(v -> {
            selectSection(id);
            closeDrawer();
        });
        row.setTag(id);
    }

    private void addDrawerBrandGroup(LinearLayout panel) {
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(9), dp(9), dp(9));
        header.setBackground(round(Color.TRANSPARENT, 13, 0, 0));
        TextView symbol = label("▾", 21, WEARABLE, Typeface.BOLD);
        symbol.setGravity(Gravity.CENTER);
        drawerBrandToggle = symbol;
        header.addView(symbol, new LinearLayout.LayoutParams(dp(39), dp(42)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(label("Značky zařízení", 14, TEXT, Typeface.BOLD));
        texts.addView(label("Rozbal Apple, Samsung, Xiaomi, hodinky a audio značky", 10, MUTED, Typeface.NORMAL));
        header.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, dp(61));
        headerParams.setMargins(0, dp(3), 0, 0);
        panel.addView(header, headerParams);

        drawerBrandItems = new LinearLayout(this);
        drawerBrandItems.setOrientation(LinearLayout.VERTICAL);
        drawerBrandItems.setPadding(dp(13), 0, 0, dp(3));
        drawerBrandsExpanded = true;
        drawerBrandItems.setVisibility(View.VISIBLE);
        addDrawerItem(drawerBrandItems, "APPLE", "A", "Apple", "Watch, AirTag, AirPods, Find My, iPhone/iPad signály");
        addDrawerItem(drawerBrandItems, "SAMSUNG", "S", "Samsung", "Galaxy telefony/tablety, Watch, Buds, SmartTag");
        addDrawerItem(drawerBrandItems, "MI_BAND", "M", "Xiaomi / Mi Band", "Smart Band, Redmi Watch, Mi Watch, koloběžky a IoT");
        addDrawerItem(drawerBrandItems, "GARMIN", "G", "Garmin", "Hodinky, cyklo počítače a sportovní senzory");
        addDrawerItem(drawerBrandItems, "FITBIT", "F", "Fitbit", "Sense, Versa, Charge, Inspire");
        addDrawerItem(drawerBrandItems, "AMAZFIT", "Z", "Amazfit / Zepp", "GTR, GTS, Bip, T-Rex a Zepp");
        addDrawerItem(drawerBrandItems, "HUAWEI", "H", "Huawei / Honor", "Watch, Band, FreeBuds a telefony");
        addDrawerItem(drawerBrandItems, "ASUS", "R", "Asus / ROG", "Telefony, periférie, routery a audio");
        addDrawerItem(drawerBrandItems, "GOOGLE", "P", "Google / Pixel", "Pixel telefony, Watch, Buds, Nest a Fast Pair");
        addDrawerItem(drawerBrandItems, "SONY", "X", "Sony", "Xperia, Walkman, sluchátka, TV a kamery");
        addDrawerItem(drawerBrandItems, "MOTOROLA", "M", "Motorola / Lenovo", "Moto, Razr, Edge, ThinkPad a příslušenství");
        addDrawerItem(drawerBrandItems, "BBK", "B", "Oppo / OnePlus / Realme / Vivo", "Telefony, hodinky, náramky a audio");
        addDrawerItem(drawerBrandItems, "NOKIA", "N", "Nokia / HMD", "Telefony, tablety a příslušenství");
        addDrawerItem(drawerBrandItems, "NOTHING", "C", "Nothing / CMF", "Phone, Ear, Watch a Buds");
        addDrawerItem(drawerBrandItems, "AUDIO_BRANDS", "♪", "Audio značky", "JBL, Bose, Sony, Sennheiser, Jabra, Marshall");
        addDrawerItem(drawerBrandItems, "OTHER_WEARABLE", "W", "Další hodinky", "Polar, Suunto, Coros, Mobvoi, Oura a levné značky");
        panel.addView(drawerBrandItems, new LinearLayout.LayoutParams(-1, -2));

        header.setOnClickListener(v -> {
            drawerBrandsExpanded = !drawerBrandsExpanded;
            drawerBrandItems.setVisibility(drawerBrandsExpanded ? View.VISIBLE : View.GONE);
            drawerBrandToggle.setText(drawerBrandsExpanded ? "▾" : "▸");
        });
    }

    private void openDrawer() {
        if (drawer == null || drawer.getVisibility() == View.VISIBLE) return;
        drawerScrim.setAlpha(0f);
        drawerScrim.setVisibility(View.VISIBLE);
        drawer.setTranslationX(-dp(310));
        drawer.setVisibility(View.VISIBLE);
        drawerScrim.animate().alpha(1f).setDuration(180).start();
        drawer.animate().translationX(0).setDuration(220).start();
    }

    private void closeDrawer() {
        if (drawer == null || drawer.getVisibility() != View.VISIBLE) return;
        drawerScrim.animate().alpha(0f).setDuration(160).withEndAction(() -> drawerScrim.setVisibility(View.GONE)).start();
        drawer.animate().translationX(-dp(310)).setDuration(190).withEndAction(() -> drawer.setVisibility(View.GONE)).start();
    }

    private void updateFilterStyles() {
        for (Map.Entry<String, TextView> entry : filterViews.entrySet()) {
            boolean active = entry.getKey().equals(activeFilter);
            entry.getValue().setTextColor(active ? BG : MUTED);
            entry.getValue().setBackground(round(active ? ACCENT : SURFACE, 18, 0, 0));
        }
        if (drawer != null) updateDrawerSelection(drawer);
    }

    private void updateDrawerSelection(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.getTag() instanceof String) {
                boolean active = child.getTag().equals(activeFilter);
                child.setBackground(round(active ? SURFACE_2 : Color.TRANSPARENT, 13, 0, 0));
            }
            if (child instanceof ViewGroup) updateDrawerSelection((ViewGroup) child);
        }
    }

    private void ensurePermissionsAndScan() {
        if (scanning) {
            setStatus("Sken už běží • čekám na další Bluetooth/Wi‑Fi odpovědi");
            return;
        }
        List<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31) {
            addMissing(missing, Manifest.permission.BLUETOOTH_SCAN);
            addMissing(missing, Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (Build.VERSION.SDK_INT >= 33) addMissing(missing, Manifest.permission.NEARBY_WIFI_DEVICES);
        if (Build.VERSION.SDK_INT >= 33) addMissing(missing, Manifest.permission.POST_NOTIFICATIONS);
        addMissing(missing, Manifest.permission.ACCESS_FINE_LOCATION);
        addMissing(missing, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS);
        } else {
            startFullScan();
        }
    }

    private void addMissing(List<String> list, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) list.add(permission);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_GNSS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) startGnssMonitor();
            else renderGnssPanel();
            return;
        }
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean anyDenied = false;
            for (int result : grantResults) if (result != PackageManager.PERMISSION_GRANTED) anyDenied = true;
            if (anyDenied) Toast.makeText(this, "Bez oprávnění bude výsledek omezený.", Toast.LENGTH_LONG).show();
            startFullScan();
        }
    }

    private void startFullScan() {
        try { stopService(new Intent(this, BackgroundBleScanService.class)); } catch (Exception ignored) { }
        stopBluetoothPresenceWatch();
        stopRadios();
        startRangePruneLoop();
        resetScanStats();
        prepareDevicesForScan();
        scanning = true;
        activeFilter = "ALL";
        showDeviceListPane();
        updateFilterStyles();
        scanButton.setEnabled(false);
        scanButton.setText("Důkladně…");
        renderDevices();
        scanStartedAt = System.currentTimeMillis();
        addPreScanDiagnostics();
        setStatus("Důkladné skenování 0 % • BLE všech podporovaných PHY + Bluetooth Classic + Wi‑Fi + ONVIF + SSDP + mDNS");
        handler.post(scanProgressTick);
        startBluetoothScan();
        startWifiScan();
        executor.submit(this::runSsdpDiscovery);
        executor.submit(this::runOnvifDiscovery);
        if (mdnsScanner != null) mdnsScanner.start(SCAN_DURATION_MS - 2000);
        handler.postDelayed(this::checkEarlyScanSilence, 9000);
        handler.postDelayed(this::checkMidScanSilence, 22000);
        handler.postDelayed(this::finishScan, SCAN_DURATION_MS);
    }

    private void finishScan() {
        handler.removeCallbacks(scanProgressTick);
        stopRadios();
        scanning = false;
        scanButton.setEnabled(true);
        scanButton.setText("SKEN");
        pruneOutOfRangeDevices(false);
        if (realDeviceCount() == 0) {
            addScanWarning("Sken doběhl bez zařízení. Nejčastěji je vypnutá Poloha, Bluetooth/Wi‑Fi, chybí oprávnění nebo jsou zařízení mimo vysílání.");
        }
        startBluetoothPresenceWatch();
        if (!appVisible) updateBackgroundServiceState();
        setStatus("Hotovo • " + scanSummary() + " • celkem " + realDeviceCount() + " • hlídání běží");
        renderDevices();
    }

    private void updateScanProgress() {
        long elapsed = Math.max(0, System.currentTimeMillis() - scanStartedAt);
        int percent = Math.min(99, (int) ((elapsed * 100L) / SCAN_DURATION_MS));
        int size = realDeviceCount();
        setStatus("Důkladné skenování " + percent + " % • " + scanSummary() + " • celkem " + size);
    }

    private void showDeviceListPane() {
        if (deviceScroll != null) deviceScroll.setVisibility(View.VISIBLE);
        if (radarSection != null) radarSection.setVisibility(View.GONE);
        if (securityScroll != null) securityScroll.setVisibility(View.GONE);
        if (databaseScroll != null) databaseScroll.setVisibility(View.GONE);
        if (settingsScroll != null) settingsScroll.setVisibility(View.GONE);
        if (gnssScroll != null) gnssScroll.setVisibility(View.GONE);
        stopGnssMonitor();
    }

    private int realDeviceCount() {
        int count = 0;
        synchronized (devices) {
            for (DeviceInfo device : devices.values()) {
                if (!"INFO".equals(device.type) && shouldDisplayDevice(device)) count++;
            }
        }
        return count;
    }

    private void resetScanStats() {
        scanBtKeys.clear();
        scanWifiKeys.clear();
        scanNetworkKeys.clear();
        scanHiddenKeys.clear();
        scanWarnings.clear();
    }

    private String scanSummary() {
        return "BT " + scanBtKeys.size() + " • Wi‑Fi " + scanWifiKeys.size()
                + " • síť " + scanNetworkKeys.size() + " • skryté " + scanHiddenKeys.size();
    }

    private void addPreScanDiagnostics() {
        if (bluetoothAdapter == null) addScanWarning("Bluetooth rádio není v telefonu dostupné");
        else {
            try {
                if (!bluetoothAdapter.isEnabled()) addScanWarning("Bluetooth je vypnuté – zapni ho v rychlém panelu nebo nastavení");
            } catch (SecurityException ignored) {
                addScanWarning("Bluetooth nelze zkontrolovat – chybí oprávnění Okolní zařízení");
            }
        }
        if (wifiManager == null) addScanWarning("Wi‑Fi rádio není dostupné");
        else {
            try {
                if (!wifiManager.isWifiEnabled()) addScanWarning("Wi‑Fi je vypnutá – Android nemusí vrátit okolní sítě");
            } catch (Exception ignored) { }
        }
        if (!isLocationEnabled()) addScanWarning("Poloha telefonu je vypnutá – Android často blokuje Bluetooth/Wi‑Fi výsledky");
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            addScanWarning("Chybí oprávnění Bluetooth scan");
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            addScanWarning("Chybí oprávnění Bluetooth connect");
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            addScanWarning("Chybí přesná poloha – bez ní Android často vrací prázdné scan výsledky");
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED)
            addScanWarning("Chybí oprávnění Blízká Wi‑Fi zařízení");
    }

    private void addScanWarning(String message) {
        if (scanWarnings.add(message)) addSystemInfo("Diagnostika skenu: " + message);
    }

    private boolean isLocationEnabled() {
        if (locationManager == null) return true;
        try {
            if (Build.VERSION.SDK_INT >= 28) return locationManager.isLocationEnabled();
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
            return true;
        }
    }

    private void checkEarlyScanSilence() {
        if (!scanning) return;
        if (scanBtKeys.isEmpty() && scanWifiKeys.isEmpty() && scanNetworkKeys.isEmpty()) {
            addScanWarning("Zatím nic neodpovědělo. Zkontroluj Bluetooth, Wi‑Fi, Polohu a oprávnění aplikace.");
            collectBondedBluetoothDevices();
        }
    }

    private void checkMidScanSilence() {
        if (!scanning) return;
        if (scanBtKeys.isEmpty()) addScanWarning("Bluetooth stále nic neukazuje – některá zařízení vysílají jen při párování nebo po probuzení displeje.");
        if (scanWifiKeys.isEmpty()) addScanWarning("Wi‑Fi seznam je prázdný – Android může vyžadovat zapnutou polohu a aktivní Wi‑Fi.");
    }

    private void startBluetoothScan() {
        if (bluetoothAdapter == null) {
            addSystemInfo("Bluetooth není v telefonu dostupné");
            return;
        }
        try {
            if (!bluetoothAdapter.isEnabled()) {
                addSystemInfo("Bluetooth je vypnuté – zapni ho a spusť sken znovu");
                return;
            }
            collectBondedBluetoothDevices();
            boolean classicStarted = bluetoothAdapter.startDiscovery();
            if (!classicStarted) addScanWarning("Bluetooth Classic discovery se nespustil – zkouším BLE a uložená spárovaná zařízení");
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner != null) {
                startPrimaryBleScan(false);
            } else {
                addScanWarning("Bluetooth LE scanner není dostupný – zůstává Classic discovery a spárovaná zařízení");
            }
        } catch (SecurityException ex) {
            addSystemInfo("Bluetooth: chybí oprávnění pro okolní zařízení");
        }
    }

    private void startPrimaryBleScan(boolean compatible) {
        if (bluetoothLeScanner == null || !scanning) return;
        final ScanCallback callback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult result) {
                if (result == null || result.getDevice() == null) return;
                addBluetoothDevice(result.getDevice(), result.getDevice().getBluetoothClass(),
                        result.getScanRecord(), result.getRssi(), compatible
                                ? "Bluetooth LE • kompatibilní režim" : "Bluetooth LE • důkladný režim");
            }
            @Override public void onBatchScanResults(List<ScanResult> results) {
                if (results == null) return;
                for (ScanResult result : results) onScanResult(0, result);
            }
            @Override public void onScanFailed(int errorCode) {
                if (bleCallback != this) return;
                addScanWarning("BLE sken selhal – " + bleErrorLabel(errorCode));
                try { bluetoothLeScanner.stopScan(this); } catch (Exception ignored) { }
                bleCallback = null;
                if (!compatible && scanning) {
                    addScanWarning("Přepínám automaticky na kompatibilní BLE sken bez rozšířeného PHY");
                    handler.postDelayed(() -> startPrimaryBleScan(true), 650);
                }
            }
        };
        bleCallback = callback;
        try {
            bluetoothLeScanner.startScan(null, compatible ? compatibleBleSettings() : detailedBleSettings(), callback);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bleCallback = null;
            if (!compatible) {
                addScanWarning("Agresivní BLE nastavení telefon nepodporuje – přepínám na kompatibilní BLE sken");
                handler.postDelayed(() -> startPrimaryBleScan(true), 250);
            } else addScanWarning("Kompatibilní BLE sken se nepodařilo spustit");
        }
    }

    private void collectBondedBluetoothDevices() {
        if (bluetoothAdapter == null) return;
        try {
            Set<BluetoothDevice> bonded = bluetoothAdapter.getBondedDevices();
            if (bonded == null || bonded.isEmpty()) return;
            for (BluetoothDevice device : bonded) {
                if (device == null) continue;
                addBluetoothDevice(device, device.getBluetoothClass(), null, 0, "Bluetooth spárované / známé zařízení");
            }
        } catch (SecurityException ignored) {
            addScanWarning("Spárovaná Bluetooth zařízení nejdou přečíst – chybí oprávnění Bluetooth connect");
        }
    }

    private void startBluetoothPresenceWatch() {
        stopBluetoothPresenceWatch();
        startWatchedWifiWatch();
        if (bluetoothAdapter == null) return;
        bluetoothPresenceTick = new Runnable() {
            @Override public void run() {
                pruneOutOfRangeDevices(false);
                if (!scanning && TextUtils.isEmpty(trackingKey)) startPresenceBleWindow();
                handler.postDelayed(this, 15000);
            }
        };
        handler.post(bluetoothPresenceTick);
    }

    private void startWatchedWifiWatch() {
        if (watchedWifiTick != null) handler.removeCallbacks(watchedWifiTick);
        watchedWifiTick = new Runnable() {
            @Override public void run() {
                if (!scanning) {
                    try {
                        if (wifiManager != null) {
                            wifiManager.startScan();
                            collectWifiResults();
                        }
                    } catch (SecurityException ignored) { }
                }
                handler.postDelayed(this, 30000);
            }
        };
        handler.postDelayed(watchedWifiTick, 2500);
    }

    private void startPresenceBleWindow() {
        try {
            if (!bluetoothAdapter.isEnabled()) return;
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner == null) return;
            startPresenceBleScan(false);
        } catch (SecurityException ignored) { }
    }

    private void startPresenceBleScan(boolean compatible) {
        if (bluetoothLeScanner == null || scanning) return;
        final ScanCallback callback = new ScanCallback() {
            @Override public void onScanResult(int callbackType, ScanResult result) {
                if (result == null || result.getDevice() == null) return;
                addBluetoothDevice(result.getDevice(), result.getDevice().getBluetoothClass(),
                        result.getScanRecord(), result.getRssi(), "Bluetooth LE • hlídání dosahu");
            }
            @Override public void onBatchScanResults(List<ScanResult> results) {
                if (results == null) return;
                for (ScanResult result : results) onScanResult(0, result);
            }
            @Override public void onScanFailed(int errorCode) {
                if (presenceBleCallback != this) return;
                addScanWarning("BLE hlídání dosahu selhalo – " + bleErrorLabel(errorCode));
                try { bluetoothLeScanner.stopScan(this); } catch (Exception ignored) { }
                presenceBleCallback = null;
                if (!compatible && !scanning) handler.postDelayed(() -> startPresenceBleScan(true), 650);
            }
        };
        presenceBleCallback = callback;
        try {
            bluetoothLeScanner.startScan(null, compatible ? compatibleBleSettings() : presenceBleSettings(), callback);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            presenceBleCallback = null;
            if (!compatible) handler.postDelayed(() -> startPresenceBleScan(true), 250);
            else addScanWarning("BLE hlídání dosahu se nepodařilo spustit");
            return;
        }
        handler.postDelayed(() -> {
            try {
                if (bluetoothLeScanner != null) bluetoothLeScanner.stopScan(callback);
            } catch (SecurityException ignored) { }
            if (presenceBleCallback == callback) presenceBleCallback = null;
            pruneOutOfRangeDevices(false);
        }, 7000);
    }

    private void stopBluetoothPresenceWatch() {
        if (bluetoothPresenceTick != null) handler.removeCallbacks(bluetoothPresenceTick);
        bluetoothPresenceTick = null;
        if (watchedWifiTick != null) handler.removeCallbacks(watchedWifiTick);
        watchedWifiTick = null;
        try {
            if (bluetoothLeScanner != null && presenceBleCallback != null) bluetoothLeScanner.stopScan(presenceBleCallback);
        } catch (SecurityException ignored) { }
        presenceBleCallback = null;
    }

    private void stopCurrentPresenceWindow() {
        try {
            if (bluetoothLeScanner != null && presenceBleCallback != null)
                bluetoothLeScanner.stopScan(presenceBleCallback);
        } catch (SecurityException ignored) { }
        presenceBleCallback = null;
    }

    private ScanSettings presenceBleSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .setReportDelay(0)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                .build();
    }

    private ScanSettings detailedBleSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setLegacy(false)
                .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                .build();
    }

    private ScanSettings compatibleBleSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
    }

    private String bleErrorLabel(int code) {
        switch (code) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED: return "už běží jiný BLE scan";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED: return "Android nezaregistroval scan aplikace";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR: return "interní chyba Bluetooth stacku";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED: return "telefon nepodporuje požadovanou BLE funkci";
            case ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES: return "došly hardwarové prostředky Bluetooth";
            case ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY: return "Android omezuje příliš časté skenování";
            default: return "kód " + code;
        }
    }

    private void addBluetoothDevice(BluetoothDevice device, BluetoothClass clazz, ScanRecord record, int rssi, String source) {
        addBluetoothDevice(device, clazz, record, rssi, source, "", null);
    }

    private void addBluetoothDevice(BluetoothDevice device, BluetoothClass clazz, ScanRecord record, int rssi,
                                    String source, String forcedName, byte[] rawAdvertisement) {
        try {
            if (device == null) return;
            String name = TextUtils.isEmpty(forcedName) ? device.getName() : forcedName;
            if (record != null && !TextUtils.isEmpty(record.getDeviceName())) name = record.getDeviceName();
            boolean hiddenName = TextUtils.isEmpty(name);
            if (TextUtils.isEmpty(name)) name = "Neznámé Bluetooth zařízení";
            String address = device.getAddress();
            int major = clazz == null ? -1 : clazz.getMajorDeviceClass();
            DeviceCatalog.BluetoothIdentity identity = catalog == null
                    ? new DeviceCatalog.BluetoothIdentity()
                    : catalog.identifyBluetooth(device, clazz, record, name);
            Classification c = classify(name + " " + identity.searchableText(), source, major);
            if (!TextUtils.isEmpty(identity.categoryHint)
                    && (identity.categoryConfidence >= c.confidence || "BLUETOOTH".equals(c.type) || "NETWORK".equals(c.type))) {
                c = new Classification(identity.categoryHint, identity.categoryConfidence, identity.categoryReason);
            }
            if (identity.trackerConfidence > 0)
                c = new Classification("TRACKER", identity.trackerConfidence, identity.trackerReason);
            String displayName = name;
            if (name.startsWith("Neznámé") && !TextUtils.isEmpty(identity.modelHint)) displayName = identity.modelHint;
            int txPower = identity.txPower == Integer.MIN_VALUE ? -59 : identity.txPower;
            boolean pairedSnapshot = source.contains("spárované") && record == null && (rssi == 0 || rssi == Short.MIN_VALUE);
            DeviceInfo info = new DeviceInfo("BT:" + address, displayName, address, source, c.type,
                    rssi == Short.MIN_VALUE ? 0 : rssi,
                    rssi == Short.MIN_VALUE ? -1 : distanceFromRssi(rssi, txPower, 2.2),
                    c.confidence, c.reason, pairedSnapshot
                    ? "Známé spárované zařízení v Androidu, ale aktuální signál nebyl potvrzen. V seznamu se zobrazí po zachycení vysílání."
                    : "");
            info.vendor = identity.vendor;
            info.model = identity.modelHint;
            info.deviceClass = identity.deviceClass;
            info.services = identity.services;
            info.manufacturerIds = identity.manufacturerIds;
            info.appearance = identity.appearance;
            info.addressType = identity.addressType;
            info.rawAdvertisement = identity.rawAdvertisement;
            if (TextUtils.isEmpty(info.rawAdvertisement) && rawAdvertisement != null)
                info.rawAdvertisement = hexBytes(rawAdvertisement, 96);
            info.decodedName = identity.decodedName;
            info.decodedData = identity.decodedData;
            info.txPower = identity.txPower;
            info.transport = "BLUETOOTH";
            info.hiddenName = hiddenName;
            info.inRange = !pairedSnapshot;
            info.signalConfirmed = !pairedSnapshot;
            if (pairedSnapshot) {
                // Uložené/spárované zařízení bez živého vysílání se nesmí tvářit jako v dosahu.
                // Vynulujeme čas posledního zachycení, aby ho prořezávání nepřepnulo zpět na "v dosahu".
                info.lastSeenAt = 0L;
                info.firstSeenAt = 0L;
            }
            addOrUpdate(info);
        } catch (SecurityException ignored) { }
    }

    private String hexBytes(byte[] data, int maxBytes) {
        if (data == null || data.length == 0) return "";
        StringBuilder out = new StringBuilder();
        int end = Math.min(data.length, Math.max(0, maxBytes));
        for (int i = 0; i < end; i++) {
            if (out.length() > 0) out.append(' ');
            out.append(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
        }
        if (data.length > end) out.append(" …");
        return out.toString();
    }

    private void startWifiScan() {
        if (wifiManager == null) {
            addSystemInfo("Wi‑Fi není dostupná");
            return;
        }
        try {
            boolean started = wifiManager.startScan();
            collectWifiResults();
            if (!started) addSystemInfo("Android omezil nový Wi‑Fi sken – zobrazuji poslední dostupné výsledky");
        } catch (SecurityException ex) {
            addSystemInfo("Wi‑Fi: povol polohu a okolní zařízení");
        }
    }

    private void startTargetTracking(String key) {
        stopTargetTracking();
        trackingKey = key == null ? "" : key;
        DeviceInfo current = latestDevice(trackingKey);
        trackingLastRssi = current == null || current.rssi == 0 ? Integer.MIN_VALUE : current.rssi;
        trackingBestRssi = trackingLastRssi;
        trackingBestHeadingDeg = currentHeadingDeg;
        gpsBestBearingDeg = -1f;
        gpsBestRssi = Integer.MIN_VALUE;
        gpsLastRssi = trackingLastRssi;
        previousTrackingLocation = null;
        currentTrackingLocation = null;
        startHeadingUpdates();
        startLocationUpdates();
        if (trackingKey.startsWith("BT:") && !scanning) {
            stopCurrentPresenceWindow();
            startTrackingBluetoothScan();
        }
        else if (trackingKey.startsWith("WIFI:")) startTrackingWifiLoop();
    }

    private void startTrackingBluetoothScan() {
        if (bluetoothAdapter == null || TextUtils.isEmpty(trackingKey)) return;
        if (!scanning) setStatus("Live dohledávání vybraného Bluetooth zařízení • průběžně dekóduji reklamy, RSSI a metry");
        try {
            if (!bluetoothAdapter.isEnabled()) return;
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner != null) {
                trackingBleCallback = new ScanCallback() {
                    @Override public void onScanResult(int callbackType, ScanResult result) {
                        String address = result.getDevice() == null ? "" : result.getDevice().getAddress();
                        if (!("BT:" + address).equals(trackingKey)) return;
                        if (result.getDevice() == null) return;
                        addBluetoothDevice(result.getDevice(), result.getDevice().getBluetoothClass(),
                                result.getScanRecord(), result.getRssi(), "Bluetooth LE • live tracking");
                    }
                    @Override public void onBatchScanResults(List<ScanResult> results) {
                        for (ScanResult result : results) onScanResult(0, result);
                    }
                    @Override public void onScanFailed(int errorCode) {
                        addScanWarning("Live BLE tracking selhal – " + bleErrorLabel(errorCode));
                    }
                };
                try {
                    bluetoothLeScanner.startScan(null, detailedBleSettings(), trackingBleCallback);
                } catch (IllegalArgumentException ex) {
                    bluetoothLeScanner.startScan(null, compatibleBleSettings(), trackingBleCallback);
                }
            }
            if (!bluetoothAdapter.isDiscovering()) bluetoothAdapter.startDiscovery();
        } catch (SecurityException ignored) { }
    }

    private void startTrackingWifiLoop() {
        if (!scanning) setStatus("Live dohledávání vybrané Wi‑Fi položky • průběžně obnovuji sílu signálu a metry");
        trackingWifiTick = new Runnable() {
            @Override public void run() {
                if (TextUtils.isEmpty(trackingKey) || !trackingKey.startsWith("WIFI:")) return;
                try {
                    if (wifiManager != null) {
                        wifiManager.startScan();
                        collectWifiResults();
                    }
                } catch (SecurityException ignored) { }
                handler.postDelayed(this, 4000);
            }
        };
        handler.post(trackingWifiTick);
    }

    private void stopTargetTracking() {
        if (trackingWifiTick != null) handler.removeCallbacks(trackingWifiTick);
        trackingWifiTick = null;
        try {
            if (bluetoothLeScanner != null && trackingBleCallback != null) bluetoothLeScanner.stopScan(trackingBleCallback);
            if (!scanning && bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
        } catch (SecurityException ignored) { }
        trackingBleCallback = null;
        trackingKey = "";
        trackingLastRssi = Integer.MIN_VALUE;
        trackingBestRssi = Integer.MIN_VALUE;
        trackingBestHeadingDeg = -1f;
        gpsBestBearingDeg = -1f;
        gpsBestRssi = Integer.MIN_VALUE;
        gpsLastRssi = Integer.MIN_VALUE;
        previousTrackingLocation = null;
        currentTrackingLocation = null;
        stopHeadingUpdates();
        stopLocationUpdates();
    }

    private void suspendTargetTrackingRadios() {
        if (trackingWifiTick != null) handler.removeCallbacks(trackingWifiTick);
        trackingWifiTick = null;
        try {
            if (bluetoothLeScanner != null && trackingBleCallback != null)
                bluetoothLeScanner.stopScan(trackingBleCallback);
        } catch (SecurityException ignored) { }
        trackingBleCallback = null;
        stopHeadingUpdates();
        stopLocationUpdates();
    }

    private void resumeTargetTrackingRadios() {
        if (TextUtils.isEmpty(trackingKey) || scanning) return;
        startHeadingUpdates();
        startLocationUpdates();
        if (trackingKey.startsWith("BT:")) {
            stopCurrentPresenceWindow();
            startTrackingBluetoothScan();
        }
        else if (trackingKey.startsWith("WIFI:")) startTrackingWifiLoop();
    }

    private DeviceInfo latestDevice(String key) {
        synchronized (devices) { return devices.get(key); }
    }

    private void startHeadingUpdates() {
        if (sensorManager == null || headingRegistered) return;
        if (rotationSensor != null) {
            headingRegistered = sensorManager.registerListener(headingListener, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        } else {
            boolean accel = accelerometer != null && sensorManager.registerListener(headingListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            boolean magnet = magnetometer != null && sensorManager.registerListener(headingListener, magnetometer, SensorManager.SENSOR_DELAY_GAME);
            headingRegistered = accel || magnet;
        }
    }

    private void stopHeadingUpdates() {
        if (sensorManager != null && headingRegistered) sensorManager.unregisterListener(headingListener);
        headingRegistered = false;
        haveAccel = false;
        haveMagnet = false;
    }

    private void startLocationUpdates() {
        if (locationManager == null || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0.4f, trackingLocationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1200, 0.6f, trackingLocationListener);
        } catch (Exception ignored) { }
    }

    private void stopLocationUpdates() {
        if (locationManager == null) return;
        try { locationManager.removeUpdates(trackingLocationListener); } catch (Exception ignored) { }
    }

    private void updateFallbackHeading() {
        if (!haveAccel || !haveMagnet) return;
        float[] matrix = new float[9];
        float[] orientation = new float[3];
        if (SensorManager.getRotationMatrix(matrix, null, accelValues, magnetValues)) {
            SensorManager.getOrientation(matrix, orientation);
            updateSmoothedHeading(normalizeDegrees((float) Math.toDegrees(orientation[0])));
        }
    }

    private void updateSmoothedHeading(float rawHeading) {
        if (currentHeadingDeg < 0) {
            currentHeadingDeg = rawHeading;
            return;
        }
        currentHeadingDeg = normalizeDegrees(currentHeadingDeg + shortestAngle(currentHeadingDeg, rawHeading) * 0.18f);
    }

    private float normalizeDegrees(float value) {
        float out = value % 360f;
        return out < 0 ? out + 360f : out;
    }

    private float shortestAngle(float from, float to) {
        return ((to - from + 540f) % 360f) - 180f;
    }

    @SuppressWarnings("deprecation")
    private void collectWifiResults() {
        try {
            List<android.net.wifi.ScanResult> results = wifiManager.getScanResults();
            if (results == null || results.isEmpty()) {
                addScanWarning("Wi‑Fi nevrátila žádné sítě – zapni Wi‑Fi i Polohu a zkontroluj oprávnění aplikace");
                return;
            }
            boolean anyFresh = false;
            for (android.net.wifi.ScanResult result : results) {
                long resultAgeMs = result.timestamp > 0
                        ? Math.max(0L, SystemClock.elapsedRealtime() - result.timestamp / 1000L) : 0L;
                boolean freshResult = result.timestamp <= 0 || resultAgeMs <= WIFI_RESULT_MAX_AGE_MS;
                anyFresh |= freshResult;
                String ssid = Build.VERSION.SDK_INT >= 33 && result.getWifiSsid() != null
                        ? result.getWifiSsid().toString() : result.SSID;
                boolean hiddenName = ssid == null || ssid.isEmpty() || "<unknown ssid>".equals(ssid);
                if (ssid == null || ssid.isEmpty() || "<unknown ssid>".equals(ssid)) ssid = "Skrytá Wi‑Fi síť";
                Classification c = classify(ssid + " " + result.capabilities, "Wi‑Fi AP", -1);
                if (!isSpecificWifiType(c.type)) c = new Classification("WIFI", 100, "Wi‑Fi přístupový bod");
                double distance = wifiDistance(result.level, result.frequency);
                DeviceInfo info = new DeviceInfo("WIFI:" + result.BSSID, ssid, result.BSSID,
                        "Wi‑Fi • " + result.frequency + " MHz", c.type, result.level, distance,
                        c.confidence, c.reason, securityLabel(result.capabilities));
                if (catalog != null) {
                    info.vendor = catalog.vendorFromMac(result.BSSID);
                    info.addressType = catalog.macAddressNote(result.BSSID);
                    Classification vendorClass = classify(ssid + " " + info.vendor, "Wi‑Fi AP", -1);
                    if (isSpecificWifiType(vendorClass.type)) {
                        info.type = vendorClass.type;
                        info.confidence = vendorClass.confidence;
                        info.reason = vendorClass.reason + " (výrobce MAC modulu)";
                    }
                }
                info.model = info.vendor.isEmpty() ? "Přístupový bod – přesný model není v SSID"
                        : catalog.modelHintForVendor(info.vendor, "Wi‑Fi modul / AP výrobce " + info.vendor);
                info.frequency = result.frequency + " MHz • kanál " + wifiChannel(result.frequency);
                info.security = securityLabel(result.capabilities);
                info.decodedData = wifiDecodedData(result);
                info.transport = "WIFI";
                info.hiddenName = hiddenName;
                if (result.timestamp > 0) {
                    info.lastSeenAt = System.currentTimeMillis() - resultAgeMs;
                    info.firstSeenAt = info.lastSeenAt;
                }
                info.inRange = freshResult;
                addOrUpdate(info);
            }
            if (!anyFresh) addScanWarning("Android vrátil jen staré Wi‑Fi výsledky; čekám na nový skutečný sken místo zobrazení nedostupných sítí");
        } catch (SecurityException ignored) { }
    }

    private String securityLabel(String capabilities) {
        if (capabilities == null) return "";
        if (capabilities.contains("WPA3") || capabilities.contains("SAE")) return "WPA3";
        if (capabilities.contains("WPA2") || capabilities.contains("RSN")) return "WPA2";
        if (capabilities.contains("WPA")) return "WPA";
        return capabilities.contains("WEP") ? "WEP" : "otevřená / neurčeno";
    }

    private String wifiChannel(int frequency) {
        if (frequency == 2484) return "14";
        if (frequency >= 2412 && frequency <= 2472) return String.valueOf((frequency - 2407) / 5);
        if (frequency >= 5000 && frequency <= 5895) return String.valueOf((frequency - 5000) / 5);
        if (frequency >= 5955 && frequency <= 7115) return String.valueOf((frequency - 5950) / 5);
        return "neurčený";
    }

    private String wifiDecodedData(android.net.wifi.ScanResult result) {
        if (result == null) return "";
        List<String> rows = new ArrayList<>();
        rows.add("BSSID: " + result.BSSID);
        rows.add("SSID: " + (TextUtils.isEmpty(result.SSID) ? "skryté/neznámé" : result.SSID));
        rows.add("RSSI: " + result.level + " dBm");
        rows.add("Frekvence: " + result.frequency + " MHz, kanál " + wifiChannel(result.frequency));
        rows.add("Šířka kanálu: " + channelWidthLabel(result.channelWidth));
        if (result.centerFreq0 > 0) rows.add("CenterFreq0: " + result.centerFreq0 + " MHz");
        if (result.centerFreq1 > 0) rows.add("CenterFreq1: " + result.centerFreq1 + " MHz");
        if (Build.VERSION.SDK_INT >= 30) rows.add("Wi‑Fi standard: " + wifiStandardLabel(result.getWifiStandard()));
        if (Build.VERSION.SDK_INT >= 23) {
            if (result.isPasspointNetwork()) rows.add("Passpoint/Hotspot 2.0: ano");
            CharSequence operator = result.operatorFriendlyName;
            if (!TextUtils.isEmpty(operator)) rows.add("Operátor: " + operator);
        }
        rows.add("Capabilities: " + result.capabilities);
        if (result.timestamp > 0) rows.add("Beacon timestamp: " + result.timestamp + " μs od startu systému");
        return joinLines(rows);
    }

    private String channelWidthLabel(int width) {
        switch (width) {
            case android.net.wifi.ScanResult.CHANNEL_WIDTH_20MHZ: return "20 MHz";
            case android.net.wifi.ScanResult.CHANNEL_WIDTH_40MHZ: return "40 MHz";
            case android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ: return "80 MHz";
            case android.net.wifi.ScanResult.CHANNEL_WIDTH_160MHZ: return "160 MHz";
            case android.net.wifi.ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ: return "80+80 MHz";
            default: return "neurčeno";
        }
    }

    private String wifiStandardLabel(int standard) {
        if (Build.VERSION.SDK_INT < 30) return "neurčeno";
        switch (standard) {
            case android.net.wifi.ScanResult.WIFI_STANDARD_LEGACY: return "Legacy 802.11a/b/g";
            case android.net.wifi.ScanResult.WIFI_STANDARD_11N: return "Wi‑Fi 4 / 802.11n";
            case android.net.wifi.ScanResult.WIFI_STANDARD_11AC: return "Wi‑Fi 5 / 802.11ac";
            case android.net.wifi.ScanResult.WIFI_STANDARD_11AX: return "Wi‑Fi 6/6E / 802.11ax";
            case android.net.wifi.ScanResult.WIFI_STANDARD_11AD: return "WiGig / 802.11ad";
            case android.net.wifi.ScanResult.WIFI_STANDARD_11BE: return "Wi‑Fi 7 / 802.11be";
            default: return "neurčeno";
        }
    }

    private String joinLines(List<String> rows) {
        StringBuilder out = new StringBuilder();
        for (String row : rows) {
            if (TextUtils.isEmpty(row)) continue;
            if (out.length() > 0) out.append('\n');
            out.append(row);
        }
        return out.toString();
    }

    private boolean isSpecificWifiType(String type) {
        return "CAMERA".equals(type) || "AUDIO".equals(type) || "IOT".equals(type)
                || "SMART_HOME".equals(type) || "SCOOTER".equals(type)
                || "COMPUTER".equals(type) || "PHONE".equals(type);
    }

    private void runSsdpDiscovery() {
        String query = "M-SEARCH * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "MAN: \"ssdp:discover\"\r\n" +
                "MX: 3\r\n" +
                "ST: ssdp:all\r\n\r\n";
        discoverUdp(query, 1900, false);
    }

    private void addMdnsDevice(String name, String serviceType, String host, int port) {
        String safeName = TextUtils.isEmpty(name) ? "mDNS zařízení" : name;
        String type = serviceType == null ? "" : serviceType;
        String evidence = safeName + " " + type;
        Classification classification = classify(evidence, "mDNS / DNS‑SD", -1);
        String address = TextUtils.isEmpty(host) ? "mDNS" : host;
        DeviceInfo info = new DeviceInfo("MDNS:" + type + ":" + safeName, safeName, address,
                "Lokální síť • mDNS / DNS‑SD", classification.type, 0, -1,
                classification.confidence, classification.reason,
                port > 0 ? "Port " + port : "Služba byla oznámena bez dostupného portu");
        info.services = type + (port > 0 ? " • TCP/UDP port " + port : "");
        info.model = mdnsModel(type, safeName);
        info.protocolData = "mDNS služba " + type;
        info.transport = "NETWORK";
        addOrUpdate(info);
    }

    private String mdnsModel(String type, String name) {
        String value = (type + " " + name).toLowerCase(Locale.ROOT);
        if (value.contains("_googlecast")) return "Google Cast / Chromecast zařízení";
        if (value.contains("_airplay") || value.contains("_raop")) return "Apple AirPlay zvukové nebo obrazové zařízení";
        if (value.contains("_rtsp")) return "RTSP streamovací zařízení – může poskytovat video nebo zvuk";
        if (value.contains("_ipp") || value.contains("_printer")) return "Síťová tiskárna";
        if (value.contains("_hap")) return "Apple HomeKit příslušenství";
        if (value.contains("_matter")) return "Matter chytré zařízení";
        if (value.contains("_smb") || value.contains("_workstation")) return "Počítač nebo síťové úložiště";
        return "Zařízení oznamující lokální síťovou službu";
    }

    private void runOnvifDiscovery() {
        String id = "uuid:" + java.util.UUID.randomUUID();
        String query = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:w=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\" xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\">" +
                "<e:Header><w:MessageID>" + id + "</w:MessageID><w:To e:mustUnderstand=\"true\">urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To><w:Action e:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action></e:Header>" +
                "<e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></e:Body></e:Envelope>";
        discoverUdp(query, 3702, true);
    }

    private void discoverUdp(String query, int port, boolean onvif) {
        WifiManager.MulticastLock lock = null;
        DatagramSocket socket = null;
        try {
            if (!isOnWifi()) return;
            if (wifiManager != null) {
                lock = wifiManager.createMulticastLock("kuba-nearby-discovery");
                lock.setReferenceCounted(false);
                lock.acquire();
            }
            socket = new DatagramSocket();
            socket.setSoTimeout(900);
            byte[] data = query.getBytes(StandardCharsets.UTF_8);
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("239.255.255.250"), port));
            long end = System.currentTimeMillis() + 5500;
            while (System.currentTimeMillis() < end) {
                try {
                    byte[] buffer = new byte[12000];
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    socket.receive(response);
                    String body = new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8);
                    processNetworkResponse(response.getAddress().getHostAddress(), body, onvif);
                } catch (java.net.SocketTimeoutException ignored) { }
            }
        } catch (Exception ignored) {
        } finally {
            if (socket != null) socket.close();
            if (lock != null && lock.isHeld()) lock.release();
        }
    }

    private boolean isOnWifi() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private void processNetworkResponse(String ip, String body, boolean onvif) {
        String location = onvif ? firstUrl(body) : header(body, "location");
        String server = onvif ? tag(body, "Scopes") : header(body, "server");
        String usn = onvif ? tag(body, "Types") : header(body, "usn");
        String evidence = (server + " " + usn + " " + body).toLowerCase(Locale.ROOT);
        Classification c = onvif
                ? new Classification("CAMERA", 98, "ONVIF NetworkVideoTransmitter odpověděl")
                : classify(evidence, "Síť SSDP", -1);
        String name = onvif ? "Možná síťová kamera" : readableNetworkName(server, usn);
        DeviceInfo info = new DeviceInfo("NET:" + ip, name, ip,
                onvif ? "ONVIF / WS‑Discovery" : "Lokální síť / SSDP", c.type, 0, -1,
                c.confidence, c.reason, location);
        info.model = onvif ? "ONVIF síťová videokamera" : readableNetworkName(server, usn);
        info.services = onvif ? "ONVIF WS‑Discovery • NetworkVideoTransmitter" : "UPnP / SSDP";
        info.protocolData = shorten((server + " " + usn).trim(), 180);
        info.transport = "NETWORK";
        addOrUpdate(info);
        if (!TextUtils.isEmpty(location) && location.startsWith("http")) executor.submit(() -> enrichFromDescription(ip, location));
    }

    private void enrichFromDescription(String ip, String location) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(location);
            if (!isPrivateAddress(InetAddress.getByName(url.getHost()))) return;
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(1400);
            connection.setReadTimeout(1400);
            connection.setRequestProperty("User-Agent", "KUBA-Nearby-Scanner/1.3");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder xml = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && xml.length() < 80000) xml.append(line);
            String friendly = tag(xml.toString(), "friendlyName");
            String maker = tag(xml.toString(), "manufacturer");
            String model = tag(xml.toString(), "modelName");
            String details = friendly + " " + maker + " " + model + " " + xml;
            Classification c = classify(details, "Síť SSDP", -1);
            DeviceInfo update = new DeviceInfo("NET:" + ip,
                    TextUtils.isEmpty(friendly) ? readableNetworkName(maker, model) : friendly,
                    ip, "Lokální síť / SSDP", c.type, 0, -1, c.confidence,
                    c.reason, (maker + " " + model).trim());
            update.vendor = maker;
            update.model = model;
            update.services = "UPnP / SSDP zařízení s XML popisem";
            update.protocolData = location;
            update.transport = "NETWORK";
            addOrUpdate(update);
        } catch (Exception ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private boolean isPrivateAddress(InetAddress address) {
        return address.isSiteLocalAddress() || address.isLinkLocalAddress() || address.isLoopbackAddress();
    }

    private String readableNetworkName(String first, String second) {
        String value = ((first == null ? "" : first) + " " + (second == null ? "" : second)).trim();
        if (value.isEmpty()) return "Síťové zařízení";
        value = value.replaceAll("(?i)uuid:[^ ]+", "").replaceAll("(?i)urn:[^ ]+", "").trim();
        return value.isEmpty() ? "Síťové zařízení" : shorten(value, 60);
    }

    private String header(String body, String wanted) {
        for (String line : body.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase(wanted)) return line.substring(colon + 1).trim();
        }
        return "";
    }

    private String tag(String xml, String name) {
        Pattern p = Pattern.compile("<(?:[A-Za-z0-9_]+:)?" + Pattern.quote(name) + "[^>]*>(.*?)</(?:[A-Za-z0-9_]+:)?" + Pattern.quote(name) + ">", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(xml == null ? "" : xml);
        return m.find() ? m.group(1).replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim() : "";
    }

    private String firstUrl(String text) {
        Matcher m = Pattern.compile("https?://[^<\\s]+", Pattern.CASE_INSENSITIVE).matcher(text == null ? "" : text);
        return m.find() ? m.group() : "";
    }

    private Classification classify(String text, String source, int btMajor) {
        String value = (text == null ? "" : text).toLowerCase(Locale.ROOT);
        if (btMajor == BluetoothClass.Device.Major.AUDIO_VIDEO)
            return new Classification("AUDIO", 94, "Bluetooth třída Audio/Video");
        if (btMajor == BluetoothClass.Device.Major.PHONE)
            return new Classification("PHONE", 92, "Bluetooth třída telefonu/tabletu");
        if (btMajor == BluetoothClass.Device.Major.COMPUTER)
            return new Classification("COMPUTER", 92, "Bluetooth třída počítače");
        if (btMajor == BluetoothClass.Device.Major.WEARABLE)
            return new Classification("WEARABLE", 94, "Bluetooth třída nositelného zařízení");
        if (value.contains("networkvideotransmitter") || value.contains("onvif"))
            return new Classification("CAMERA", 96, "ONVIF / síťový video profil");
        if (containsAny(value, "spy", "hidden camera", "spy cam", "mini cam", "a9 camera", "sq11", "v380", "yoosee", "icsee", "xmeye"))
            return new Classification("CAMERA", 80, "Název nebo profil odpovídá častému spy/kamerovému zařízení");
        if (containsAny(value, "hikvision", "dahua", "reolink", "ezviz", "imou", "uniview", "axis", "ambarella", "hisilicon", "sigmastar", "ingenic", "goke", "xm camera", "ipcam", "ip cam", "camera", "webcam", "tapo c", "nvr", "dvr", "rtsp"))
            return new Classification("CAMERA", source.startsWith("Wi‑Fi") ? 58 : 84, "Název nebo síťový profil odpovídá kameře");
        if (containsAny(value, "watch", "smartwatch", "smart watch", "galaxy watch", "galaxy fit", "galaxy ring", "sm-r3", "sm-r4", "sm-r5", "sm-r8", "sm-r9", "apple watch", "garmin", "fitbit", "amazfit", "zepp", "huawei watch", "honor band", "honor watch", "mi band", "smart band", "redmi band", "redmi watch", "xiaomi watch", "fitness band", "ticwatch", "mobvoi", "polar", "suunto", "coros", "whoop", "oura", "heart rate", "running speed", "cycling speed", "wearable", "wrist", "pulse oximeter"))
            return new Classification("WEARABLE", 88, "Název, služba nebo Bluetooth appearance odpovídá hodinkám/náramku");
        if (containsAny(value, "m365", "mi scooter", "xiaomi scooter", "xiaomi electric scooter", "ninebot", "segway", "kickscooter", "kick scooter", "scooter", "e-scooter", "escooter", "e bike", "e-bike", "ebike", "bafang", "bosch ebike", "specialized levo", "vanmoof", "niu", "kukirin", "kugoo", "dualtron", "kaabo", "inmotion", "sur-ron", "surron"))
            return new Classification("SCOOTER", 86, "Název nebo BLE/Wi‑Fi profil odpovídá koloběžce, e-bike nebo dopravnímu zařízení");
        if (containsAny(value, "mediarenderer", "sonos", "speaker", "soundbar", "headphone", "headset", "earbud", "buds", "airpods", "galaxy buds", "freebuds", "redmi buds", "xiaomi buds", "soundcore", "anker", "jabra", "beats", "nothing ear", "jbl", "bose", "marshall", "raop", "airplay", "chromecast", "googlecast", "audio"))
            return new Classification("AUDIO", 86, "Zvukový profil nebo známý název zařízení");
        if (containsAny(value, "_rtsp._tcp"))
            return new Classification("CAMERA", 62, "Zařízení oznamuje RTSP stream; může přenášet video nebo pouze zvuk");
        if (containsAny(value, "_hap._tcp", "_matter._tcp", "homekit", "matter", "thread", "zigbee", "tuya", "smart life", "ewelink", "sonoff", "shelly", "aqara", "yeelight", "philips hue", "hue bridge", "nanoleaf", "tapo", "kasa smart", "meross", "switchbot", "govee", "ikea tradfri", "tradfri"))
            return new Classification("SMART_HOME", 88, "Název, výrobce nebo mDNS služba odpovídá chytré domácnosti");
        if (containsAny(value, "iphone", "ipad", "android phone", "pixel ", "pixel phone", "xperia", "galaxy s", "galaxy a", "galaxy z", "galaxy note", "galaxy tab", "samsung phone", "one ui", "sm-g", "sm-a", "sm-s", "sm-f", "sm-n", "redmi note", "poco", "oneplus", "oppo", "realme", "vivo", "iqoo", "honor phone", "huawei phone", "motorola", "moto g", "moto edge", "razr", "nokia", "hmd", "nothing phone", "cmf phone", "tecno", "infinix", "itel", "zte", "nubia", "redmagic", "meizu", "phone", "tablet"))
            return new Classification("PHONE", 82, "Název nebo třída odpovídá telefonu/tabletu");
        if (containsAny(value, "macbook", "imac", "windows", "laptop", "notebook", "pc", "desktop", "workstation", "nas", "synology", "qnap", "smb", "_smb._tcp", "_workstation._tcp"))
            return new Classification("COMPUTER", 82, "Název nebo lokální služba odpovídá počítači, notebooku nebo NAS");
        if (containsAny(value, "_hap._tcp", "_matter._tcp"))
            return new Classification("IOT", 88, "Potvrzená mDNS služba chytré domácnosti");
        if (containsAny(value, "asus", "asustek", "rog", "zenfone", "tuf"))
            return new Classification("BLUETOOTH", 84, "Asus/ROG název nebo výrobce zařízení");
        if (containsAny(value, "raspberry pi", "espressif", "esp32", "esp8266", "arduino", "nordic semiconductor", "nrf52", "nrf53", "telink", "silicon labs", "tuya", "smart life", "iot module", "embedded linux"))
            return new Classification("IOT", 78, "Známý výrobce nebo název malého IoT modulu");
        if (source.startsWith("Bluetooth")) return new Classification("BLUETOOTH", 100, "Bluetooth zařízení");
        if (source.startsWith("Wi‑Fi")) return new Classification("WIFI", 100, "Wi‑Fi přístupový bod");
        return new Classification("NETWORK", 70, "Zařízení odpovědělo v lokální síti");
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private void addSystemInfo(String message) {
        DeviceInfo info = new DeviceInfo("INFO:" + message, message, "", "Systém", "INFO", 0, -1, 100,
                "Informace o skenování", "");
        addOrUpdate(info);
    }

    private void addOrUpdate(DeviceInfo incoming) {
        registerScanHit(incoming);
        synchronized (devices) {
            DeviceInfo old = devices.get(incoming.key);
            if (old == null) devices.put(incoming.key, incoming);
            else {
                boolean oldStillFresh = old.inRange && System.currentTimeMillis() - old.lastSeenAt <= staleMsFor(old);
                if (incoming.inRange) {
                    old.lastSeenAt = incoming.lastSeenAt;
                    if (old.firstSeenAt == 0L) old.firstSeenAt = incoming.lastSeenAt;
                    old.inRange = true;
                    old.signalConfirmed = true;
                } else {
                    old.inRange = old.signalConfirmed && oldStillFresh;
                }
                if (!incoming.name.startsWith("Neznámé") && !incoming.name.equals("Síťové zařízení")) old.name = incoming.name;
                if (incoming.rssi != 0) {
                    if (Double.isNaN(old.filteredRssi)) old.filteredRssi = incoming.rssi;
                    else old.filteredRssi += clamp(incoming.rssi - old.filteredRssi, -10.0, 10.0) * 0.32;
                    old.rssi = (int) Math.round(old.filteredRssi);
                    old.sampleCount++;
                }
                if (incoming.distance >= 0) {
                    if (old.filteredDistance < 0) old.filteredDistance = incoming.distance;
                    else old.filteredDistance += (incoming.distance - old.filteredDistance) * 0.26;
                    old.distance = old.filteredDistance;
                }
                if (incoming.confidence >= old.confidence || "NETWORK".equals(old.type)) {
                    old.type = incoming.type;
                    old.confidence = incoming.confidence;
                    old.reason = incoming.reason;
                }
                if (!incoming.detail.isEmpty()) old.detail = incoming.detail;
                if (!incoming.vendor.isEmpty()) old.vendor = incoming.vendor;
                if (!incoming.model.isEmpty()) old.model = incoming.model;
                if (!incoming.deviceClass.isEmpty()) old.deviceClass = incoming.deviceClass;
                if (!incoming.services.isEmpty()) old.services = incoming.services;
                if (!incoming.manufacturerIds.isEmpty()) old.manufacturerIds = incoming.manufacturerIds;
                if (!incoming.appearance.isEmpty()) old.appearance = incoming.appearance;
                if (!incoming.addressType.isEmpty()) old.addressType = incoming.addressType;
                if (!incoming.rawAdvertisement.isEmpty()) old.rawAdvertisement = incoming.rawAdvertisement;
                if (!incoming.decodedName.isEmpty()) old.decodedName = incoming.decodedName;
                if (!incoming.decodedData.isEmpty()) old.decodedData = incoming.decodedData;
                if (!incoming.frequency.isEmpty()) old.frequency = incoming.frequency;
                if (!incoming.security.isEmpty()) old.security = incoming.security;
                if (!incoming.protocolData.isEmpty()) old.protocolData = incoming.protocolData;
                if (incoming.txPower != Integer.MIN_VALUE) old.txPower = incoming.txPower;
                if (!incoming.transport.isEmpty()) old.transport = incoming.transport;
                old.hiddenName = old.hiddenName && incoming.hiddenName;
                old.source = incoming.source;
            }
        }
        handler.post(this::renderDevices);
        if (incoming.inRange && appVisible && watchAlertsEnabled && watchedKeys.contains(incoming.key)) {
            notifyWatchedDevice(incoming);
        }
    }

    private void registerScanHit(DeviceInfo incoming) {
        if (incoming == null) return;
        if (!incoming.inRange || "INFO".equals(incoming.type)) return;
        if ("BLUETOOTH".equals(incoming.transport) || incoming.key.startsWith("BT:")) scanBtKeys.add(incoming.key);
        else if ("WIFI".equals(incoming.transport) || incoming.key.startsWith("WIFI:")) scanWifiKeys.add(incoming.key);
        else if ("NETWORK".equals(incoming.transport) || incoming.key.startsWith("NET:") || incoming.key.startsWith("MDNS:")) scanNetworkKeys.add(incoming.key);
        if (incoming.hiddenName) scanHiddenKeys.add(incoming.key);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void startRangePruneLoop() {
        if (rangePruneTick != null) handler.removeCallbacks(rangePruneTick);
        rangePruneTick = new Runnable() {
            @Override public void run() {
                pruneOutOfRangeDevices(false);
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(rangePruneTick, 5000);
    }

    private void pruneOutOfRangeDevices(boolean removeAllStale) {
        long now = System.currentTimeMillis();
        boolean changed = false;
        synchronized (devices) {
            List<String> remove = new ArrayList<>();
            for (DeviceInfo device : devices.values()) {
                if ("INFO".equals(device.type) || device.demo) continue;
                long age = now - device.lastSeenAt;
                boolean inRange = device.signalConfirmed && age <= staleMsFor(device);
                if (device.inRange != inRange) {
                    device.inRange = inRange;
                    changed = true;
                }
                // Uložená/spárovaná zařízení bez potvrzeného živého signálu držíme skrytá kvůli
                // pozdějšímu obohacení dat, ale nikdy je nezobrazujeme ani nepovažujeme za "stará".
                if (!device.signalConfirmed) continue;
                if ((removeAllStale || age > removeMsFor(device)) && !device.key.equals(trackingKey)) remove.add(device.key);
            }
            for (String key : remove) {
                devices.remove(key);
                changed = true;
            }
        }
        if (changed) handler.post(this::renderDevices);
    }

    private long staleMsFor(DeviceInfo device) {
        return "BLUETOOTH".equals(device.transport) ? BLUETOOTH_STALE_MS : RADIO_STALE_MS;
    }

    private long removeMsFor(DeviceInfo device) {
        return "BLUETOOTH".equals(device.transport) ? BLUETOOTH_REMOVE_MS : RADIO_REMOVE_MS;
    }

    private boolean shouldDisplayDevice(DeviceInfo device) {
        if (device == null) return false;
        if ("INFO".equals(device.type) || device.demo) return true;
        // Zobrazit jen zařízení s potvrzeným živým signálem, které je aktuálně v dosahu.
        // Uložená/spárovaná zařízení z paměti telefonu se bez skutečného vysílání nikdy neukážou.
        return device.signalConfirmed && device.inRange;
    }

    private void prepareDevicesForScan() {
        long now = System.currentTimeMillis();
        synchronized (devices) {
            List<String> remove = new ArrayList<>();
            for (DeviceInfo device : devices.values()) {
                if ("INFO".equals(device.type)) remove.add(device.key);
                else if ("BLUETOOTH".equals(device.transport)) device.inRange = device.signalConfirmed && now - device.lastSeenAt <= BLUETOOTH_STALE_MS;
                else device.inRange = false;
            }
            for (String key : remove) devices.remove(key);
        }
    }

    private void renderDevices() {
        if (deviceList == null) return;
        List<DeviceInfo> all;
        synchronized (devices) {
            all = new ArrayList<>();
            for (DeviceInfo device : devices.values()) {
                if (shouldDisplayDevice(device)) all.add(device);
            }
        }
        if (sortByDistance) {
            Collections.sort(all, Comparator
                    .comparingDouble((DeviceInfo d) -> d.distance < 0 ? Double.MAX_VALUE : d.distance)
                    .thenComparingInt(d -> categoryRank(d.type))
                    .thenComparingInt(d -> d.rssi == 0 ? 999 : -d.rssi));
        } else {
            Collections.sort(all, Comparator
                    .comparingInt((DeviceInfo d) -> categoryRank(d.type))
                    .thenComparingInt(d -> d.rssi == 0 ? 999 : -d.rssi));
        }
        int cams = 0, audio = 0, realDevices = 0;
        for (DeviceInfo d : all) {
            if (!"INFO".equals(d.type)) realDevices++;
            if ("CAMERA".equals(d.type)) cams++;
            if ("AUDIO".equals(d.type)) audio++;
        }
        totalCount.setText(String.valueOf(realDevices));
        cameraCount.setText(String.valueOf(cams));
        audioCount.setText(String.valueOf(audio));
        if (radarView != null) {
            List<RadarView.RadarDevice> markers = new ArrayList<>();
            for (DeviceInfo d : all) markers.add(new RadarView.RadarDevice(d.key, d.name, d.transport,
                    d.type, d.distance, d.rssi, d.hiddenName));
            radarView.setDevices(markers);
            if (fullscreenRadarView != null) fullscreenRadarView.setDevices(markers);
        }
        deviceList.removeAllViews();
        int shown = 0;
        for (DeviceInfo d : all) {
            if (!matchesFilter(d)) continue;
            deviceList.addView(deviceCard(d));
            shown++;
        }
        if (shown == 0) showEmptyState();
    }

    private boolean matchesFilter(DeviceInfo d) {
        if ("ALL".equals(activeFilter) || "RADAR".equals(activeFilter) || "SECURITY".equals(activeFilter) || "SETTINGS".equals(activeFilter)) return true;
        if ("WATCHED".equals(activeFilter)) return watchedKeys.contains(d.key);
        if ("SPY".equals(activeFilter)) return isSpyLike(d);
        if ("HIDDEN".equals(activeFilter)) return d.hiddenName && ("BLUETOOTH".equals(d.transport) || "WIFI".equals(d.transport));
        if ("NAMED".equals(activeFilter)) return !d.hiddenName && ("BLUETOOTH".equals(d.transport) || "WIFI".equals(d.transport));
        if ("BLUETOOTH".equals(activeFilter)) return "BLUETOOTH".equals(d.transport);
        if ("WIFI".equals(activeFilter)) return "WIFI".equals(d.transport);
        if (isBrandFilter(activeFilter)) return matchesBrand(d, activeFilter);
        return activeFilter.equals(d.type);
    }

    private boolean isBrandFilter(String id) {
        return "MI_BAND".equals(id) || "APPLE".equals(id) || "SAMSUNG".equals(id) || "GARMIN".equals(id)
                || "FITBIT".equals(id) || "AMAZFIT".equals(id) || "HUAWEI".equals(id) || "ASUS".equals(id)
                || "GOOGLE".equals(id) || "SONY".equals(id) || "MOTOROLA".equals(id) || "BBK".equals(id)
                || "NOKIA".equals(id) || "NOTHING".equals(id) || "AUDIO_BRANDS".equals(id)
                || "OTHER_WEARABLE".equals(id);
    }

    private boolean matchesBrand(DeviceInfo d, String brand) {
        String value = deviceEvidence(d);
        switch (brand) {
            case "MI_BAND": return containsAny(value, "mi band", "miband", "smart band", "redmi band", "redmi watch", "xiaomi watch", "xiaomi band", "xiaomi", "huami", "mijia");
            case "APPLE": return containsAny(value, "apple", "airtag", "airpods", "find my", "watchos", "iwatch", "iphone", "ipad", "macbook", "0x004c");
            case "SAMSUNG": return containsAny(value, "samsung", "galaxy watch", "galaxy fit", "galaxy ring", "galaxy buds", "galaxy s", "galaxy a", "galaxy z", "galaxy note", "galaxy tab", "smarttag", "smartthings", "one ui", "sm-g", "sm-a", "sm-s", "sm-f", "sm-n", "sm-r", "fd5a", "fd4b");
            case "GARMIN": return containsAny(value, "garmin", "forerunner", "fenix", "venu", "vivoactive", "instinct");
            case "FITBIT": return containsAny(value, "fitbit", "sense", "versa", "charge", "inspire");
            case "AMAZFIT": return containsAny(value, "amazfit", "zepp", "huami", "gtr", "gts", "bip", "t-rex");
            case "HUAWEI": return containsAny(value, "huawei", "honor", "freebuds", "huawei phone", "honor phone");
            case "ASUS": return containsAny(value, "asus", "asustek", "rog", "zenfone", "tuf");
            case "GOOGLE": return containsAny(value, "google", "pixel", "nest", "fast pair", "fitbit");
            case "SONY": return containsAny(value, "sony", "xperia", "walkman", "bravia", "linkbuds", "wf-", "wh-");
            case "MOTOROLA": return containsAny(value, "motorola", "moto ", "moto g", "moto edge", "razr", "lenovo", "thinkpad");
            case "BBK": return containsAny(value, "oppo", "oneplus", "realme", "vivo", "iqoo", "bbk electronics");
            case "NOKIA": return containsAny(value, "nokia", "hmd global", "hmd ");
            case "NOTHING": return containsAny(value, "nothing phone", "nothing ear", "cmf phone", "cmf watch", "cmf buds", "nothing technology");
            case "AUDIO_BRANDS": return containsAny(value, "jbl", "bose", "sennheiser", "jabra", "marshall", "soundcore", "anker", "skullcandy", "edifier", "beats", "audio-technica");
            case "OTHER_WEARABLE": return "WEARABLE".equals(d.type) && containsAny(value,
                    "polar", "suunto", "coros", "whoop", "withings", "oura", "ultrahuman",
                    "ticwatch", "mobvoi", "haylou", "mibro", "colmi", "kospet", "lemfo",
                    "oppo watch", "oneplus watch", "realme watch", "boatlunar", "noise", "fire-boltt");
            default: return false;
        }
    }

    private boolean isSpyLike(DeviceInfo d) {
        String value = deviceEvidence(d);
        return "TRACKER".equals(d.type) || "CAMERA".equals(d.type) || d.hiddenName
                || containsAny(value, "spy", "hidden camera", "camera", "ipcam", "onvif", "rtsp", "airtag",
                "smarttag", "tile", "chipolo", "pebblebee", "tracker", "find my", "microphone", "recorder",
                "nvr", "dvr", "tuya", "esp32", "nordic");
    }

    private String deviceEvidence(DeviceInfo d) {
        return (d.name + " " + d.vendor + " " + d.model + " " + d.deviceClass + " " + d.services + " "
                + d.manufacturerIds + " " + d.appearance + " " + d.decodedName + " " + d.decodedData + " "
                + d.frequency + " " + d.security + " " + d.protocolData + " " + d.reason + " " + d.detail + " "
                + d.address).toLowerCase(Locale.ROOT);
    }

    private int categoryRank(String type) {
        if ("TRACKER".equals(type)) return 0;
        if ("WEARABLE".equals(type)) return 1;
        if ("CAMERA".equals(type)) return 2;
        if ("AUDIO".equals(type)) return 3;
        if ("SCOOTER".equals(type)) return 4;
        if ("SMART_HOME".equals(type)) return 5;
        if ("IOT".equals(type)) return 6;
        if ("PHONE".equals(type)) return 7;
        if ("COMPUTER".equals(type)) return 8;
        if ("BLUETOOTH".equals(type)) return 9;
        if ("WIFI".equals(type)) return 10;
        if ("NETWORK".equals(type)) return 11;
        return 12;
    }

    private View deviceCard(DeviceInfo d) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(13), dp(15), dp(13));
        int stripe = colorForType(d.type);
        card.setBackground(round(SURFACE, 15, stripe, 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> showDeviceDetails(d));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = label(typeLabel(d.type), 10, stripe, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(9), dp(4), dp(9), dp(4));
        badge.setBackground(round(SURFACE_2, 12, 0, 0));
        top.addView(badge);
        if (watchedKeys.contains(d.key)) {
            TextView watched = label("HLÍDÁM", 10, BG, Typeface.BOLD);
            watched.setGravity(Gravity.CENTER);
            watched.setPadding(dp(8), dp(4), dp(8), dp(4));
            watched.setBackground(round(TRACKER, 12, 0, 0));
            LinearLayout.LayoutParams watchParams = new LinearLayout.LayoutParams(-2, -2);
            watchParams.setMargins(dp(6), 0, 0, 0);
            top.addView(watched, watchParams);
        }
        TextView signal = label(d.rssi == 0 ? "" : signalBars(d.rssi) + "  " + d.rssi + " dBm", 11, MUTED, Typeface.BOLD);
        signal.setGravity(Gravity.END);
        top.addView(signal, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(top);

        TextView name = label(d.name, 17, TEXT, Typeface.BOLD);
        name.setPadding(0, dp(8), 0, dp(3));
        card.addView(name);
        String meta = d.source;
        if (!d.address.isEmpty()) meta += "  •  " + d.address;
        if ("BLUETOOTH".equals(d.transport)) meta += "  •  " + bluetoothPresenceLabel(d);
        else if (!"INFO".equals(d.type)) meta += "  •  " + genericPresenceLabel(d);
        card.addView(label(meta, 11, MUTED, Typeface.NORMAL));

        LinearLayout facts = new LinearLayout(this);
        facts.setPadding(0, dp(9), 0, 0);
        if (d.distance >= 0) facts.addView(fact("VZDÁLENOST", formatDistance(d.distance)));
        if (isImportantType(d.type)) facts.addView(fact("JISTOTA", d.confidence + " %"));
        if (d.sampleCount > 1) facts.addView(fact("VZORKY", String.valueOf(d.sampleCount)));
        if (!d.detail.isEmpty()) facts.addView(fact("DETAIL", shorten(d.detail, 32)));
        card.addView(facts);

        if (!d.vendor.isEmpty() || !d.model.isEmpty()) {
            String identified = !d.model.isEmpty() ? d.model : d.vendor;
            TextView match = label("Rozpoznání: " + shorten(identified, 90), 12, TEXT, Typeface.BOLD);
            match.setPadding(0, dp(8), 0, 0);
            card.addView(match);
        }

        TextView reason = label("Proč: " + d.reason, 11, MUTED, Typeface.NORMAL);
        reason.setPadding(0, dp(8), 0, 0);
        card.addView(reason);
        TextView open = label("Klepnutím otevřeš úplný detail. Vzdálenost i označení trackeru jsou odhady podle vysílaných dat; náhodná MAC může skrýt výrobce.", 10, MUTED, Typeface.NORMAL);
        open.setGravity(Gravity.START);
        open.setPadding(0, dp(9), 0, 0);
        card.addView(open);
        return card;
    }

    private View fact(String caption, String value) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, 0, dp(18), 0);
        box.addView(label(caption, 9, MUTED, Typeface.BOLD));
        box.addView(label(value, 13, TEXT, Typeface.BOLD));
        return box;
    }

    private void showDeviceDetails(DeviceInfo d) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final String trackedKey = d.key;
        startTargetTracking(trackedKey);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(18), dp(20), dp(18));
        panel.setBackground(round(SURFACE, 20, colorForType(d.type), 1));
        scroll.addView(panel, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = label(typeLabel(d.type), 11, colorForType(d.type), Typeface.BOLD);
        badge.setPadding(dp(10), dp(5), dp(10), dp(5));
        badge.setBackground(round(SURFACE_2, 13, 0, 0));
        header.addView(badge);
        TextView x = label("×", 28, TEXT, Typeface.NORMAL);
        x.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        x.setOnClickListener(v -> dialog.dismiss());
        header.addView(x, new LinearLayout.LayoutParams(0, dp(42), 1));
        panel.addView(header);

        TextView title = label(d.name, 23, TEXT, Typeface.BOLD);
        title.setPadding(0, dp(10), 0, dp(4));
        panel.addView(title);
        if (!d.model.isEmpty() && !d.model.equals(d.name))
            panel.addView(label("Rozpoznáno jako: " + d.model, 14, colorForType(d.type), Typeface.BOLD));

        Button watch = new Button(this);
        watch.setText(watchedKeys.contains(d.key) ? "ODEBRAT ZE SLEDOVANÝCH" : "HLÍDAT TOHLE ZAŘÍZENÍ");
        watch.setTextColor(watchedKeys.contains(d.key) ? TEXT : BG);
        watch.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        watch.setBackground(round(watchedKeys.contains(d.key) ? SURFACE_2 : TRACKER, 14, 0, 0));
        watch.setOnClickListener(v -> {
            DeviceInfo current = latestDevice(trackedKey);
            toggleWatched(current == null ? d : current);
            watch.setText(watchedKeys.contains(trackedKey) ? "ODEBRAT ZE SLEDOVANÝCH" : "HLÍDAT TOHLE ZAŘÍZENÍ");
            watch.setTextColor(watchedKeys.contains(trackedKey) ? TEXT : BG);
            watch.setBackground(round(watchedKeys.contains(trackedKey) ? SURFACE_2 : TRACKER, 14, 0, 0));
        });
        LinearLayout.LayoutParams watchParams = new LinearLayout.LayoutParams(-1, dp(46));
        watchParams.setMargins(0, dp(12), 0, 0);
        panel.addView(watch, watchParams);

        LinearLayout live = new LinearLayout(this);
        live.setOrientation(LinearLayout.VERTICAL);
        live.setPadding(dp(15), dp(13), dp(15), dp(13));
        live.setBackground(round(Color.rgb(10, 25, 35), 15, colorForType(d.type), 1));
        TextView liveCaption = label("ŽIVÉ DOHLEDÁVÁNÍ CÍLE", 10, colorForType(d.type), Typeface.BOLD);
        DirectionArrowView liveArrow = new DirectionArrowView(this);
        liveArrow.setColors(colorForType(d.type), TEXT, MUTED, Color.rgb(12, 30, 42));
        TextView liveTitle = label("", 18, TEXT, Typeface.BOLD);
        liveTitle.setGravity(Gravity.CENTER);
        liveTitle.setPadding(0, dp(7), 0, dp(3));
        TextView liveBody = label("", 12, MUTED, Typeface.NORMAL);
        live.addView(liveCaption);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(-1, dp(210));
        arrowParams.setMargins(0, dp(8), 0, dp(5));
        live.addView(liveArrow, arrowParams);
        live.addView(liveTitle);
        live.addView(liveBody);
        LinearLayout.LayoutParams liveParams = new LinearLayout.LayoutParams(-1, -2);
        liveParams.setMargins(0, dp(13), 0, dp(5));
        panel.addView(live, liveParams);
        updateTrackingPanel(latestDevice(trackedKey) == null ? d : latestDevice(trackedKey), liveArrow, liveTitle, liveBody);
        Runnable detailTick = new Runnable() {
            @Override public void run() {
                DeviceInfo current = latestDevice(trackedKey);
                if (current != null) updateTrackingPanel(current, liveArrow, liveTitle, liveBody);
                if (trackedKey.equals(trackingKey) && !isFinishing()) handler.postDelayed(this, 250);
            }
        };
        handler.postDelayed(detailTick, 250);

        if ("TRACKER".equals(d.type)) {
            TextView warning = label("⚠ Možné sledovací zařízení v dosahu. Samotný nález neznamená, že sleduje právě tebe. Sleduj, zda se stejné zařízení opakovaně pohybuje s telefonem.", 12, TEXT, Typeface.BOLD);
            warning.setPadding(dp(13), dp(12), dp(13), dp(12));
            warning.setBackground(round(Color.rgb(69, 49, 22), 13, TRACKER, 1));
            LinearLayout.LayoutParams warningParams = new LinearLayout.LayoutParams(-1, -2);
            warningParams.setMargins(0, dp(12), 0, dp(5));
            panel.addView(warning, warningParams);
        }

        addDetail(panel, "ZDROJ SKENU", d.source);
        addDetail(panel, "STAV V DOSAHU", d.inRange ? "V dosahu – signál byl čerstvě zachycen" : "Mimo dosah – v hlavním seznamu se nezobrazuje");
        addDetail(panel, "NAPOSLEDY ZACHYCENO", ageText(d.lastSeenAt));
        addDetail(panel, "PRVNÍ ZACHYCENÍ", ageText(d.firstSeenAt));
        addDetail(panel, "AUTOMATICKÉ MAZÁNÍ", autoDeleteLabel(d));
        addDetail(panel, "ADRESA", d.address);
        if ("BLUETOOTH".equals(d.transport)) addDetail(panel, "DOSAH", bluetoothPresenceLabel(d));
        addDetail(panel, "TYP ADRESY", d.addressType);
        addDetail(panel, "VÝROBCE", d.vendor.isEmpty() ? "Neurčen – zařízení výrobce nevysílá nebo používá náhodnou MAC" : d.vendor);
        addDetail(panel, "KATEGORIE ZAŘÍZENÍ", d.deviceClass);
        addDetail(panel, "MOŽNÉ TYPY", possibleDeviceTypes(d));
        addDetail(panel, "DEKÓDOVANÝ NÁZEV Z BLE", d.decodedName);
        addDetail(panel, "BLUETOOTH APPEARANCE", d.appearance);
        addDetail(panel, "BLUETOOTH SIG VÝROBCE", d.manufacturerIds);
        addDetail(panel, "VYSÍLANÉ SLUŽBY", d.services);
        addDetail(panel, "DEKÓDOVANÁ BLE REKLAMA", d.decodedData);
        addDetail(panel, "FREKVENCE / KANÁL", d.frequency);
        addDetail(panel, "ZABEZPEČENÍ WI‑FI", d.security);
        if (d.rssi != 0) addDetail(panel, "SÍLA SIGNÁLU", signalBars(d.rssi) + "  " + d.rssi + " dBm");
        addDetail(panel, "SIGNÁLOVÁ POZNÁMKA", signalHint(d));
        if (d.txPower != Integer.MIN_VALUE) addDetail(panel, "TX POWER", d.txPower + " dBm");
        if (d.distance >= 0) addDetail(panel, "ORIENTAČNÍ VZDÁLENOST", formatDistance(d.distance));
        if (isImportantType(d.type))
            addDetail(panel, "JISTOTA ROZPOZNÁNÍ", d.confidence + " %");
        addDetail(panel, "PŘESNOST VZDÁLENOSTI", distanceQuality(d));
        addDetail(panel, "DŮVOD ZAŘAZENÍ", d.reason);
        addDetail(panel, "DALŠÍ DATA", d.detail);
        addDetail(panel, "PROTOKOLOVÁ DATA", d.protocolData);
        addDetail(panel, "RAW BLUETOOTH REKLAMA", d.rawAdvertisement);
        addDetail(panel, "VŠECHNA DOSTUPNÁ DATA", allKnownData(d));

        TextView note = label("Výrobce MAC označuje držitele síťového prefixu nebo výrobce modulu. Nemusí být totožný se značkou celého výrobku. Přesný model lze ukázat jen tehdy, když ho zařízení zveřejní.", 11, MUTED, Typeface.NORMAL);
        note.setPadding(0, dp(15), 0, dp(12));
        panel.addView(note);

        Button close = new Button(this);
        close.setText("ZAVŘÍT DETAIL");
        close.setTextColor(BG);
        close.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        close.setBackground(round(ACCENT, 14, 0, 0));
        close.setOnClickListener(v -> dialog.dismiss());
        panel.addView(close, new LinearLayout.LayoutParams(-1, dp(48)));

        dialog.setContentView(scroll);
        dialog.setOnDismissListener(ignored -> {
            handler.removeCallbacks(detailTick);
            if (trackedKey.equals(trackingKey)) stopTargetTracking();
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.72f);
            int width = getResources().getDisplayMetrics().widthPixels - dp(24);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90f);
            window.setLayout(width, height);
            window.setGravity(Gravity.CENTER);
        }
    }

    private void updateTrackingPanel(DeviceInfo device, DirectionArrowView arrow, TextView title, TextView body) {
        if (device == null) return;
        int rssi = device.rssi;
        int delta = 0;
        if (rssi != 0) {
            if (trackingLastRssi != Integer.MIN_VALUE) delta = rssi - trackingLastRssi;
            trackingLastRssi = rssi;
            if (currentHeadingDeg >= 0 && (trackingBestHeadingDeg < 0 || trackingBestRssi == Integer.MIN_VALUE || rssi >= trackingBestRssi + 2)) {
                trackingBestRssi = rssi;
                trackingBestHeadingDeg = currentHeadingDeg;
            }
            updateGpsSignalBearing(rssi);
        }
        String distance = device.distance >= 0 ? formatDistance(device.distance) : "metry neurčeny";
        String signal = rssi == 0 ? "signál bez RSSI" : signalBars(rssi) + " " + rssi + " dBm";
        float targetHeading = bestTargetHeading();
        float relative = targetHeading >= 0 && currentHeadingDeg >= 0 ? shortestAngle(currentHeadingDeg, targetHeading) : 0f;
        arrow.setState(relative, currentHeadingDeg, targetHeading, rssi, device.distance, bluetoothPresenceLabel(device), directionConfidence(device));
        title.setText(distance + "  •  " + signal + directionText(relative, targetHeading));
        body.setText(trackingTrend(delta, rssi) + "\n"
                + "Otáčej se a choď pomalu: šipka drží směr nejsilnějšího signálu a venku si pomáhá GPS pohybem. Přesný Bluetooth kompas bez AoA/AoD hardwaru Android běžně nedává.");
    }

    private void updateGpsSignalBearing(int rssi) {
        if (rssi == 0 || previousTrackingLocation == null || currentTrackingLocation == null) return;
        float moved = previousTrackingLocation.distanceTo(currentTrackingLocation);
        if (moved < 0.8f) return;
        float bearing = normalizeDegrees(previousTrackingLocation.bearingTo(currentTrackingLocation));
        if (gpsLastRssi != Integer.MIN_VALUE && rssi >= gpsLastRssi + 2 && (gpsBestRssi == Integer.MIN_VALUE || rssi >= gpsBestRssi)) {
            gpsBestRssi = rssi;
            gpsBestBearingDeg = bearing;
        }
        gpsLastRssi = rssi;
    }

    private float bestTargetHeading() {
        if (gpsBestBearingDeg >= 0 && gpsBestRssi >= trackingBestRssi - 1) return smoothHeadingBlend(trackingBestHeadingDeg, gpsBestBearingDeg, 0.35f);
        return trackingBestHeadingDeg;
    }

    private float smoothHeadingBlend(float first, float second, float weightSecond) {
        if (first < 0) return second;
        if (second < 0) return first;
        return normalizeDegrees(first + shortestAngle(first, second) * weightSecond);
    }

    private int directionConfidence(DeviceInfo device) {
        int confidence = Math.min(95, 35 + device.sampleCount * 5);
        if (trackingBestHeadingDeg >= 0) confidence += 10;
        if (gpsBestBearingDeg >= 0) confidence += 8;
        if (device.distance >= 0 && device.distance < 8) confidence += 7;
        return Math.max(20, Math.min(95, confidence));
    }

    private String directionText(float relative, float targetHeading) {
        if (targetHeading < 0 || currentHeadingDeg < 0) return "";
        float abs = Math.abs(relative);
        if (abs < 15f) return " • drž rovně";
        return relative > 0 ? " • otoč doprava " + Math.round(abs) + "°" : " • otoč doleva " + Math.round(abs) + "°";
    }

    private String trackingTrend(int delta, int rssi) {
        if (rssi == 0) return "Sbírám data o cíli. Zkus se pomalu pohnout a počkej na další reklamní paket.";
        if (delta >= 4) return "Signál sílí: pokračuj tímto směrem, pravděpodobně se přibližuješ.";
        if (delta <= -4) return "Signál slábne: změň směr nebo se vrať k místu, kde byl signál silnější.";
        return "Signál je podobný: jdi pomalu a sleduj, kdy začne sílit.";
    }

    private boolean isImportantType(String type) {
        return "TRACKER".equals(type) || "CAMERA".equals(type) || "AUDIO".equals(type)
                || "WEARABLE".equals(type) || "SCOOTER".equals(type) || "SMART_HOME".equals(type)
                || "IOT".equals(type) || "PHONE".equals(type) || "COMPUTER".equals(type);
    }

    private static class DirectionArrowView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path arrowPath = new Path();
        private int arrowColor = ACCENT;
        private int textColor = TEXT;
        private int mutedColor = MUTED;
        private int surfaceColor = SURFACE_2;
        private float relativeDeg;
        private float currentHeading = -1f;
        private float targetHeading = -1f;
        private int rssi;
        private double distance = -1;
        private String presence = "";
        private int confidence;

        DirectionArrowView(Context context) {
            super(context);
        }

        void setColors(int arrowColor, int textColor, int mutedColor, int surfaceColor) {
            this.arrowColor = arrowColor;
            this.textColor = textColor;
            this.mutedColor = mutedColor;
            this.surfaceColor = surfaceColor;
        }

        void setState(float relativeDeg, float currentHeading, float targetHeading, int rssi, double distance, String presence, int confidence) {
            this.relativeDeg = relativeDeg;
            this.currentHeading = currentHeading;
            this.targetHeading = targetHeading;
            this.rssi = rssi;
            this.distance = distance;
            this.presence = presence == null ? "" : presence;
            this.confidence = confidence;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h * 0.43f;
            float radius = Math.min(w, h) * 0.34f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(surfaceColor);
            canvas.drawCircle(cx, cy, radius + 18f, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f);
            paint.setColor(arrowColor);
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(24f);
            paint.setColor(mutedColor);
            canvas.drawText("N", cx, cy - radius - 16f, paint);

            canvas.save();
            canvas.rotate(targetHeading >= 0 && currentHeading >= 0 ? relativeDeg : 0f, cx, cy);
            arrowPath.reset();
            arrowPath.moveTo(cx, cy - radius * 0.72f);
            arrowPath.lineTo(cx - radius * 0.25f, cy + radius * 0.08f);
            arrowPath.lineTo(cx - radius * 0.07f, cy + radius * 0.02f);
            arrowPath.lineTo(cx - radius * 0.07f, cy + radius * 0.55f);
            arrowPath.lineTo(cx + radius * 0.07f, cy + radius * 0.55f);
            arrowPath.lineTo(cx + radius * 0.07f, cy + radius * 0.02f);
            arrowPath.lineTo(cx + radius * 0.25f, cy + radius * 0.08f);
            arrowPath.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(targetHeading >= 0 ? arrowColor : mutedColor);
            canvas.drawPath(arrowPath, paint);
            canvas.restore();

            paint.setColor(textColor);
            paint.setTextSize(19f);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            canvas.drawText(targetHeading >= 0 ? "SMĚR CÍLE • " + confidence + " %" : "UČÍM SMĚR", cx, h - 42f, paint);
            paint.setColor(mutedColor);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(14f);
            String heading = currentHeading >= 0 ? Math.round(currentHeading) + "°" : "kompas čeká";
            String meters = distance >= 0 ? (distance < 10 ? String.format(Locale.forLanguageTag("cs-CZ"), "%.1f m", distance) : Math.round(distance) + " m") : "metry ?";
            String signal = rssi == 0 ? "RSSI ?" : rssi + " dBm";
            String shortPresence = presence.length() > 20 ? presence.substring(0, 20) : presence;
            canvas.drawText(heading + " • " + meters + " • " + signal + " • " + shortPresence, cx, h - 18f, paint);
        }
    }

    private void addDetail(LinearLayout panel, String caption, String value) {
        if (value == null || value.trim().isEmpty()) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(13), dp(10), dp(13), dp(10));
        row.setBackground(round(Color.rgb(12, 25, 35), 12, Color.rgb(37, 57, 70), 1));
        row.addView(label(caption, 9, MUTED, Typeface.BOLD));
        TextView content = label(value, 13, TEXT, Typeface.NORMAL);
        content.setTextIsSelectable(true);
        content.setPadding(0, dp(3), 0, 0);
        row.addView(content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(7), 0, 0);
        panel.addView(row, params);
    }

    private String distanceQuality(DeviceInfo d) {
        if (d.distance < 0) return "Nelze určit – zařízení nevyslalo použitelnou sílu signálu.";
        int score = 25;
        if (d.sampleCount >= 3) score += 20;
        if (d.sampleCount >= 8) score += 15;
        if (d.rssi <= -35 && d.rssi >= -82) score += 15;
        if (d.txPower != Integer.MIN_VALUE) score += 15;
        if ("WIFI".equals(d.transport) && !d.frequency.isEmpty()) score += 10;
        if (d.hiddenName) score -= 8;
        score = Math.max(10, Math.min(95, score));
        String note;
        if (score >= 75) note = "dobrý orientační odhad";
        else if (score >= 50) note = "střední odhad";
        else note = "hrubý odhad";
        return score + " % • " + note + " • RSSI ovlivní zdi, tělo, natočení telefonu a odrazy signálu.";
    }

    private String signalHint(DeviceInfo d) {
        if (d.rssi == 0) {
            return "Zařízení neposlalo měřitelnou sílu signálu. Může jít o starý síťový výsledek, spárovanou položku nebo zařízení, které právě nevysílá RSSI.";
        }
        if (d.rssi >= -50) return "Velmi silný signál – zařízení je pravděpodobně blízko, ale odrazy signálu mohou metry zkreslit.";
        if (d.rssi >= -65) return "Silný signál – sleduj, zda při přibližování RSSI stoupá a odhad metrů klesá.";
        if (d.rssi >= -78) return "Střední signál – zařízení může být za zdí, v tašce, autě nebo ve vedlejší místnosti.";
        return "Slabý signál – zařízení je dál, překryté, nebo vysílá nízkým výkonem.";
    }

    private String possibleDeviceTypes(DeviceInfo d) {
        String value = deviceEvidence(d);
        List<String> types = new ArrayList<>();
        if ("TRACKER".equals(d.type) || containsAny(value, "airtag", "smarttag", "tile", "chipolo", "pebblebee", "find my", "key finder", "locator")) types.add("tracker / lokátor");
        if ("WEARABLE".equals(d.type) || containsAny(value, "watch", "band", "heart rate", "cycling", "running speed", "galaxy fit", "galaxy ring")) types.add("hodinky / náramek / sportovní senzor");
        if ("PHONE".equals(d.type) || containsAny(value, "iphone", "ipad", "galaxy s", "galaxy a", "galaxy z", "galaxy note", "galaxy tab", "pixel", "phone", "tablet", "sm-g", "sm-a", "sm-s")) types.add("telefon / tablet");
        if ("CAMERA".equals(d.type) || containsAny(value, "camera", "onvif", "rtsp", "nvr", "dvr", "ipcam", "webcam")) types.add("kamera / video zařízení");
        if ("AUDIO".equals(d.type) || containsAny(value, "buds", "airpods", "headset", "speaker", "audio", "soundbar")) types.add("sluchátka / reproduktor / TV audio");
        if ("SMART_HOME".equals(d.type) || containsAny(value, "matter", "homekit", "tuya", "smart life", "zigbee", "sensor", "switch", "plug", "light")) types.add("chytrá domácnost / senzor");
        if ("SCOOTER".equals(d.type) || containsAny(value, "scooter", "e-bike", "ebike", "obd", "car audio", "tesla", "bmw", "skoda")) types.add("koloběžka / auto / dopravní modul");
        if ("COMPUTER".equals(d.type) || containsAny(value, "laptop", "desktop", "nas", "workstation", "macbook", "windows")) types.add("počítač / NAS");
        if (types.isEmpty()) types.add(typeLabel(d.type).toLowerCase(Locale.forLanguageTag("cs-CZ")));
        return joinUnique(types);
    }

    private String joinUnique(List<String> values) {
        List<String> unique = new ArrayList<>();
        for (String value : values) if (!unique.contains(value)) unique.add(value);
        return TextUtils.join(", ", unique);
    }

    private String autoDeleteLabel(DeviceInfo d) {
        if ("INFO".equals(d.type)) return "Systémová informace se odstraní při dalším skenu.";
        if (d.demo) return "Demo položka se nemaže podle dosahu.";
        long age = Math.max(0, System.currentTimeMillis() - d.lastSeenAt);
        if (d.inRange) return "Zobrazuje se jen dokud chodí čerstvý signál. Bez dalšího zachycení se schová po " + seconds(staleMsFor(d)) + " s.";
        long removeIn = Math.max(0, removeMsFor(d) - age);
        if (d.key.equals(trackingKey)) return "Detail je otevřený, proto nechávám poslední data pro live dohledávání. V seznamu je skryté, dokud se signál nevrátí.";
        if (removeIn > 0) return "Mimo dosah – v seznamu je skryté a bude smazané asi za " + seconds(removeIn) + " s, pokud se znovu neobjeví.";
        return "Mimo dosah – připravené k automatickému odstranění.";
    }

    private String ageText(long timestamp) {
        long age = Math.max(0, System.currentTimeMillis() - timestamp);
        if (age < 1500) return "teď";
        if (age < 60000) return "před " + seconds(age) + " s";
        long minutes = age / 60000;
        if (minutes < 60) return "před " + minutes + " min";
        long hours = minutes / 60;
        return "před " + hours + " h";
    }

    private long seconds(long millis) {
        return Math.max(1, Math.round(millis / 1000.0));
    }

    private String allKnownData(DeviceInfo d) {
        StringBuilder out = new StringBuilder();
        appendKnown(out, "Název", d.name);
        appendKnown(out, "Typ", typeLabel(d.type));
        appendKnown(out, "Transport", d.transport);
        appendKnown(out, "Zdroj", d.source);
        appendKnown(out, "Stav v dosahu", d.inRange ? "Ano" : "Ne");
        appendKnown(out, "První zachycení", ageText(d.firstSeenAt));
        appendKnown(out, "Naposledy zachyceno", ageText(d.lastSeenAt));
        appendKnown(out, "Automatické mazání", autoDeleteLabel(d));
        appendKnown(out, "Adresa", d.address);
        appendKnown(out, "Typ adresy", d.addressType);
        appendKnown(out, "Výrobce", d.vendor);
        appendKnown(out, "Model / odhad", d.model);
        appendKnown(out, "Kategorie", d.deviceClass);
        appendKnown(out, "RSSI", d.rssi == 0 ? "" : d.rssi + " dBm");
        appendKnown(out, "Filtrované RSSI", Double.isNaN(d.filteredRssi) ? "" : String.format(Locale.forLanguageTag("cs-CZ"), "%.1f dBm", d.filteredRssi));
        appendKnown(out, "Vzorky", d.sampleCount <= 0 ? "" : String.valueOf(d.sampleCount));
        appendKnown(out, "Vzdálenost", d.distance < 0 ? "" : formatDistance(d.distance));
        appendKnown(out, "Přesnost vzdálenosti", distanceQuality(d));
        appendKnown(out, "Signálová poznámka", signalHint(d));
        appendKnown(out, "Možné typy", possibleDeviceTypes(d));
        appendKnown(out, "TX Power", d.txPower == Integer.MIN_VALUE ? "" : d.txPower + " dBm");
        appendKnown(out, "Frekvence", d.frequency);
        appendKnown(out, "Zabezpečení", d.security);
        appendKnown(out, "BLE název", d.decodedName);
        appendKnown(out, "BLE appearance", d.appearance);
        appendKnown(out, "BLE výrobce", d.manufacturerIds);
        appendKnown(out, "Služby", d.services);
        appendKnown(out, "Dekódovaná reklama", d.decodedData);
        appendKnown(out, "Protokolová data", d.protocolData);
        appendKnown(out, "Detail", d.detail);
        appendKnown(out, "Důvod", d.reason);
        appendKnown(out, "Demo", d.demo ? "Ano - interní testovací položka" : "");
        appendKnown(out, "Sledované", watchedKeys.contains(d.key) ? "Ano" : "Ne");
        appendKnown(out, "Dosah", "BLUETOOTH".equals(d.transport) ? bluetoothPresenceLabel(d) : "");
        appendKnown(out, "Stav v posledním skenu", !"BLUETOOTH".equals(d.transport) ? genericPresenceLabel(d) : "");
        return out.toString().trim();
    }

    private void appendKnown(StringBuilder out, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        out.append(label).append(": ").append(value.trim()).append('\n');
    }

    private void showEmptyState() {
        if (deviceList == null || deviceList.getChildCount() > 0) return;
        TextView empty = label(scanning ? "Hledám zařízení v okolí…" : "Spusť sken a povol požadovaná oprávnění.", 14, MUTED, Typeface.NORMAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(20), dp(70), dp(20), dp(70));
        deviceList.addView(empty, new LinearLayout.LayoutParams(-1, -2));
    }

    private void setStatus(String text) {
        handler.post(() -> statusView.setText(text));
    }

    private void stopRadios() {
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) bluetoothAdapter.cancelDiscovery();
            if (bluetoothLeScanner != null && bleCallback != null) bluetoothLeScanner.stopScan(bleCallback);
        } catch (SecurityException ignored) { }
        bleCallback = null;
    }

    @Override public void onBackPressed() {
        if (drawer != null && drawer.getVisibility() == View.VISIBLE) closeDrawer();
        else super.onBackPressed();
    }

    @Override protected void onResume() {
        super.onResume();
        appVisible = true;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        backgroundScanEnabled = prefs.getBoolean(PREF_BACKGROUND_SCAN, true);
        prefs.edit().putBoolean(PREF_APP_VISIBLE, true).apply();
        updateBackgroundServiceState();
        if (!scanning) {
            startBluetoothPresenceWatch();
            resumeTargetTrackingRadios();
        }
    }

    @Override protected void onPause() {
        appVisible = false;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREF_APP_VISIBLE, false).apply();
        if (!scanning) {
            stopBluetoothPresenceWatch();
            suspendTargetTrackingRadios();
        }
        updateBackgroundServiceState();
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        stopGnssMonitor();
        stopTargetTracking();
        stopBluetoothPresenceWatch();
        stopRadios();
        executor.shutdownNow();
        if (receiverRegistered) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) { }
        }
        if (backgroundReceiverRegistered) {
            try { unregisterReceiver(backgroundResultReceiver); } catch (Exception ignored) { }
        }
        if (mdnsScanner != null) mdnsScanner.stop();
        super.onDestroy();
    }

    private double distanceFromRssi(int rssi, int txPower, double factor) {
        return Math.pow(10.0, (txPower - rssi) / (10.0 * factor));
    }

    private double wifiDistance(int rssi, int frequencyMhz) {
        if (frequencyMhz <= 0 || rssi == 0) return -1;
        return Math.pow(10.0, (27.55 - 20.0 * Math.log10(frequencyMhz) + Math.abs(rssi)) / 20.0);
    }

    private String formatDistance(double meters) {
        if (meters < 0.1) return "< 0,1 m";
        if (meters < 10) return String.format(Locale.forLanguageTag("cs-CZ"), "≈ %.1f m", meters);
        return String.format(Locale.forLanguageTag("cs-CZ"), "≈ %.0f m", meters);
    }

    private String bluetoothPresenceLabel(DeviceInfo d) {
        long age = Math.max(0, System.currentTimeMillis() - d.lastSeenAt);
        if (age <= BLUETOOTH_STALE_MS) return "v dosahu • naposledy teď";
        if (age < BLUETOOTH_REMOVE_MS) return "mimo dosah • skryto ze seznamu, čekám na nový signál";
        return "mimo dosah";
    }

    private String genericPresenceLabel(DeviceInfo d) {
        long age = Math.max(0, System.currentTimeMillis() - d.lastSeenAt);
        if (d.inRange || age < RADIO_STALE_MS) return "v dosahu • potvrzeno posledním skenem";
        if (watchedKeys.contains(d.key)) return "hlídané • skryto, čekám na znovuobjevení";
        return "mimo dosah • automaticky schováno";
    }

    private String signalBars(int rssi) {
        if (rssi >= -55) return "▰▰▰▰";
        if (rssi >= -67) return "▰▰▰▱";
        if (rssi >= -78) return "▰▰▱▱";
        return "▰▱▱▱";
    }

    private String typeLabel(String type) {
        switch (type) {
            case "TRACKER": return "MOŽNÝ TRACKER";
            case "WEARABLE": return "HODINKY / NÁRAMEK";
            case "CAMERA": return "MOŽNÁ KAMERA";
            case "AUDIO": return "ZVUKOVÉ ZAŘÍZENÍ";
            case "SCOOTER": return "KOLOBĚŽKA / VOZIDLO";
            case "SMART_HOME": return "CHYTRÁ DOMÁCNOST";
            case "IOT": return "IOT / MALÝ MODUL";
            case "COMPUTER": return "POČÍTAČ / NAS";
            case "PHONE": return "TELEFON / TABLET";
            case "WIFI": return "WI‑FI SÍŤ";
            case "BLUETOOTH": return "BLUETOOTH";
            case "NETWORK": return "SÍŤOVÉ ZAŘÍZENÍ";
            case "SPY": return "SPY / SLEDOVÁNÍ";
            case "WATCHED": return "SLEDOVANÉ";
            case "SETTINGS": return "NASTAVENÍ";
            default: return "INFORMACE";
        }
    }

    private int colorForType(String type) {
        switch (type) {
            case "TRACKER": return TRACKER;
            case "WEARABLE": return WEARABLE;
            case "CAMERA": return CAMERA;
            case "AUDIO": return AUDIO;
            case "SCOOTER": return VEHICLE;
            case "SMART_HOME": return SMART_HOME;
            case "IOT": return IOT;
            case "COMPUTER": return COMPUTER;
            case "PHONE": return PHONE;
            case "HIDDEN": return TRACKER;
            case "WATCHED": return TRACKER;
            case "SPY": return CAMERA;
            case "MI_BAND":
            case "APPLE":
            case "SAMSUNG":
            case "GARMIN":
            case "FITBIT":
            case "AMAZFIT":
            case "HUAWEI":
            case "ASUS":
            case "GOOGLE":
            case "SONY":
            case "MOTOROLA":
            case "BBK":
            case "NOKIA":
            case "NOTHING":
            case "OTHER_WEARABLE": return WEARABLE;
            case "AUDIO_BRANDS": return AUDIO;
            case "RADAR": return ACCENT;
            case "SECURITY": return ACCENT;
            case "SETTINGS": return ACCENT;
            case "GNSS": return ACCENT;
            case "WIFI": return WIFI;
            case "BLUETOOTH": return BLUETOOTH;
            default: return ACCENT;
        }
    }

    private TextView label(String text, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        String clean = value.replaceAll("\\s+", " ").trim();
        return clean.length() <= max ? clean : clean.substring(0, max - 1) + "…";
    }

    private static class Classification {
        final String type;
        final int confidence;
        final String reason;
        Classification(String type, int confidence, String reason) {
            this.type = type;
            this.confidence = confidence;
            this.reason = reason;
        }
    }

    private static class DeviceInfo {
        final String key;
        String name;
        final String address;
        String source;
        String type;
        int rssi;
        double distance;
        int confidence;
        String reason;
        String detail;
        String vendor = "";
        String model = "";
        String deviceClass = "";
        String services = "";
        String manufacturerIds = "";
        String appearance = "";
        String addressType = "";
        String rawAdvertisement = "";
        String decodedName = "";
        String decodedData = "";
        String frequency = "";
        String security = "";
        String protocolData = "";
        String transport = "";
        boolean hiddenName;
        boolean inRange;
        boolean signalConfirmed;
        boolean demo;
        int txPower = Integer.MIN_VALUE;
        long firstSeenAt;
        long lastSeenAt;
        double filteredRssi = Double.NaN;
        double filteredDistance = -1;
        int sampleCount;
        DeviceInfo(String key, String name, String address, String source, String type, int rssi,
                   double distance, int confidence, String reason, String detail) {
            this.key = key;
            this.name = name;
            this.address = address;
            this.source = source;
            this.type = type;
            this.rssi = rssi;
            this.distance = distance;
            this.confidence = confidence;
            this.reason = reason;
            this.detail = detail == null ? "" : detail;
            this.lastSeenAt = System.currentTimeMillis();
            this.firstSeenAt = this.lastSeenAt;
            this.inRange = true;
            this.signalConfirmed = true;
            if (rssi != 0) {
                this.filteredRssi = rssi;
                this.sampleCount = 1;
            }
            this.filteredDistance = distance;
        }
    }

    private static class GnssSatelliteInfo {
        final int constellation;
        final int svid;
        final float cn0;
        final float basebandCn0;
        final float elevation;
        final float azimuth;
        final float carrierMhz;
        final boolean usedInFix;
        final boolean hasAlmanac;
        final boolean hasEphemeris;

        GnssSatelliteInfo(int constellation, int svid, float cn0, float basebandCn0,
                          float elevation, float azimuth, float carrierMhz, boolean usedInFix,
                          boolean hasAlmanac, boolean hasEphemeris) {
            this.constellation = constellation;
            this.svid = svid;
            this.cn0 = cn0;
            this.basebandCn0 = basebandCn0;
            this.elevation = elevation;
            this.azimuth = azimuth;
            this.carrierMhz = carrierMhz;
            this.usedInFix = usedInFix;
            this.hasAlmanac = hasAlmanac;
            this.hasEphemeris = hasEphemeris;
        }
    }
}
