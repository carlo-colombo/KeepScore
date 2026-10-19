package ovh.litapp.keepscore.android;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Window inset handling for the legacy View hierarchy.
 *
 * <p>targetSdk 35+ enforces edge-to-edge. This app uses the AppCompat ActionBar
 * (not a Toolbar) with plain content layouts, and on Android 16 the system does
 * not fit the content below the action bar, so the first content row was drawn
 * over it. Pad the content root down by the status-bar inset plus the action bar
 * height, and in from the sides, so the layout starts below the action bar. Activities that hide
 * their action bar after creation must opt out explicitly because insets are applied before
 * {@code onResume()} and cannot infer that later visibility change.
 */
public final class EdgeToEdge {

    private EdgeToEdge() {
    }

    public static void apply(Activity activity) {
        apply(activity, true);
    }

    /**
     * Applies system bar and display cutout insets, optionally including action bar height.
     *
     * @param includeActionBar false for activities that hide the action bar after creation
     */
    public static void apply(Activity activity, boolean includeActionBar) {
        final int actionBarHeight = includeActionBar ? resolveActionBarHeight(activity) : 0;
        View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, calculateTopPadding(bars.top, actionBarHeight, includeActionBar),
                    bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    static int calculateTopPadding(int statusBarInset, int actionBarHeight, boolean includeActionBar) {
        return statusBarInset + (includeActionBar ? actionBarHeight : 0);
    }

    private static int resolveActionBarHeight(Activity activity) {
        TypedValue value = new TypedValue();
        if (activity.getTheme().resolveAttribute(androidx.appcompat.R.attr.actionBarSize, value, true)) {
            return TypedValue.complexToDimensionPixelSize(
                    value.data, activity.getResources().getDisplayMetrics());
        }
        return 0;
    }
}
