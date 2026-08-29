package com.kuba.nearbyscanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class DeviceSecurityScanner {
    private long previousTx = -1;
    private long previousRx = -1;
    private long previousTime = -1;

    static final class Result {
        String device = "";
        String network = "";
        String traffic = "";
        String protection = "";
        String limitation = "";
        int userApps;
        int systemApps;
        int riskCount;
        final List<String> findings = new ArrayList<>();
    }

    Result scan(Context context) {
        Result out = new Result();
        out.device = Build.MANUFACTURER + " " + Build.MODEL + " • Android " + Build.VERSION.RELEASE
                + " • bezpečnostní aktualizace " + safe(Build.VERSION.SECURITY_PATCH, "neuvedena")
                + "\nBuild: " + Build.DISPLAY;
        scanNetwork(context, out);
        scanTraffic(out);
        scanSettings(context, out);
        scanApps(context, out);
        out.limitation = "Android bez rootu/VPN nedovolí této aplikaci číst obsah cizí komunikace ani spolehlivě přiřadit celkový provoz jednotlivým aplikacím. Zobrazuje proto skutečné souhrnné počty bajtů, síťová nastavení a riziková oprávnění – ne důkaz špehování.";
        return out;
    }

    private void scanNetwork(Context context, Result out) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            out.network = "Síťové informace nejsou dostupné";
            return;
        }
        Network network = cm.getActiveNetwork();
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        List<String> parts = new ArrayList<>();
        if (caps == null) parts.add("Bez aktivního připojení");
        else {
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) parts.add("Wi‑Fi");
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) parts.add("mobilní data");
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) parts.add("Ethernet");
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) parts.add("VPN aktivní");
            else parts.add("VPN není aktivní");
            parts.add(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ? "internet ověřen" : "internet neověřen");
            parts.add(cm.isActiveNetworkMetered() ? "účtované připojení" : "neúčtované připojení");
        }
        LinkProperties lp = cm.getLinkProperties(network);
        if (Build.VERSION.SDK_INT >= 28 && lp != null) {
            if (lp.isPrivateDnsActive()) parts.add("Privátní DNS: " + safe(lp.getPrivateDnsServerName(), "aktivní"));
            else parts.add("Privátní DNS není aktivní");
        }
        out.network = join(parts, " • ");
    }

    private void scanTraffic(Result out) {
        long tx = TrafficStats.getTotalTxBytes();
        long rx = TrafficStats.getTotalRxBytes();
        long now = System.currentTimeMillis();
        String delta = "První měření – obnov kontrolu pro změnu v čase";
        if (previousTime > 0 && tx >= 0 && rx >= 0) {
            long seconds = Math.max(1, (now - previousTime) / 1000);
            delta = "+" + bytes(Math.max(0, tx - previousTx)) + " odesláno, +"
                    + bytes(Math.max(0, rx - previousRx)) + " přijato za " + seconds + " s";
        }
        out.traffic = "Odesláno od startu telefonu: " + bytes(tx) + "\nPřijato od startu telefonu: "
                + bytes(rx) + "\n" + delta;
        previousTx = tx;
        previousRx = rx;
        previousTime = now;
    }

    private void scanSettings(Context context, Result out) {
        List<String> protection = new ArrayList<>();
        try {
            int adb = Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0);
            protection.add(adb == 0 ? "USB ladění vypnuto" : "USB ladění zapnuto");
            if (adb != 0) out.findings.add("USB ladění je zapnuté – cizímu počítači nepovoluj ladicí přístup.");
        } catch (Exception ignored) { }
        try {
            int dev = Settings.Global.getInt(context.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
            protection.add(dev == 0 ? "Vývojářské volby vypnuté" : "Vývojářské volby zapnuté");
        } catch (Exception ignored) { }
        try {
            String accessibility = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (!TextUtils.isEmpty(accessibility)) {
                out.findings.add("Aktivní služby Usnadnění: " + shorten(accessibility.replace(':', ' '), 140));
                protection.add("Usnadnění: aktivní služba");
            } else protection.add("Usnadnění: bez aktivní služby");
        } catch (Exception ignored) { }
        try {
            String listeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
            if (!TextUtils.isEmpty(listeners)) {
                out.findings.add("Aplikace s přístupem k oznámením: " + shorten(listeners.replace(':', ' '), 140));
                protection.add("Přístup k oznámením: aktivní");
            } else protection.add("Přístup k oznámením: bez aktivní aplikace");
        } catch (Exception ignored) { }
        out.protection = join(protection, " • ");
    }

    @SuppressWarnings("deprecation")
    private void scanApps(Context context, Result out) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages;
        if (Build.VERSION.SDK_INT >= 33) packages = pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS));
        else packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        List<String> appFindings = new ArrayList<>();
        for (PackageInfo info : packages) {
            ApplicationInfo ai = info.applicationInfo;
            boolean system = ai != null && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            if (system) out.systemApps++; else out.userApps++;
            if (system || info.packageName.equals(context.getPackageName())) continue;
            List<String> sensitive = new ArrayList<>();
            addPermission(pm, info, Manifest.permission.RECORD_AUDIO, "mikrofon", sensitive);
            addPermission(pm, info, Manifest.permission.CAMERA, "kamera", sensitive);
            addPermission(pm, info, Manifest.permission.ACCESS_FINE_LOCATION, "přesná poloha", sensitive);
            addPermission(pm, info, Manifest.permission.REQUEST_INSTALL_PACKAGES, "instalace APK", sensitive);
            if (sensitive.size() >= 2) {
                String name = ai == null ? info.packageName : String.valueOf(pm.getApplicationLabel(ai));
                appFindings.add(name + " (" + info.packageName + "): " + join(sensitive, ", "));
            }
        }
        Collections.sort(appFindings, Comparator.naturalOrder());
        int max = Math.min(16, appFindings.size());
        for (int i = 0; i < max; i++) out.findings.add("Citlivá oprávnění • " + appFindings.get(i));
        if (appFindings.size() > max) out.findings.add("… a " + (appFindings.size() - max) + " dalších aplikací s více citlivými oprávněními");
        out.riskCount = out.findings.size();
        if (out.findings.isEmpty()) out.findings.add("Kontrola nenašla zjevné rizikové nastavení. To samo o sobě nevylučuje škodlivou aplikaci.");
    }

    private void addPermission(PackageManager pm, PackageInfo info, String permission, String label, List<String> out) {
        if (pm.checkPermission(permission, info.packageName) == PackageManager.PERMISSION_GRANTED) out.add(label);
    }

    private String bytes(long value) {
        if (value < 0) return "nedostupné";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double number = value;
        int unit = 0;
        while (number >= 1024 && unit < units.length - 1) { number /= 1024; unit++; }
        return String.format(Locale.forLanguageTag("cs-CZ"), unit == 0 ? "%.0f %s" : "%.1f %s", number, units[unit]);
    }

    private String safe(String value, String fallback) { return TextUtils.isEmpty(value) ? fallback : value; }
    private String shorten(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    private String join(List<String> values, String separator) {
        StringBuilder out = new StringBuilder();
        for (String value : values) { if (out.length() > 0) out.append(separator); out.append(value); }
        return out.toString();
    }
}
