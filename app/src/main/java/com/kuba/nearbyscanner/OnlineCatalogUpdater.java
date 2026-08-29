package com.kuba.nearbyscanner;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class OnlineCatalogUpdater {
    private static final String[][] SOURCES = {
            {"IEEE MA‑L", "https://standards-oui.ieee.org/oui/oui.csv"},
            {"IEEE MA‑M", "https://standards-oui.ieee.org/oui28/mam.csv"},
            {"IEEE MA‑S", "https://standards-oui.ieee.org/oui36/oui36.csv"}
    };

    static final class Result {
        final boolean success;
        final int entries;
        final String message;
        Result(boolean success, int entries, String message) {
            this.success = success;
            this.entries = entries;
            this.message = message;
        }
    }

    static File catalogFile(Context context) {
        return new File(new File(context.getFilesDir(), "device_catalog"), "ieee_online.tsv");
    }

    static Result update(Context context) {
        File target = catalogFile(context);
        File directory = target.getParentFile();
        if (directory == null || (!directory.exists() && !directory.mkdirs()))
            return new Result(false, 0, "Nelze vytvořit složku databáze");
        File temporary = new File(directory, "ieee_online.new");
        int total = 0;
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(temporary), StandardCharsets.UTF_8)) {
            for (String[] source : SOURCES) total += download(source[0], source[1], writer);
        } catch (Exception ex) {
            if (temporary.exists()) temporary.delete();
            return new Result(false, 0, "Aktualizace selhala: " + safeMessage(ex));
        }
        if (total < 1000) {
            temporary.delete();
            return new Result(false, total, "Stažený katalog je neúplný – původní databáze zůstala zachována");
        }
        File backup = new File(directory, "ieee_online.backup");
        if (backup.exists()) backup.delete();
        boolean hadOld = target.exists();
        if (hadOld && !target.renameTo(backup)) {
            temporary.delete();
            return new Result(false, total, "Starou online databázi nelze bezpečně zazálohovat");
        }
        if (!temporary.renameTo(target)) {
            if (hadOld) backup.renameTo(target);
            return new Result(false, total, "Staženou databázi nelze aktivovat; původní verze byla obnovena");
        }
        if (backup.exists()) backup.delete();
        return new Result(true, total, "Online databáze IEEE aktualizována • " + total + " přiřazení");
    }

    private static int download(String label, String address, Writer writer) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(25000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "KUBA-Nearby-Scanner/1.3");
        int code = connection.getResponseCode();
        if (code < 200 || code >= 300) throw new IllegalStateException(label + " HTTP " + code);
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                List<String> fields = csv(line);
                if (fields.size() < 3) continue;
                String prefix = fields.get(1).replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
                String vendor = fields.get(2).trim();
                if ((prefix.length() == 6 || prefix.length() == 7 || prefix.length() == 9) && !vendor.isEmpty()) {
                    writer.write(prefix);
                    writer.write('\t');
                    writer.write(vendor.replace('\t', ' '));
                    writer.write('\n');
                    count++;
                }
            }
        } finally {
            connection.disconnect();
        }
        return count;
    }

    private static List<String> csv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { fields.add(value.toString()); value.setLength(0); }
            else value.append(c);
        }
        fields.add(value.toString());
        return fields;
    }

    static String status(Context context) {
        File file = catalogFile(context);
        if (!file.isFile()) return "Online katalog zatím nebyl stažen. Aplikace používá úplnou vestavěnou databázi z doby sestavení.";
        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) lines++;
        } catch (Exception ignored) { }
        String date = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                Locale.forLanguageTag("cs-CZ")).format(new Date(file.lastModified()));
        return "Aktivní online aktualizace: " + lines + " přiřazení • staženo " + date;
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.trim().isEmpty() ? ex.getClass().getSimpleName() : message;
    }
}
