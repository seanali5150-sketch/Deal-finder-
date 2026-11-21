package com.dealfinder.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

/**
 * "Live watch": keeps running in the background and checks every few minutes,
 * which is much faster than Android's normal 15-minute minimum.
 */
public class WatchService extends Service {
    private HandlerThread thread;
    private Handler handler;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            Scanner.run(WatchService.this);
            handler.postDelayed(this, new Prefs(WatchService.this).intervalMin() * 60_000L);
        }
    };

    public static void start(Context c) {
        c.startForegroundService(new Intent(c, WatchService.class));
    }

    public static void stop(Context c) {
        c.stopService(new Intent(c, WatchService.class));
    }

    @Override public void onCreate() {
        super.onCreate();
        Notifier.channels(this);
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, Notifier.CH_WATCH)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("Watching for price errors")
            .setContentText("Checking every " + new Prefs(this).intervalMin() + " min")
            .setContentIntent(pi)
            .setOngoing(true)
            .build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
        thread = new HandlerThread("watch");
        thread.start();
        handler = new Handler(thread.getLooper());
        handler.post(tick);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }

    @Override public void onDestroy() {
        if (handler != null) handler.removeCallbacksAndMessages(null);
        if (thread != null) thread.quitSafely();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
