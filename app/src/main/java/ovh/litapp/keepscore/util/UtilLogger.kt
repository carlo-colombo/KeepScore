package ovh.litapp.keepscore.util

import java.util.Arrays

import android.util.Log

/**
 * Easier way to interact with logcat.
 *
 * @author nolan
 */
class UtilLogger(private val tag: String) {

    constructor(clazz: Class<*>) : this(clazz.simpleName)

    fun i(format: String, vararg more: Any?) {
        Log.i(tag, String.format(format, *more))
    }

    fun i(e: Exception, format: String, vararg more: Any?) {
        Log.i(tag, String.format(format, *more), e)
    }

    fun w(e: Exception, format: String, vararg more: Any?) {
        Log.w(tag, String.format(format, *more), e)
    }

    fun w(format: String, vararg more: Any?) {
        Log.w(tag, String.format(format, *more))
    }

    fun e(format: String, vararg more: Any?) {
        Log.e(tag, String.format(format, *more))
    }

    fun e(e: Exception, format: String, vararg more: Any?) {
        Log.e(tag, String.format(format, *more), e)
    }

    fun v(format: String, vararg more: Any?) {
        v(null, format, *more)
    }

    fun v(e: Exception?, format: String, vararg more: Any?) {
        if (DEBUG_MODE) {
            formatArgs(more)
            if (e != null) {
                Log.v(tag, String.format(format, *more), e)
            } else {
                Log.v(tag, String.format(format, *more))
            }
        }
    }

    fun d(format: String, vararg more: Any?) {
        d(null, format, *more)
    }

    fun d(e: Exception?, format: String, vararg more: Any?) {
        if (DEBUG_MODE) {
            formatArgs(more)
            if (e != null) {
                Log.d(tag, String.format(format, *more), e)
            } else {
                Log.d(tag, String.format(format, *more))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun formatArgs(more: Array<out Any?>) {
        val args = more as Array<Any?>
        for (i in args.indices) {
            if (args[i] is IntArray) {
                args[i] = Arrays.toString(args[i] as IntArray)
            } else if (args[i] is Array<*>) {
                args[i] = Arrays.toString(args[i] as Array<*>)
            }
        }
    }

    companion object {

        @JvmField
        val DEBUG_MODE: Boolean = false
    }
}
