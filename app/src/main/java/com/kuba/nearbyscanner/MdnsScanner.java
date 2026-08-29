package com.kuba.nearbyscanner;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

final class MdnsScanner {
    interface Callback {
        void onService(String name, String type, String host, int port);
        void onInfo(String message);
    }

    private static final String[] TYPES = {
            "_http._tcp.", "_https._tcp.", "_rtsp._tcp.", "_googlecast._tcp.",
            "_airplay._tcp.", "_raop._tcp.", "_ipp._tcp.", "_printer._tcp.",
            "_hap._tcp.", "_matter._tcp.", "_workstation._tcp.", "_smb._tcp."
    };

    private final NsdManager manager;
    private final Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<NsdManager.DiscoveryListener> listeners = new ArrayList<>();
    private boolean running;

    MdnsScanner(Context context, Callback callback) {
        this.manager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.callback = callback;
    }

    void start(long durationMs) {
        stop();
        if (manager == null) {
            callback.onInfo("mDNS sken není v telefonu dostupný");
            return;
        }
        running = true;
        for (String type : TYPES) discover(type);
        handler.postDelayed(this::stop, durationMs);
    }

    void stop() {
        handler.removeCallbacksAndMessages(null);
        if (manager != null) {
            for (NsdManager.DiscoveryListener listener : new ArrayList<>(listeners)) {
                try { manager.stopServiceDiscovery(listener); } catch (Exception ignored) { }
            }
        }
        listeners.clear();
        running = false;
    }

    private void discover(final String wantedType) {
        NsdManager.DiscoveryListener listener = new NsdManager.DiscoveryListener() {
            @Override public void onDiscoveryStarted(String type) { }
            @Override public void onDiscoveryStopped(String type) { listeners.remove(this); }
            @Override public void onStartDiscoveryFailed(String type, int code) {
                listeners.remove(this);
                try { manager.stopServiceDiscovery(this); } catch (Exception ignored) { }
            }
            @Override public void onStopDiscoveryFailed(String type, int code) { listeners.remove(this); }
            @Override public void onServiceLost(NsdServiceInfo service) { }
            @Override public void onServiceFound(NsdServiceInfo service) {
                if (!running || service == null) return;
                resolve(service, wantedType);
            }
        };
        listeners.add(listener);
        try {
            manager.discoverServices(wantedType, NsdManager.PROTOCOL_DNS_SD, listener);
        } catch (Exception ex) {
            listeners.remove(listener);
        }
    }

    @SuppressWarnings("deprecation")
    private void resolve(final NsdServiceInfo found, final String fallbackType) {
        try {
            manager.resolveService(found, new NsdManager.ResolveListener() {
                @Override public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    callback.onService(found.getServiceName(), fallbackType, "", 0);
                }

                @Override public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    InetAddress address = serviceInfo.getHost();
                    String host = address == null ? "" : address.getHostAddress();
                    String type = serviceInfo.getServiceType() == null ? fallbackType : serviceInfo.getServiceType();
                    callback.onService(serviceInfo.getServiceName(), type, host, serviceInfo.getPort());
                }
            });
        } catch (Exception ex) {
            callback.onService(found.getServiceName(), fallbackType, "", 0);
        }
    }
}
