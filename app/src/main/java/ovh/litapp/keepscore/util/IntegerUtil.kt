package ovh.litapp.keepscore.util

import android.text.TextUtils

object IntegerUtil {

    /**
     * calls toString on the int, but also adds a "+" if it's nonnegative
     */
    @JvmStatic
    fun toCharSequenceWithSign(i: Int): CharSequence =
        if (i >= 0) StringBuilder("+").append(i) else i.toString()

    @JvmStatic
    fun validInt(integer: CharSequence?): Boolean {
        if (!TextUtils.isEmpty(integer)) {
            try {
                integer.toString().toInt()
                return true
            } catch (ignored: NumberFormatException) {
            }
        }
        return false
    }

    @JvmStatic
    fun parseIntOrZero(charSequence: CharSequence?): Int {
        if (!TextUtils.isEmpty(charSequence)) {
            try {
                return charSequence.toString().toInt()
            } catch (ignored: NumberFormatException) {
            }
        }
        return 0
    }
}
