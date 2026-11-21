package com.dealfinder.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) return;
        ScanJob.schedule(c);
        if (new Prefs(c).liveWatch()) {
            try { WatchService.start(c); } catch (Exception ignored) { } // some phones block this; the job still runs
        }
    }
}
