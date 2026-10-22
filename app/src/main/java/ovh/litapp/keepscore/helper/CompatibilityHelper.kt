package ovh.litapp.keepscore.helper

import android.app.Activity
import ovh.litapp.keepscore.util.UtilLogger

/**
 * Transition helpers.
 *
 * The API-5 reflection shim this used to contain is gone: `overridePendingTransition` has existed
 * since API 5 and the app now requires API 34.
 */
object CompatibilityHelper {

    private val log = UtilLogger(CompatibilityHelper::class.java)

    @JvmStatic
    fun overridePendingTransition(activity: Activity, enterAnim: Int, exitAnim: Int) {
        try {
            activity.overridePendingTransition(enterAnim, exitAnim)
        } catch (e: IllegalArgumentException) {
            log.d(e, "")
        }
    }
}
