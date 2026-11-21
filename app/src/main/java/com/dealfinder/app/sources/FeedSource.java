package com.dealfinder.app.sources;

import android.util.Xml;

import com.dealfinder.app.Deal;
import com.dealfinder.app.Http;
import com.dealfinder.app.Prefs;
import com.dealfinder.app.Targets;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Public deal-community RSS feeds (no key needed). People post price errors
 * here within minutes, across every store, including ones with no API
 * (Target, Walmart, Costco, Newegg, B&H, Dell, Apple...).
 */
public class FeedSource implements Source {
    private static final String[][] FEEDS = {
        {"Slickdeals", "https://slickdeals.net/newsearch.php?mode=frontpage&searcharea=deals&searchin=first&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=price+error&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=laptop&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=ipad&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=unlocked+phone&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=price+mistake&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=glitch&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=macbook&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=iphone&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=galaxy&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=chromebook&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Slickdeals", "https://slickdeals.net/newsearch.php?q=gaming+pc&searcharea=deals&searchin=first&sort=newest&rss=1"},
        {"Reddit r/buildapcsales", "https://www.reddit.com/r/buildapcsales/new/.rss"},
        {"Reddit r/laptopdeals", "https://www.reddit.com/r/laptopdeals/new/.rss"},
        {"Reddit r/deals", "https://www.reddit.com/r/deals/new/.rss"},
    };
    private static final Pattern MONEY = Pattern.compile("\\$\\s?([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?|[0-9]+(?:\\.[0-9]{2})?)");
    private static final Pattern PCT_OFF = Pattern.compile("(?i)\\b([1-9][0-9])\\s?%\\s?off\\b");
    private static final Pattern STORE = Pattern.compile("(?i)\\b(?:at|@|from)\\s+(Best ?Buy|Amazon|Walmart|Target|Costco|Newegg|B&H|Dell|HP|Lenovo|Apple|Samsung|Micro ?Center|Sam'?s Club|Woot|eBay|Adorama|Antonline|Office ?Depot|Staples|Kohl'?s|Macy'?s)\\b");

    public String name() { return "Deal feeds"; }
    public boolean enabled(Prefs p) { return p.feedsOn(); }

    public List<Deal> fetch(Prefs p) throws Exception {
        List<Deal> out = new ArrayList<>();
        Exception last = null;
        int ok = 0;
        for (String[] f : FEEDS) {
            try { parse(f[0], Http.get(f[1], null), p, out); ok++; }
            catch (Exception e) { last = e; }
        }
        if (ok == 0 && last != null) throw last;
        return out;
    }

    private void parse(String feed, String xml, Prefs p, List<Deal> out) throws Exception {
        XmlPullParser x = Xml.newPullParser();
        x.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        x.setInput(new StringReader(xml));
        String title = null, link = null; boolean inItem = false;
        for (int ev = x.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = x.next()) {
            if (ev == XmlPullParser.START_TAG) {
                String n = x.getName();
                if (n.equals("item") || n.equals("entry")) { inItem = true; title = null; link = null; }
                else if (inItem && n.equals("title")) title = x.nextText();
                else if (inItem && n.equals("link")) {
                    String href = x.getAttributeValue(null, "href"); // Atom
                    link = href != null ? href : x.nextText();      // RSS
                }
            } else if (ev == XmlPullParser.END_TAG && (x.getName().equals("item") || x.getName().equals("entry"))) {
                inItem = false;
                Deal d = fromTitle(feed, title, link, p);
                if (d != null) out.add(d);
            }
        }
    }

    /** Turns "Apple MacBook Air M3 $499 (Reg $1099) at Best Buy" into a Deal. */
    static Deal fromTitle(String feed, String title, String link, Prefs p) {
        if (title == null || link == null) return null;
        title = title.trim();
        if (!Targets.isTarget(title)) return null;
        List<Double> amounts = new ArrayList<>();
        Matcher m = MONEY.matcher(title);
        while (m.find()) {
            try { amounts.add(Double.parseDouble(m.group(1).replace(",", ""))); } catch (NumberFormatException ignored) { }
        }
        Deal d = new Deal();
        d.flaggedAsError = Targets.saysPriceError(title);
        if (amounts.isEmpty() && !d.flaggedAsError) return null;
        d.price = amounts.isEmpty() ? 0 : amounts.get(0);
        double max = 0;
        for (double a : amounts) max = Math.max(max, a);
        if (max > d.price) d.listPrice = max;
        // "75% off" in the title with only one price: work out the original price.
        Matcher pct = PCT_OFF.matcher(title);
        if (d.listPrice == 0 && d.price > 0 && pct.find()) {
            int off = Integer.parseInt(pct.group(1));
            d.listPrice = Math.round(d.price / (1 - off / 100.0) * 100) / 100.0;
        }
        if (d.price > 0 && d.listPrice > 0 && d.listPrice < p.minPrice()) return null;
        Matcher s = STORE.matcher(title);
        d.store = s.find() ? s.group(1) : feed;
        d.source = feed;
        d.name = title;
        d.url = link.trim();
        d.id = "feed:" + d.url;
        return d;
    }
}
