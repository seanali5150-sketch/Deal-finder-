package com.dealfinder.app.sources;

import android.util.Base64;

import com.dealfinder.app.Deal;
import com.dealfinder.app.Http;
import com.dealfinder.app.Prefs;
import com.dealfinder.app.Targets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** eBay Browse API (free keys at developer.ebay.com). New, buy-it-now listings only. */
public class EbaySource implements Source {
    private static final String[] QUERIES = {
        "laptop", "macbook", "gaming pc", "ipad", "galaxy tab", "iphone unlocked", "samsung galaxy unlocked", "pixel phone"
    };
    private static String token;
    private static long tokenExpires;

    public String name() { return "eBay"; }
    public boolean enabled(Prefs p) { return !p.ebayId().isEmpty() && !p.ebaySecret().isEmpty(); }

    private static synchronized String token(Prefs p) throws Exception {
        if (token != null && System.currentTimeMillis() < tokenExpires) return token;
        Map<String, String> h = new HashMap<>();
        h.put("Content-Type", "application/x-www-form-urlencoded");
        h.put("Authorization", "Basic " + Base64.encodeToString(
            (p.ebayId() + ":" + p.ebaySecret()).getBytes("UTF-8"), Base64.NO_WRAP));
        JSONObject b = new JSONObject(Http.request("POST", "https://api.ebay.com/identity/v1/oauth2/token", h,
            "grant_type=client_credentials&scope=https%3A%2F%2Fapi.ebay.com%2Foauth%2Fapi_scope"));
        token = b.getString("access_token");
        tokenExpires = System.currentTimeMillis() + (b.optLong("expires_in", 7200) - 120) * 1000;
        return token;
    }

    public List<Deal> fetch(Prefs p) throws Exception {
        Map<String, String> h = new HashMap<>();
        h.put("Authorization", "Bearer " + token(p));
        h.put("X-EBAY-C-MARKETPLACE-ID", "EBAY_US");
        List<Deal> out = new ArrayList<>();
        for (String q : QUERIES) {
            String url = "https://api.ebay.com/buy/browse/v1/item_summary/search?limit=100&sort=newlyListed&q="
                + URLEncoder.encode(q, "UTF-8")
                + "&filter=" + URLEncoder.encode("conditions:{NEW},buyingOptions:{FIXED_PRICE},price:[" + p.minPrice() / 2 + "..],priceCurrency:USD", "UTF-8");
            JSONArray items = new JSONObject(Http.get(url, h)).optJSONArray("itemSummaries");
            if (items == null) continue;
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.getJSONObject(i);
                Deal d = new Deal();
                d.id = "ebay:" + it.optString("itemId");
                d.store = "eBay"; d.source = name();
                d.name = it.optString("title");
                JSONObject price = it.optJSONObject("price");
                d.price = price == null ? 0 : price.optDouble("value", 0);
                JSONObject mp = it.optJSONObject("marketingPrice");
                JSONObject orig = mp == null ? null : mp.optJSONObject("originalPrice");
                d.listPrice = orig == null ? 0 : orig.optDouble("value", 0);
                d.url = it.optString("itemWebUrl");
                if (d.price > 0 && Targets.isTarget(d.name)) out.add(d);
            }
        }
        return out;
    }
}
