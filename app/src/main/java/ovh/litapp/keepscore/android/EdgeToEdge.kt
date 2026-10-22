package ovh.litapp.keepscore.android

import android.app.Activity
import android.util.TypedValue
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Window inset handling for the View-based screens.
 *
 * targetSdk 35+ enforces edge-to-edge. The screens that still use the AppCompat ActionBar with plain
 * content layouts are not fitted below the action bar by the system on Android 16, so the first
 * content row was drawn over it. Pad the content root down by the status-bar inset plus the action
 * bar height, and in from the sides, so the layout starts below the action bar. Activities that hide
 * their action bar after creation must opt out explicitly because insets are applied before
 * [Activity.onResume] and cannot infer that later visibility change.
 */
object EdgeToEdge {

    /**
     * Applies system bar and display cutout insets, optionally including action bar height.
     *
     * @param includeActionBar false for activities that hide the action bar after creation
     */
    @JvmStatic
    @JvmOverloads
    fun apply(activity: Activity, includeActionBar: Boolean = true) {
        val actionBarHeight = if (includeActionBar) resolveActionBarHeight(activity) else 0
        val content = activity.findViewById<android.view.View>(android.R.id.content) ?: return

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val bars: Insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            view.setPadding(
                bars.left,
                calculateTopPadding(bars.top, actionBarHeight, includeActionBar),
                bars.right,
                bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    @JvmStatic
    fun calculateTopPadding(
        statusBarInset: Int,
        actionBarHeight: Int,
        includeActionBar: Boolean,
    ): Int = statusBarInset + if (includeActionBar) actionBarHeight else 0

    private fun resolveActionBarHeight(activity: Activity): Int {
        val value = TypedValue()
        return if (activity.theme.resolveAttribute(androidx.appcompat.R.attr.actionBarSize, value, true)) {
            TypedValue.complexToDimensionPixelSize(
                value.data,
                activity.resources.displayMetrics,
            )
        } else {
            0
        }
    }
}
