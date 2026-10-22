package ovh.litapp.keepscore.helper

import android.content.Context
import android.widget.Toast

/**
 * Helper class for avoiding some boilerplate with the Toasts.
 *
 * @author nolan
 */
object ToastHelper {

    @JvmStatic
    fun showLong(context: Context, resId: Int, vararg args: Any?) {
        show(context, resId, Toast.LENGTH_LONG, *args)
    }

    @JvmStatic
    fun showShort(context: Context, resId: Int, vararg args: Any?) {
        show(context, resId, Toast.LENGTH_SHORT, *args)
    }

    private fun show(context: Context, resId: Int, length: Int, vararg args: Any?) {
        if (args.isEmpty()) {
            Toast.makeText(context, resId, length).show()
        } else {
            val message = String.format(context.getString(resId), *args)
            Toast.makeText(context, message, length).show()
        }
    }
}
