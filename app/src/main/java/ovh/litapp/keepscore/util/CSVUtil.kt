package ovh.litapp.keepscore.util

import android.text.TextUtils

import ovh.litapp.keepscore.util.CollectionUtil.Function

/**
 * Write CSV-style files, as defined by http://en.wikipedia.org/wiki/Comma-separated_values
 *
 * @author nolan
 */
object CSVUtil {

    // Elements are nullable because callers build rows where a missing cell is a null entry.
    private val CONVERT_VALUE: Function<String?, String> = Function { convertValue(it) }

    /**
     * Convert a list of values into a valid CSV line.
     */
    @JvmStatic
    fun convertToLine(values: List<String?>): String =
        TextUtils.join(",", CollectionUtil.transform(values, CONVERT_VALUE)) + "\r\n"

    private fun convertValue(obj: Any?): String {
        if (obj == null) {
            return ""
        }
        val str = obj.toString()
        if (TextUtils.isEmpty(str)) {
            return ""
        }

        return '"' + escape(str) + '"'
    }

    private fun escape(input: String): String = input.replace("\"", "\"\"").replace("\r", "")
}
