package com.dealfinder.app;

import android.content.Context;
import android.os.PowerManager;

import com.dealfinder.app.sources.BestBuySource;
import com.dealfinder.app.sources.EbaySource;
import com.dealfinder.app.sources.FeedSource;
import com.dealfinder.app.sources.KeepaSource;
import com.dealfinder.app.sources.Source;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Runs every source, scores what comes back, saves the list, and alerts on new big drops. */
public final class Scanner {
    public static final Source[] SOURCES = { new BestBuySource(), new KeepaSource(), new EbaySource(), new FeedSource() };
    private static final Object LOCK = new Object();
    private static final int MAX_SAVED = 300;

    public static class Result {
        public int found, alerted;
        public final Map<String, String> status = new LinkedHashMap<>();
    }

    public static Result run(Context ctx) {
        synchronized (LOCK) {
            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DealFinder:scan");
            wl.acquire(3 * 60 * 1000L);
            try { return runLocked(ctx.getApplicationContext()); }
            finally { if (wl.isHeld()) wl.release(); }
        }
    }

    private static Result runLocked(Context ctx) {
        Prefs prefs = new Prefs(ctx);
        Result r = new Result();
        ExecutorService pool = Executors.newFixedThreadPool(SOURCES.length);
        Map<Source, Future<List<Deal>>> jobs = new LinkedHashMap<>();
        for (Source s : SOURCES) {
            if (!s.enabled(prefs)) { r.status.put(s.name(), "off (add key in Settings)"); continue; }
            jobs.put(s, pool.submit(() -> s.fetch(prefs)));
        }
        History history = new History(ctx);
        List<Deal> hits = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<Source, Future<List<Deal>>> e : jobs.entrySet()) {
            try {
                List<Deal> deals = e.getValue().get(90, TimeUnit.SECONDS);
                int n = 0;
                for (Deal d : deals) {
                    boolean fromFeed = d.id.startsWith("feed:");
                    Scorer.score(d, fromFeed ? Collections.emptyList() : history.prior(d.id));
                    if (!fromFeed) history.record(d.id, d.price);
                    if (d.discountPct >= prefs.minDiscount() || d.flaggedAsError) { d.foundAt = now; hits.add(d); n++; }
                }
                r.status.put(e.getKey().name(), "ok, " + deals.size() + " checked, " + n + " deals");
            } catch (Exception ex) {
                Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                r.status.put(e.getKey().name(), "error: " + c.getMessage());
            }
        }
        pool.shutdownNow();
        history.save();

        // Merge with saved list; alert only on deals we haven't alerted before.
        List<Deal> saved = load(ctx);
        Set<String> alerted = alertedIds(ctx);
        Map<String, Deal> merged = new LinkedHashMap<>();
        for (Deal d : hits) merged.put(d.id, d);
        long keepAfter = now - 3L * 24 * 3600 * 1000; // drop deals not seen again for 3 days
        for (Deal d : saved) {
            if (merged.containsKey(d.id)) merged.get(d.id).foundAt = Math.min(merged.get(d.id).foundAt, d.foundAt);
            else if (d.foundAt >= keepAfter) merged.put(d.id, d);
        }

        for (Deal d : hits) {
            boolean big = d.discountPct >= prefs.alertDiscount() || d.flaggedAsError;
            String key = d.id + "@" + Math.round(d.price);
            if (big && !alerted.contains(key)) { Notifier.deal(ctx, d); alerted.add(key); r.alerted++; }
        }
        List<Deal> list = new ArrayList<>(merged.values());
        list.sort((a, b) -> Long.compare(b.foundAt, a.foundAt) != 0 ? Long.compare(b.foundAt, a.foundAt) : b.discountPct - a.discountPct);
        if (list.size() > MAX_SAVED) list = list.subList(0, MAX_SAVED);
        save(ctx, list);
        saveAlerted(ctx, alerted);
        r.found = hits.size();

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> s : r.status.entrySet()) sb.append(s.getKey()).append(": ").append(s.getValue()).append('\n');
        prefs.edit().putLong("lastScan", now).putString("lastStatus", sb.toString().trim()).apply();
        return r;
    }

    private static File dealsFile(Context c) { return new File(c.getFilesDir(), "deals.json"); }

    public static List<Deal> load(Context c) {
        List<Deal> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(new String(Files.readAllBytes(dealsFile(c).toPath()), StandardCharsets.UTF_8));
            for (int i = 0; i < a.length(); i++) out.add(Deal.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) { }
        return out;
    }

    private static void save(Context c, List<Deal> list) {
        JSONArray a = new JSONArray();
        for (Deal d : list) a.put(d.toJson());
        try { Files.write(dealsFile(c).toPath(), a.toString().getBytes(StandardCharsets.UTF_8)); } catch (Exception ignored) { }
    }

    private static Set<String> alertedIds(Context c) {
        return new HashSet<>(c.getSharedPreferences("alerted", Context.MODE_PRIVATE).getStringSet("ids", new HashSet<>()));
    }

    private static void saveAlerted(Context c, Set<String> ids) {
        if (ids.size() > 2000) { List<String> l = new ArrayList<>(ids); ids = new HashSet<>(l.subList(l.size() - 1500, l.size())); }
        c.getSharedPreferences("alerted", Context.MODE_PRIVATE).edit().putStringSet("ids", ids).apply();
    }

}
