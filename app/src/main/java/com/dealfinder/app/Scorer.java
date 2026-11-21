package com.dealfinder.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decides how far below "normal" a price is.
 * Normal = our own 90-day median once we have enough history, else the store's
 * average price, else its regular/list price. Using real observed prices beats
 * fake "was $1,999" stickers.
 */
public final class Scorer {
    public static double median(List<Double> xs) {
        List<Double> a = new ArrayList<>();
        for (Double x : xs) if (x != null && x > 0) a.add(x);
        if (a.isEmpty()) return 0;
        Collections.sort(a);
        int m = a.size() / 2;
        return a.size() % 2 == 1 ? a.get(m) : (a.get(m - 1) + a.get(m)) / 2;
    }

    public static void score(Deal d, List<Double> history) {
        double observed = median(history);
        double ref; String basis;
        if (history.size() >= 5 && observed > 0) { ref = observed; basis = "your 90-day price history"; }
        else if (d.typicalPrice > 0) { ref = d.typicalPrice; basis = "store's 90-day average"; }
        else if (d.listPrice > 0) { ref = d.listPrice; basis = "regular price"; }
        else { ref = 0; basis = d.flaggedAsError ? "posted as a price error" : "no reference price"; }

        // Don't trust a list price that's way above what we've actually seen.
        if ("regular price".equals(basis) && observed > 0 && observed < ref * 0.8) {
            ref = observed; basis = "your price history";
        }
        d.reference = Math.round(ref * 100) / 100.0;
        d.basis = basis;
        d.discountPct = ref > 0 ? (int) Math.round((1 - d.price / ref) * 100) : 0;
    }
}
