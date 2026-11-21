package com.dealfinder.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.Locale;

public final class Notifier {
    public static final String CH_DEALS = "deals";
    public static final String CH_WATCH = "watch";

    public static void channels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel deals = new NotificationChannel(CH_DEALS, "Price error alerts", NotificationManager.IMPORTANCE_HIGH);
        deals.setDescription("Fires when a computer, tablet or phone drops far below its normal price");
        deals.enableVibration(true);
        nm.createNotificationChannel(deals);
        NotificationChannel watch = new NotificationChannel(CH_WATCH, "Live watch running", NotificationManager.IMPORTANCE_MIN);
        nm.createNotificationChannel(watch);
    }

    public static String money(double v) { return String.format(Locale.US, "$%,.2f", v); }

    public static void deal(Context c, Deal d) {
        channels(c);
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(d.url));
        PendingIntent pi = PendingIntent.getActivity(c, d.id.hashCode(), open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String title = d.discountPct > 0
            ? d.discountPct + "% off at " + d.store + ": " + money(d.price)
            : "Possible price error at " + d.store;
        String text = d.name + (d.reference > 0 ? "\nNormally about " + money(d.reference) + " (" + d.basis + ")" : "");
        Notification n = new Notification.Builder(c, CH_DEALS)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(d.name)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build();
        c.getSystemService(NotificationManager.class).notify(d.id.hashCode(), n);
    }
}
