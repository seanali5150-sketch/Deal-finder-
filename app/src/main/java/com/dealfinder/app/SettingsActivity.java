package com.dealfinder.app;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private Prefs prefs;
    private EditText bestbuy, keepa, ebayId, ebaySecret, minDisc, alertDisc, minPrice, interval;
    private CheckBox feeds;
    private LinearLayout col;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        ScrollView sv = new ScrollView(this);
        sv.setFitsSystemWindows(true);
        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        col.setPadding(pad, pad, pad, pad);
        sv.addView(col);

        header("Store API keys");
        note("Leave blank to skip a store. Deal feeds (Slickdeals, Reddit) work with no key.");
        bestbuy = field("Best Buy API key (free, developer.bestbuy.com)", prefs.bestbuyKey(), false);
        keepa = field("Keepa API key for Amazon (paid, keepa.com)", prefs.keepaKey(), false);
        ebayId = field("eBay App ID / Client ID (free, developer.ebay.com)", prefs.ebayId(), false);
        ebaySecret = field("eBay Cert ID / Client Secret", prefs.ebaySecret(), false);
        feeds = new CheckBox(this);
        feeds.setText("Watch deal feeds (Slickdeals, Reddit)");
        feeds.setChecked(prefs.feedsOn());
        col.addView(feeds);

        header("Deal rules");
        minDisc = field("Show deals at least this % off", "" + prefs.minDiscount(), true);
        alertDisc = field("Buzz my phone at this % off or more", "" + prefs.alertDiscount(), true);
        minPrice = field("Ignore items normally under $ (skips cables, cases)", "" + prefs.minPrice(), true);
        interval = field("Live watch: check every N minutes", "" + prefs.intervalMin(), true);

        Button save = new Button(this);
        save.setText("Save");
        save.setOnClickListener(v -> save());
        col.addView(save);
        setContentView(sv);
    }

    private void save() {
        prefs.edit()
            .putString("bestbuyKey", bestbuy.getText().toString().trim())
            .putString("keepaKey", keepa.getText().toString().trim())
            .putString("ebayId", ebayId.getText().toString().trim())
            .putString("ebaySecret", ebaySecret.getText().toString().trim())
            .putBoolean("feedsOn", feeds.isChecked())
            .putInt("minDiscount", clamp(minDisc, 40, 5, 95))
            .putInt("alertDiscount", clamp(alertDisc, 60, 5, 99))
            .putInt("minPrice", clamp(minPrice, 80, 0, 100000))
            .putInt("intervalMin", clamp(interval, 3, 1, 120))
            .apply();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private int clamp(EditText e, int def, int lo, int hi) {
        try { return Math.max(lo, Math.min(hi, Integer.parseInt(e.getText().toString().trim()))); }
        catch (NumberFormatException x) { return def; }
    }

    private void header(String s) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(18); t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, 24, 0, 4);
        col.addView(t);
    }

    private void note(String s) {
        TextView t = new TextView(this); t.setText(s); t.setAlpha(0.7f); col.addView(t);
    }

    private EditText field(String label, String value, boolean number) {
        TextView l = new TextView(this); l.setText(label); l.setPadding(0, 16, 0, 0); col.addView(l);
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(value);
        e.setInputType(number ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        col.addView(e);
        return e;
    }
}
