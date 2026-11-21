package com.dealfinder.app;

import android.content.Context;
import android.content.SharedPreferences;

/** All user settings. Any source without a key is skipped. */
public class Prefs {
    private final SharedPreferences p;
    public Prefs(Context c) { p = c.getSharedPreferences("settings", Context.MODE_PRIVATE); }

    public String bestbuyKey() { return p.getString("bestbuyKey", "").trim(); }
    public String keepaKey()   { return p.getString("keepaKey", "").trim(); }
    public String ebayId()     { return p.getString("ebayId", "").trim(); }
    public String ebaySecret() { return p.getString("ebaySecret", "").trim(); }
    public boolean feedsOn()   { return p.getBoolean("feedsOn", true); }

    /** Show deals at least this % below normal. */
    public int minDiscount()   { return p.getInt("minDiscount", 40); }
    /** Buzz the phone at or above this %. */
    public int alertDiscount() { return p.getInt("alertDiscount", 60); }
    /** Ignore anything cheaper than this (filters out cables and cases). */
    public int minPrice()      { return p.getInt("minPrice", 80); }
    public int intervalMin()   { return Math.max(1, p.getInt("intervalMin", 3)); }
    public boolean liveWatch() { return p.getBoolean("liveWatch", false); }

    public SharedPreferences.Editor edit() { return p.edit(); }
    public SharedPreferences raw() { return p; }
}
