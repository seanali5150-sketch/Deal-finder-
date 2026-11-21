package com.dealfinder.app.sources;

import com.dealfinder.app.Deal;
import com.dealfinder.app.Http;
import com.dealfinder.app.Prefs;
import com.dealfinder.app.Targets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Best Buy Products API (free key at developer.bestbuy.com).
 * Asks Best Buy directly for Computers & Tablets and Cell Phones that are on
 * sale by a big percentage, biggest discount first.
 */
public class BestBuySource implements Source {
    // abcat0500000 = Computers & Tablets, abcat0800000 = Cell Phones
    private static final String CATEGORIES = "(categoryPath.id=abcat0500000|categoryPath.id=abcat0800000)";

    public String name() { return "Best Buy"; }
    public boolean enabled(Prefs p) { return !p.bestbuyKey().isEmpty(); }

    public List<Deal> fetch(Prefs p) throws Exception {
        List<Deal> out = new ArrayList<>();
        String filter = "(" + CATEGORIES + "&onSale=true&active=true"
            + "&percentSavings>=" + p.minDiscount() + "&regularPrice>=" + p.minPrice() + ")";
        for (int page = 1; page <= 3; page++) {
            String url = "https://api.bestbuy.com/v1/products" + URLEncoder.encode(filter, "UTF-8").replace("+", "%20")
                + "?apiKey=" + p.bestbuyKey()
                + "&show=sku,name,salePrice,regularPrice,url,percentSavings"
                + "&sort=percentSavings.dsc&pageSize=100&page=" + page + "&format=json";
            JSONObject body = new JSONObject(Http.get(url, null));
            JSONArray products = body.optJSONArray("products");
            if (products == null) break;
            for (int i = 0; i < products.length(); i++) {
                JSONObject o = products.getJSONObject(i);
                Deal d = new Deal();
                d.id = "bestbuy:" + o.optString("sku");
                d.store = "Best Buy"; d.source = name();
                d.name = o.optString("name");
                d.price = o.optDouble("salePrice", 0);
                d.listPrice = o.optDouble("regularPrice", 0);
                d.url = o.optString("url");
                if (d.price > 0 && Targets.isTarget(d.name)) out.add(d);
            }
            if (page >= body.optInt("totalPages", 1)) break;
            Thread.sleep(250); // stay well under Best Buy's 5 requests/second limit
        }
        return out;
    }
}
