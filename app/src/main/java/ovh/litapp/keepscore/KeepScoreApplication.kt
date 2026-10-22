package ovh.litapp.keepscore

import android.app.Application
import androidx.annotation.VisibleForTesting
import ovh.litapp.keepscore.data.work.BackupScheduler

/**
 * Application entry point. Keeps a process-wide context for helpers that need one but are called
 * without it, and schedules the daily automatic backup with WorkManager instead of the old
 * AlarmManager + IntentService pair.
 */
open class KeepScoreApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        BackupScheduler.schedule(this)
    }

    companion object {

        @Volatile
        private var instance: KeepScoreApplication? = null

        /** The application context, or null while no application is alive. */
        @JvmStatic
        fun getInstance(): KeepScoreApplication? = instance

        /**
         * Registers [application] without scheduling the backup work.
         *
         * Robolectric creates the application itself, and a test that does not want the periodic
         * backup enqueued overrides `onCreate`; this is how it then keeps the singleton in step.
         */
        @VisibleForTesting
        internal fun registerInstance(application: KeepScoreApplication) {
            instance = application
        }
    }
}
