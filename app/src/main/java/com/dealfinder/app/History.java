package com.dealfinder.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Our own record of prices seen per product, kept on the phone. */
public class History {
    private static final int MAX_POINTS = 300;
    private static final long KEEP_MS = 120L * 24 * 3600 * 1000;
    private final File file;
    private JSONObject db;

    public History(Context c) {
        file = new File(c.getFilesDir(), "history.json");
        try { db = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)); }
        catch (Exception e) { db = new JSONObject(); }
    }

    /** Prices seen in the last 90 days, before this one. */
    public List<Double> prior(String id) {
        List<Double> out = new ArrayList<>();
        JSONArray a = db.optJSONArray(id);
        if (a == null) return out;
        long cutoff = System.currentTimeMillis() - 90L * 24 * 3600 * 1000;
        for (int i = 0; i < a.length(); i++) {
            JSONArray pt = a.optJSONArray(i);
            if (pt != null && pt.optLong(0) >= cutoff) out.add(pt.optDouble(1));
        }
        return out;
    }

    public void record(String id, double price) {
        try {
            JSONArray a = db.optJSONArray(id);
            if (a == null) { a = new JSONArray(); db.put(id, a); }
            long now = System.currentTimeMillis();
            JSONArray last = a.length() > 0 ? a.optJSONArray(a.length() - 1) : null;
            // one point per price change, or per hour if unchanged
            if (last == null || last.optDouble(1) != price || now - last.optLong(0) > 3600_000L) {
                a.put(new JSONArray().put(now).put(price));
            }
            while (a.length() > MAX_POINTS) a.remove(0);
        } catch (Exception ignored) { }
    }

    public void save() {
        try {
            long cutoff = System.currentTimeMillis() - KEEP_MS;
            for (Iterator<String> it = db.keys(); it.hasNext(); ) {
                JSONArray a = db.optJSONArray(it.next());
                if (a == null || a.length() == 0 || a.optJSONArray(a.length() - 1).optLong(0) < cutoff) it.remove();
            }
            Files.write(file.toPath(), db.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }
}
