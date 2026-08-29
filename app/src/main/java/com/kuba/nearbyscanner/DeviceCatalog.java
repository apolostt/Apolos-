package com.kuba.nearbyscanner;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class DeviceCatalog {
    private final Map<String, String> oui = new HashMap<>(45000);
    private final Map<Integer, String> companies = new HashMap<>(4500);
    private final Map<Integer, String> services = new HashMap<>(1000);
    private final Map<Integer, String> appearances = new HashMap<>(350);
    private final List<NameSignature> nameSignatures = new ArrayList<>();
    private final List<NameSignature> profileSignatures = new ArrayList<>();

    static DeviceCatalog load(Context context) {
        DeviceCatalog catalog = new DeviceCatalog();
        catalog.readStringMap(context, "ieee_oui.tsv", catalog.oui);
        catalog.readIntMap(context, "bt_companies.tsv", catalog.companies);
        catalog.readIntMap(context, "bt_services.tsv", catalog.services);
        catalog.readIntMap(context, "bt_appearances.tsv", catalog.appearances);
        catalog.readNameSignatures(context, "device_name_signatures.tsv");
        catalog.readProfileSignatures(context, "device_profile_signatures.tsv");
        catalog.readOnlineMap(OnlineCatalogUpdater.catalogFile(context));
        return catalog;
    }

    private void readOnlineMap(File file) {
        if (file == null || !file.isFile()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab > 0) oui.put(line.substring(0, tab), line.substring(tab + 1));
            }
        } catch (Exception ignored) { }
    }

    private void readStringMap(Context context, String asset, Map<String, String> target) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(asset), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab > 0) target.put(line.substring(0, tab), line.substring(tab + 1));
            }
        } catch (Exception ignored) { }
    }

    private void readIntMap(Context context, String asset, Map<Integer, String> target) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(asset), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab > 0) target.put(Integer.parseInt(line.substring(0, tab), 16), line.substring(tab + 1));
            }
        } catch (Exception ignored) { }
    }

    private void readNameSignatures(Context context, String asset) {
        readSignatures(context, asset, nameSignatures);
    }

    private void readProfileSignatures(Context context, String asset) {
        readSignatures(context, asset, profileSignatures);
    }

    private void readSignatures(Context context, String asset, List<NameSignature> target) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(asset), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length < 6) continue;
                try {
                    target.add(new NameSignature(parts[0], parts[1], parts[2], parts[3],
                            Integer.parseInt(parts[4]), parts[5]));
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
    }

    String vendorFromMac(String address) {
        String normalized = normalizeMac(address);
        if (normalized.length() < 6 || isPrivateMac(normalized)) return "";
        String value = normalized.length() >= 9 ? oui.get(normalized.substring(0, 9)) : null;
        if (value == null && normalized.length() >= 7) value = oui.get(normalized.substring(0, 7));
        if (value == null) value = oui.get(normalized.substring(0, 6));
        return value == null ? "" : value;
    }

    String macAddressNote(String address) {
        String normalized = normalizeMac(address);
        if (normalized.length() < 2) return "Neurčený typ adresy";
        return isPrivateMac(normalized)
                ? "Soukromá/náhodná MAC – výrobce z OUI nemusí jít zjistit"
                : "Veřejně přidělená MAC – výrobce ověřen přes IEEE OUI";
    }

    BluetoothIdentity identifyBluetooth(BluetoothDevice device, BluetoothClass bluetoothClass,
                                        ScanRecord record, String advertisedName) {
        BluetoothIdentity out = new BluetoothIdentity();
        out.deviceClass = classLabel(bluetoothClass);
        out.addressType = macAddressNote(device.getAddress()) + " • " + bluetoothTransport(device.getType());
        out.vendor = vendorFromMac(device.getAddress());

        if (record != null) {
            SparseArray<byte[]> data = record.getManufacturerSpecificData();
            List<String> makers = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                int id = data.keyAt(i);
                String company = companies.get(id);
                makers.add(String.format(Locale.ROOT, "0x%04X – %s", id,
                        company == null ? "neznámý Bluetooth SIG identifikátor" : company));
                if (out.vendor.isEmpty() && company != null) out.vendor = company;
            }
            out.manufacturerIds = join(makers);
            out.services = serviceLabels(record.getServiceUuids());
            out.appearance = appearanceLabel(record.getBytes());
            out.rawAdvertisement = hex(record.getBytes(), 96);
            out.decodedName = decodedNames(record.getBytes());
            out.decodedData = decodedAdvertisingSummary(record.getBytes());
            out.txPower = record.getTxPowerLevel();

            TrackerMatch tracker = trackerMatch(advertisedName, record);
            out.trackerName = tracker.name;
            out.trackerConfidence = tracker.confidence;
            out.trackerReason = tracker.reason;
        } else {
            TrackerMatch tracker = trackerMatch(advertisedName, null);
            out.trackerName = tracker.name;
            out.trackerConfidence = tracker.confidence;
            out.trackerReason = tracker.reason;
        }

        applyNameSignature(advertisedName, out);
        applyNameSignature(out.decodedName, out);
        applyCategoryHint(out);
        applyProfileSignature(out);
        out.modelHint = modelHint(advertisedName, out);
        return out;
    }

    private String modelHint(String name, BluetoothIdentity identity) {
        String cleanName = name == null ? "" : name.trim();
        if (!identity.trackerName.isEmpty()) return identity.trackerName;
        if (!identity.modelHint.isEmpty()) return identity.modelHint;
        if (!cleanName.isEmpty() && !cleanName.toLowerCase(Locale.ROOT).startsWith("neznámé")) return cleanName;
        String module = modelHintForVendor(identity.vendor, "");
        if (!module.isEmpty()) return module;
        if (!identity.appearance.isEmpty() && !identity.vendor.isEmpty()) return identity.appearance + " • " + identity.vendor;
        if (!identity.deviceClass.isEmpty() && !identity.vendor.isEmpty()) return identity.deviceClass + " • " + identity.vendor;
        if (!identity.appearance.isEmpty()) return identity.appearance;
        if (!identity.deviceClass.isEmpty()) return identity.deviceClass;
        if (!identity.vendor.isEmpty()) return "Zařízení výrobce " + identity.vendor;
        return "Přesný model zařízení nevysílá";
    }

    String modelHintForVendor(String vendor, String fallback) {
        String value = vendor == null ? "" : vendor.toLowerCase(Locale.ROOT);
        if (containsAny(value, "raspberry pi")) return "Raspberry Pi / malý embedded Linux počítač";
        if (containsAny(value, "espressif")) return "ESP32 / ESP8266 IoT Wi‑Fi nebo Bluetooth modul";
        if (containsAny(value, "nordic semiconductor")) return "Nordic nRF BLE modul pro senzor, tracker nebo IoT";
        if (containsAny(value, "arduino")) return "Arduino / embedded řídicí modul";
        if (containsAny(value, "tuya", "hangzhou tuya")) return "Tuya / Smart Life IoT modul";
        if (containsAny(value, "shelly", "aqara", "sonoff", "ewelink", "meross", "nanoleaf", "switchbot", "govee", "philips lighting", "signify", "ikea", "wiz connected")) return "Chytrá domácnost / světlo / zásuvka / senzor • " + vendor;
        if (containsAny(value, "hikvision", "dahua", "axis communications", "reolink", "uniview", "ezviz", "imou", "tp-link", "vivotek", "ubiquiti", "arlo", "wyze")) return "Síťový video / kamerový modul výrobce " + vendor;
        if (containsAny(value, "ambarella", "hisilicon", "sigmastar", "ingenic", "goke", "xiongmai", "novatek", "fullhan")) return "Čipová platforma používaná v embedded videu a kamerách • " + vendor;
        if (containsAny(value, "sonos", "bose", "harman", "jbl", "shure", "sennheiser", "jabra", "anker", "soundcore", "skullcandy", "edifier", "sony")) return "Síťové nebo Bluetooth zvukové zařízení • " + vendor;
        if (containsAny(value, "ninebot", "segway", "xiaomi communications", "niu", "bafang", "bosch ebike")) return "Elektrokoloběžka, e-bike nebo dopravní BLE/Wi‑Fi modul • " + vendor;
        if (containsAny(value, "apple")) return "Apple zařízení / hodinky, sluchátka, tag nebo telefon";
        if (containsAny(value, "samsung")) return "Samsung zařízení / Galaxy Watch, Buds, SmartTag nebo telefon";
        if (containsAny(value, "google", "pixel")) return "Google Pixel, Nest, Fitbit nebo Fast Pair zařízení";
        if (containsAny(value, "motorola")) return "Motorola telefon, headset nebo komunikační zařízení";
        if (containsAny(value, "hmd global", "nokia")) return "Nokia/HMD telefon, tablet nebo příslušenství";
        if (containsAny(value, "nothing technology", "cmf")) return "Nothing/CMF telefon, hodinky nebo audio";
        if (containsAny(value, "zte", "nubia")) return "ZTE/Nubia telefon nebo síťové zařízení";
        if (containsAny(value, "transsion", "tecno", "infinix", "itel")) return "Tecno/Infinix/Itel telefon nebo příslušenství";
        if (containsAny(value, "huawei", "honor")) return "Huawei/Honor telefon, hodinky, náramek nebo audio";
        if (containsAny(value, "oppo", "oneplus", "realme", "vivo", "bbk")) return "Oppo/OnePlus/Realme/Vivo telefon, hodinky nebo audio";
        if (containsAny(value, "garmin")) return "Garmin hodinky, cyklo počítač nebo senzor";
        if (containsAny(value, "fitbit")) return "Fitbit hodinky nebo fitness náramek";
        if (containsAny(value, "huami", "amazfit", "zepp")) return "Amazfit / Zepp hodinky nebo fitness náramek";
        if (containsAny(value, "xiaomi", "huami")) return "Xiaomi / Mi Band / chytré zařízení";
        if (containsAny(value, "asustek", "asus")) return "Asus/ROG telefon, notebook, router, periferie nebo audio";
        if (containsAny(value, "lenovo", "hp inc", "dell", "acer", "micro-star", "msi", "intel corporate")) return "Počítač, notebook nebo síťový modul • " + vendor;
        if (containsAny(value, "synology", "qnap", "western digital", "seagate")) return "NAS nebo úložiště v lokální síti • " + vendor;
        if (containsAny(value, "realtek", "broadcom", "mediatek", "qualcomm", "atheros", "lite-on", "azurewave")) return "Komunikační modul / čipset • " + vendor;
        return fallback == null ? "" : fallback;
    }

    private void applyNameSignature(String name, BluetoothIdentity out) {
        String value = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return;
        for (NameSignature signature : nameSignatures) {
            if (!signature.matches(value)) continue;
            if (out.vendor.isEmpty()) out.vendor = signature.brand;
            if (out.modelHint.isEmpty()) out.modelHint = signature.model;
            if (out.categoryHint.isEmpty() || signature.confidence > out.categoryConfidence) {
                out.categoryHint = signature.category;
                out.categoryConfidence = signature.confidence;
                out.categoryReason = signature.reason;
            }
            return;
        }
    }

    private void applyCategoryHint(BluetoothIdentity out) {
        String evidence = out.searchableText().toLowerCase(Locale.ROOT);
        if (containsAny(evidence, "smartwatch", "sports watch", "wearable computer", "watch size", "heart rate", "running speed", "cycling speed", "cycling power", "pulse oximeter", "body composition", "weight scale")) {
            setCategory(out, "WEARABLE", 88, "Bluetooth služba nebo appearance odpovídá hodinkám/náramku/senzoru");
        } else if (containsAny(evidence, "earbud", "headset", "headphones", "microphone control", "audio stream control", "media control", "broadcast audio", "volume control")) {
            setCategory(out, "AUDIO", 88, "Bluetooth služba nebo appearance odpovídá audio zařízení");
        } else if (containsAny(evidence, "matter", "homekit", "hap", "tuya", "smart life", "shelly", "aqara", "sonoff", "occupancy sensor", "contact sensor", "smoke sensor", "leak sensor", "temperature sensor", "humidity sensor")) {
            setCategory(out, "SMART_HOME", 84, "Bluetooth/mDNS služba nebo výrobce odpovídá chytré domácnosti");
        } else if (containsAny(evidence, "motion sensor", "nordic", "espressif", "arduino", "embedded")) {
            setCategory(out, "IOT", 82, "Bluetooth služba, appearance nebo výrobce odpovídá IoT/senzoru");
        }
    }

    private void applyProfileSignature(BluetoothIdentity out) {
        String evidence = out.searchableText().toLowerCase(Locale.ROOT);
        for (NameSignature signature : profileSignatures) {
            if (!signature.matches(evidence)) continue;
            if (out.vendor.isEmpty()) out.vendor = signature.brand;
            if (out.modelHint.isEmpty() || out.modelHint.startsWith("Zařízení výrobce") || out.modelHint.startsWith("Přesný model")) {
                out.modelHint = signature.model;
            }
            setCategory(out, signature.category, signature.confidence, signature.reason);
            return;
        }
    }

    private void setCategory(BluetoothIdentity out, String category, int confidence, String reason) {
        if (confidence <= out.categoryConfidence) return;
        out.categoryHint = category;
        out.categoryConfidence = confidence;
        out.categoryReason = reason;
    }

    private TrackerMatch trackerMatch(String name, ScanRecord record) {
        String value = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (containsAny(value, "airtag")) return new TrackerMatch("Apple AirTag", 99, "Název AirTag ve Bluetooth vysílání");
        if (containsAny(value, "smarttag", "smart tag")) return new TrackerMatch("Samsung Galaxy SmartTag", 99, "Název SmartTag ve Bluetooth vysílání");
        if (containsAny(value, "tile mate", "tile pro", "tile slim", "tile sticker", "tile tracker")) return new TrackerMatch("Tile tracker", 98, "Název trackeru Tile");
        if (containsAny(value, "chipolo")) return new TrackerMatch("Chipolo tracker", 98, "Název trackeru Chipolo");
        if (containsAny(value, "pebblebee")) return new TrackerMatch("Pebblebee tracker", 98, "Název trackeru Pebblebee");
        if (containsAny(value, "smarttrack", "smart track")) return new TrackerMatch("Eufy SmartTrack", 96, "Název SmartTrack ve vysílání");
        if (containsAny(value, "cube tracker", "nutale", "nut tracker")) return new TrackerMatch("Bluetooth tracker", 92, "Známý název sledovacího přívěsku");

        if (record != null) {
            for (ParcelUuid uuid : safe(record.getServiceUuids())) {
                int shortUuid = shortUuid(uuid);
                if (shortUuid == 0xFEED || shortUuid == 0xFEEC)
                    return new TrackerMatch("Tile tracker / služba Tile", 94, "Bluetooth SIG služba 0x" + Integer.toHexString(shortUuid).toUpperCase(Locale.ROOT));
                if (shortUuid == 0xFD5A)
                    return new TrackerMatch("Možný Samsung SmartTag", 68, "Samsung služba 0xFD5A – může ji používat i jiné zařízení Samsung");
            }
            byte[] apple = record.getManufacturerSpecificData(0x004C);
            if (apple != null && apple.length >= 20 && (apple[0] & 0xFF) == 0x12)
                return new TrackerMatch("Možné Apple Find My zařízení", 82, "Apple Offline Finding typ 0x12 v reklamních datech");
        }
        return new TrackerMatch("", 0, "");
    }

    private String serviceLabels(List<ParcelUuid> uuids) {
        List<String> labels = new ArrayList<>();
        for (ParcelUuid uuid : safe(uuids)) {
            int value = shortUuid(uuid);
            if (value >= 0) {
                String name = services.get(value);
                labels.add(String.format(Locale.ROOT, "0x%04X%s", value, name == null ? "" : " – " + name));
            } else labels.add(uuid.toString());
        }
        return join(labels);
    }

    private int shortUuid(ParcelUuid parcelUuid) {
        if (parcelUuid == null) return -1;
        String uuid = parcelUuid.toString().toUpperCase(Locale.ROOT);
        if (uuid.startsWith("0000") && uuid.endsWith("-0000-1000-8000-00805F9B34FB")) {
            try { return Integer.parseInt(uuid.substring(4, 8), 16); } catch (Exception ignored) { }
        }
        return -1;
    }

    private String appearanceLabel(byte[] raw) {
        int appearance = findAppearance(raw);
        if (appearance < 0) return "";
        String exact = appearances.get(appearance);
        if (exact != null) return exact + String.format(Locale.ROOT, " (0x%04X)", appearance);
        String category = appearances.get((appearance >> 6) << 6);
        return (category == null ? "Neznámý typ" : category) + String.format(Locale.ROOT, " (0x%04X)", appearance);
    }

    private int findAppearance(byte[] raw) {
        if (raw == null) return -1;
        int index = 0;
        while (index < raw.length) {
            int length = raw[index] & 0xFF;
            if (length == 0 || index + length >= raw.length) break;
            int type = raw[index + 1] & 0xFF;
            if (type == 0x19 && length >= 3) return (raw[index + 2] & 0xFF) | ((raw[index + 3] & 0xFF) << 8);
            index += length + 1;
        }
        return -1;
    }

    private String decodedNames(byte[] raw) {
        if (raw == null) return "";
        List<String> names = new ArrayList<>();
        int index = 0;
        while (index < raw.length) {
            int length = raw[index] & 0xFF;
            if (length == 0 || index + length >= raw.length) break;
            int type = raw[index + 1] & 0xFF;
            if ((type == 0x08 || type == 0x09) && length > 1) {
                String value = new String(raw, index + 2, length - 1, StandardCharsets.UTF_8).trim();
                if (!value.isEmpty()) names.add(value);
            }
            index += length + 1;
        }
        return join(names);
    }

    private String decodedAdvertisingSummary(byte[] raw) {
        if (raw == null) return "";
        List<String> fields = new ArrayList<>();
        int index = 0;
        while (index < raw.length) {
            int length = raw[index] & 0xFF;
            if (length == 0 || index + length >= raw.length) break;
            int type = raw[index + 1] & 0xFF;
            String label;
            switch (type) {
                case 0x01: label = "Flags"; break;
                case 0x02: case 0x03: label = "16bit služby"; break;
                case 0x06: case 0x07: label = "128bit služby"; break;
                case 0x08: label = "Krátký název"; break;
                case 0x09: label = "Úplný název"; break;
                case 0x0A: label = "TX Power"; break;
                case 0x16: label = "Service Data"; break;
                case 0x19: label = "Appearance"; break;
                case 0xFF: label = "Manufacturer Data"; break;
                default: label = String.format(Locale.ROOT, "AD typ 0x%02X", type);
            }
            fields.add(label + " (" + (length - 1) + " B)" + decodedAdValue(raw, index + 2, length - 1, type));
            index += length + 1;
        }
        return join(fields);
    }

    private String decodedAdValue(byte[] raw, int offset, int length, int type) {
        if (raw == null || length <= 0 || offset < 0 || offset + length > raw.length) return "";
        if (type == 0x08 || type == 0x09) {
            String name = new String(raw, offset, length, StandardCharsets.UTF_8).replaceAll("\\p{Cntrl}", "").trim();
            return name.isEmpty() ? "" : ": " + name;
        }
        if (type == 0x0A && length >= 1) return ": " + raw[offset] + " dBm";
        if (type == 0x19 && length >= 2) {
            int appearance = (raw[offset] & 0xFF) | ((raw[offset + 1] & 0xFF) << 8);
            String name = appearances.get(appearance);
            return String.format(Locale.ROOT, ": 0x%04X%s", appearance, name == null ? "" : " – " + name);
        }
        if ((type == 0x02 || type == 0x03 || type == 0x16) && length >= 2) {
            int uuid = (raw[offset] & 0xFF) | ((raw[offset + 1] & 0xFF) << 8);
            String name = services.get(uuid);
            String prefix = String.format(Locale.ROOT, ": 0x%04X%s", uuid, name == null ? "" : " – " + name);
            return type == 0x16 && length > 2 ? prefix + " • data " + hexSlice(raw, offset + 2, length - 2, 24) : prefix;
        }
        if (type == 0xFF && length >= 2) {
            int company = (raw[offset] & 0xFF) | ((raw[offset + 1] & 0xFF) << 8);
            String name = companies.get(company);
            return String.format(Locale.ROOT, ": 0x%04X%s • data %s", company,
                    name == null ? "" : " – " + name, hexSlice(raw, offset + 2, length - 2, 28));
        }
        return ": " + hexSlice(raw, offset, length, 30);
    }

    private String hexSlice(byte[] data, int offset, int length, int maxBytes) {
        if (data == null || length <= 0 || offset < 0 || offset >= data.length) return "";
        StringBuilder out = new StringBuilder();
        int end = Math.min(data.length, offset + Math.min(length, maxBytes));
        for (int i = offset; i < end; i++) {
            if (out.length() > 0) out.append(' ');
            out.append(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
        }
        if (length > maxBytes) out.append(" …");
        return out.toString();
    }

    private String classLabel(BluetoothClass clazz) {
        if (clazz == null) return "Bluetooth třída nebyla vyslána";
        int device = clazz.getDeviceClass();
        switch (device) {
            case BluetoothClass.Device.COMPUTER_DESKTOP: return "Stolní počítač";
            case BluetoothClass.Device.COMPUTER_LAPTOP: return "Notebook";
            case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA: return "Kapesní počítač/PDA";
            case BluetoothClass.Device.PHONE_SMART: return "Chytrý telefon";
            case BluetoothClass.Device.PHONE_CELLULAR: return "Mobilní telefon";
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET: return "Bluetooth headset";
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES: return "Sluchátka";
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER: return "Bluetooth reproduktor";
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO: return "Autorádio / audio v autě";
            case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO: return "Hi‑Fi audio zařízení";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA: return "Videokamera";
            case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER: return "Videokamera / camcorder";
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER: return "TV / displej s reproduktorem";
            case BluetoothClass.Device.WEARABLE_WRIST_WATCH: return "Chytré hodinky";
            case BluetoothClass.Device.WEARABLE_GLASSES: return "Chytré brýle";
        }
        switch (clazz.getMajorDeviceClass()) {
            case BluetoothClass.Device.Major.COMPUTER: return "Počítač";
            case BluetoothClass.Device.Major.PHONE: return "Telefon";
            case BluetoothClass.Device.Major.AUDIO_VIDEO: return "Audio/Video zařízení";
            case BluetoothClass.Device.Major.PERIPHERAL: return "Klávesnice, myš nebo ovladač";
            case BluetoothClass.Device.Major.IMAGING: return "Tiskárna, skener nebo kamera";
            case BluetoothClass.Device.Major.WEARABLE: return "Nositelné zařízení";
            case BluetoothClass.Device.Major.HEALTH: return "Zdravotní zařízení";
            case BluetoothClass.Device.Major.TOY: return "Hračka";
            default: return "Neurčená Bluetooth kategorie";
        }
    }

    private String bluetoothTransport(int type) {
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC: return "Bluetooth Classic";
            case BluetoothDevice.DEVICE_TYPE_LE: return "Bluetooth Low Energy";
            case BluetoothDevice.DEVICE_TYPE_DUAL: return "Bluetooth Classic + LE";
            default: return "neznámý Bluetooth transport";
        }
    }

    private String normalizeMac(String address) {
        return address == null ? "" : address.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
    }

    private boolean isPrivateMac(String normalized) {
        if (normalized.length() < 2) return true;
        try { return (Integer.parseInt(normalized.substring(0, 2), 16) & 0x02) != 0; }
        catch (Exception ex) { return true; }
    }

    private String hex(byte[] data, int maxBytes) {
        if (data == null) return "";
        StringBuilder out = new StringBuilder();
        int length = Math.min(data.length, maxBytes);
        for (int i = 0; i < length; i++) {
            if (i > 0) out.append(' ');
            out.append(String.format(Locale.ROOT, "%02X", data[i] & 0xFF));
        }
        if (data.length > length) out.append(" …");
        return out.toString();
    }

    private boolean containsAny(String value, String... values) {
        for (String item : values) if (value.contains(item)) return true;
        return false;
    }

    private <T> List<T> safe(List<T> value) {
        return value == null ? new ArrayList<>() : value;
    }

    private String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) out.append("\n");
            out.append(value);
        }
        return out.toString();
    }

    static final class BluetoothIdentity {
        String vendor = "";
        String manufacturerIds = "";
        String services = "";
        String appearance = "";
        String deviceClass = "";
        String addressType = "";
        String rawAdvertisement = "";
        String decodedName = "";
        String decodedData = "";
        String modelHint = "";
        String trackerName = "";
        String trackerReason = "";
        String categoryHint = "";
        String categoryReason = "";
        int trackerConfidence;
        int categoryConfidence;
        int txPower = Integer.MIN_VALUE;

        String searchableText() {
            return vendor + " " + manufacturerIds + " " + services + " " + appearance + " " + deviceClass + " " + decodedName + " " + decodedData + " " + modelHint + " " + categoryReason;
        }
    }

    private static final class TrackerMatch {
        final String name;
        final int confidence;
        final String reason;
        TrackerMatch(String name, int confidence, String reason) {
            this.name = name;
            this.confidence = confidence;
            this.reason = reason;
        }
    }

    private static final class NameSignature {
        final String[] needles;
        final String brand;
        final String model;
        final String category;
        final int confidence;
        final String reason;

        NameSignature(String pattern, String brand, String model, String category, int confidence, String reason) {
            this.needles = pattern.toLowerCase(Locale.ROOT).split("\\|");
            this.brand = brand;
            this.model = model;
            this.category = category;
            this.confidence = confidence;
            this.reason = reason;
        }

        boolean matches(String value) {
            for (String needle : needles) {
                String clean = needle.trim();
                if (!clean.isEmpty() && value.contains(clean)) return true;
            }
            return false;
        }
    }
}
