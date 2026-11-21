package com.dealfinder.app.sources;

import com.dealfinder.app.Deal;
import com.dealfinder.app.Http;
import com.dealfinder.app.Prefs;
import com.dealfinder.app.Targets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Amazon via Keepa (paid key at keepa.com/#!api). Keepa already tracks every
 * Amazon price, so its Deals endpoint returns products whose price just fell
 * far below their own 90-day average. Keepa prices are integer cents.
 */
public class KeepaSource implements Source {
    private static final int PRICE_AMAZON = 0, PRICE_NEW = 1;
    private static final int RANGE_90_DAYS = 3; // deal.avg index: day, week, month, 90 days

    public String name() { return "Amazon (Keepa)"; }
    public boolean enabled(Prefs p) { return !p.keepaKey().isEmpty(); }

    public List<Deal> fetch(Prefs p) throws Exception {
        JSONObject sel = new JSONObject()
            .put("page", 0).put("domainId", 1) // amazon.com
            .put("priceTypes", new JSONArray().put(PRICE_NEW))
            .put("dateRange", RANGE_90_DAYS)
            .put("deltaPercentRange", new JSONArray().put(p.minDiscount()).put(100))
            .put("currentRange", new JSONArray().put(p.minPrice() * 100 / 4).put(Integer.MAX_VALUE))
            // 541966 = Computers & Accessories, 2335752011 = Cell Phones & Accessories
            .put("includeCategories", new JSONArray().put(541966L).put(2335752011L))
            .put("isRangeEnabled", true).put("isFilterEnabled", false)
            .put("sortType", 4); // biggest % drop first
        String body = Http.request("POST", "https://api.keepa.com/deal?key=" + p.keepaKey(),
            Collections.singletonMap("Content-Type", "application/json"), sel.toString());
        JSONObject res = new JSONObject(body);
        JSONObject deals = res.optJSONObject("deals");
        JSONArray dr = deals == null ? null : deals.optJSONArray("dr");
        List<Deal> out = new ArrayList<>();
        if (dr == null) return out;
        for (int i = 0; i < dr.length(); i++) {
            JSONObject o = dr.getJSONObject(i);
            JSONArray cur = o.optJSONArray("current");
            int c = cur == null ? -1 : cur.optInt(PRICE_NEW, -1);
            if (c <= 0 && cur != null) c = cur.optInt(PRICE_AMAZON, -1);
            if (c <= 0) continue;
            int avg = -1;
            JSONArray avgs = o.optJSONArray("avg");
            if (avgs != null && avgs.optJSONArray(RANGE_90_DAYS) != null) avg = avgs.optJSONArray(RANGE_90_DAYS).optInt(PRICE_NEW, -1);
            Deal d = new Deal();
            d.id = "amazon:" + o.optString("asin");
            d.store = "Amazon"; d.source = name();
            d.name = o.optString("title");
            d.price = c / 100.0;
            d.typicalPrice = avg > 0 ? avg / 100.0 : 0;
            d.url = "https://www.amazon.com/dp/" + o.optString("asin");
            if (Targets.isTarget(d.name)) out.add(d);
        }
        return out;
    }
}
