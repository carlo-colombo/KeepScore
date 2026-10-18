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
 * height, and in from the sides, so the layout starts below the action bar.
 */
public final class EdgeToEdge {

    private EdgeToEdge() {
    }

    public static void apply(Activity activity) {
        final int actionBarHeight = resolveActionBarHeight(activity);
        View content = activity.findViewById(android.R.id.content);
        if (content == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top + actionBarHeight, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
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
