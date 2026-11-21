package com.dealfinder.app;

import org.json.JSONException;
import org.json.JSONObject;

/** One product offer, normalized across every source. */
public class Deal {
    public String id;          // stable per product, e.g. "bestbuy:6534613"
    public String store;       // "Best Buy", "Amazon", "eBay", "Slickdeals", ...
    public String name;
    public double price;       // current price, USD
    public double listPrice;   // store's regular/original price, 0 if unknown
    public double typicalPrice;// store-reported average price, 0 if unknown
    public String url;
    public String source;      // which connector found it

    // Filled in by Scorer
    public double reference;   // the "normal" price we compare against
    public int discountPct;
    public String basis = "";
    public boolean flaggedAsError; // title literally says price error / glitch
    public long foundAt;

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id).put("store", store).put("name", name).put("price", price)
             .put("listPrice", listPrice).put("typicalPrice", typicalPrice).put("url", url)
             .put("source", source).put("reference", reference).put("discountPct", discountPct)
             .put("basis", basis).put("flaggedAsError", flaggedAsError).put("foundAt", foundAt);
        } catch (JSONException ignored) { }
        return o;
    }

    public static Deal fromJson(JSONObject o) {
        Deal d = new Deal();
        d.id = o.optString("id"); d.store = o.optString("store"); d.name = o.optString("name");
        d.price = o.optDouble("price", 0); d.listPrice = o.optDouble("listPrice", 0);
        d.typicalPrice = o.optDouble("typicalPrice", 0); d.url = o.optString("url");
        d.source = o.optString("source"); d.reference = o.optDouble("reference", 0);
        d.discountPct = o.optInt("discountPct"); d.basis = o.optString("basis");
        d.flaggedAsError = o.optBoolean("flaggedAsError"); d.foundAt = o.optLong("foundAt");
        return d;
    }
}
