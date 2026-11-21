package com.dealfinder.app;

import java.util.Locale;
import java.util.regex.Pattern;

/** What Shawn cares about: computers, laptops, tablets/iPads and phones. */
public final class Targets {
    private static final Pattern WANT = Pattern.compile(
        "\\b(laptop|notebook|macbook|chromebook|ultrabook|desktop|gaming pc|all-in-one|imac|mac mini|mac studio|" +
        "computer|workstation|thinkpad|ideapad|zenbook|vivobook|xps|spectre|envy|pavilion|omen|legion|rog|alienware|surface|" +
        "ipad|tablet|galaxy tab|fire hd|fire max|" +
        "iphone|smartphone|unlocked phone|cell phone|pixel \\d|galaxy s\\d|galaxy z|galaxy a\\d|oneplus|motorola edge|razr)\\b");
    // Things that mention a device but are not one.
    private static final Pattern SKIP = Pattern.compile(
        "\\b(case|cover|sleeve|screen protector|charger|cable|adapter|stand|mount|skin|stylus|keyboard cover|" +
        "backpack|bag|dock|hub|replacement|battery for|refurbished parts|for parts)\\b");
    private static final Pattern ERROR_WORDS = Pattern.compile(
        "\\b(price (error|mistake|glitch)|pricing error|glitch|mispriced|mis-priced|price mistake)\\b");

    public static boolean isTarget(String title) {
        String t = title.toLowerCase(Locale.US);
        return WANT.matcher(t).find() && !SKIP.matcher(t).find();
    }

    public static boolean saysPriceError(String title) {
        return ERROR_WORDS.matcher(title.toLowerCase(Locale.US)).find();
    }
}
