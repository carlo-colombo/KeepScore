package ovh.litapp.keepscore;

import android.app.Application;

import ovh.litapp.keepscore.data.work.BackupScheduler;

/**
 * Application entry point. Keeps a process-wide context for helpers that need
 * one but are called without it, and schedules the daily automatic backup with
 * WorkManager instead of the old AlarmManager + IntentService pair.
 */
public class KeepScoreApplication extends Application {

    private static KeepScoreApplication instance;

    public static KeepScoreApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        BackupScheduler.schedule(this);
    }
}
