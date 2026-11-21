package com.dealfinder.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Prefs prefs;
    private TextView status;
    private Button liveBtn, scanBtn;
    private final List<Deal> deals = new ArrayList<>();
    private DealAdapter adapter;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        Notifier.channels(this);
        ScanJob.schedule(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), 0);
        root.setFitsSystemWindows(true);

        TextView h = new TextView(this);
        h.setText("Deal Finder");
        h.setTextSize(24); h.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(h);
        TextView sub = new TextView(this);
        sub.setText("Price errors on computers, laptops, tablets and phones");
        sub.setAlpha(0.7f);
        root.addView(sub);

        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(12), 0, dp(8));
        scanBtn = button("Check now", v -> scanNow());
        liveBtn = button("", v -> toggleLive());
        Button set = button("Settings", v -> startActivity(new Intent(this, SettingsActivity.class)));
        row.addView(scanBtn, weight()); row.addView(liveBtn, weight()); row.addView(set, weight());
        root.addView(row);

        status = new TextView(this);
        status.setTextSize(12); status.setAlpha(0.7f);
        status.setPadding(0, 0, 0, dp(8));
        root.addView(status);

        ListView list = new ListView(this);
        adapter = new DealAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(deals.get(pos).url))); }
            catch (Exception e) { Toast.makeText(this, "Can't open link", Toast.LENGTH_SHORT).show(); }
        });
        TextView empty = new TextView(this);
        empty.setText("No deals yet. Tap Check now. Add your free Best Buy key in Settings for the fastest alerts.");
        empty.setGravity(Gravity.CENTER); empty.setPadding(dp(24), dp(48), dp(24), dp(24)); empty.setAlpha(0.7f);
        root.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        list.setEmptyView(empty);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        deals.clear();
        deals.addAll(Scanner.load(this));
        adapter.notifyDataSetChanged();
        liveBtn.setText(prefs.liveWatch() ? "Stop live" : "Live watch");
        long last = prefs.raw().getLong("lastScan", 0);
        status.setText((last == 0 ? "Not checked yet" : "Last check " + DateUtils.getRelativeTimeSpanString(last)) +
            (prefs.liveWatch() ? " · live every " + prefs.intervalMin() + " min" : " · auto every 15 min") +
            "\n" + prefs.raw().getString("lastStatus", ""));
    }

    private void scanNow() {
        scanBtn.setEnabled(false); scanBtn.setText("Checking…");
        new Thread(() -> {
            Scanner.Result r = Scanner.run(this);
            runOnUiThread(() -> {
                scanBtn.setEnabled(true); scanBtn.setText("Check now");
                Toast.makeText(this, r.found + " deals, " + r.alerted + " new alerts", Toast.LENGTH_SHORT).show();
                refresh();
            });
        }).start();
    }

    private void toggleLive() {
        boolean on = !prefs.liveWatch();
        prefs.edit().putBoolean("liveWatch", on).apply();
        if (on) {
            WatchService.start(this);
            PowerManager pm = getSystemService(PowerManager.class);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                // Without this, Android pauses network checks when the screen is off.
                try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))); }
                catch (Exception ignored) { }
            }
        } else {
            WatchService.stop(this);
        }
        refresh();
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setOnClickListener(l); return b;
    }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, -2, 1); }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private class DealAdapter extends BaseAdapter {
        public int getCount() { return deals.size(); }
        public Object getItem(int i) { return deals.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int i, View convert, ViewGroup parent) {
            LinearLayout box;
            TextView top, name, meta;
            if (convert == null) {
                box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(0, dp(10), 0, dp(10));
                top = new TextView(MainActivity.this); top.setTypeface(Typeface.DEFAULT_BOLD); top.setTextSize(16);
                name = new TextView(MainActivity.this); name.setMaxLines(2);
                meta = new TextView(MainActivity.this); meta.setTextSize(12); meta.setAlpha(0.65f);
                box.addView(top); box.addView(name); box.addView(meta);
            } else {
                box = (LinearLayout) convert;
                top = (TextView) box.getChildAt(0); name = (TextView) box.getChildAt(1); meta = (TextView) box.getChildAt(2);
            }
            Deal d = deals.get(i);
            String pct = d.discountPct > 0 ? d.discountPct + "% off" : "Possible price error";
            top.setText(pct + " · " + (d.price > 0 ? Notifier.money(d.price) : "see post") + " · " + d.store);
            top.setTextColor(d.discountPct >= prefs.alertDiscount() || d.flaggedAsError ? Color.rgb(255, 82, 82) : Color.rgb(102, 187, 106));
            name.setText(d.name);
            meta.setText((d.reference > 0 ? "Normally " + Notifier.money(d.reference) + " (" + d.basis + ") · " : "") +
                "via " + d.source + " · " + DateUtils.getRelativeTimeSpanString(d.foundAt));
            return box;
        }
    }
}
