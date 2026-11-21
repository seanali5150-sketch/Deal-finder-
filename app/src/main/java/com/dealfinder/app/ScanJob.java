package com.dealfinder.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Backup check every ~15 minutes (Android's minimum) even when Live watch is off. */
public class ScanJob extends JobService {
    private static final int ID = 42;

    public static void schedule(Context c) {
        JobInfo job = new JobInfo.Builder(ID, new ComponentName(c, ScanJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(15 * 60 * 1000L)
            .setPersisted(true)
            .build();
        c.getSystemService(JobScheduler.class).schedule(job);
    }

    @Override public boolean onStartJob(JobParameters params) {
        new Thread(() -> { Scanner.run(this); jobFinished(params, false); }).start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) { return true; }
}
