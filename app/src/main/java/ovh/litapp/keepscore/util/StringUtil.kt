package ovh.litapp.keepscore.util

import android.text.SpannableString
import android.text.Spanned
import android.text.SpannedString
import android.text.TextUtils

import java.util.LinkedList

import ovh.litapp.keepscore.util.CollectionUtil.Function

object StringUtil {

    @JvmStatic
    fun isEmptyOrWhitespace(str: String?): Boolean {
        val value = str ?: return true
        if (value.isEmpty()) {
            return true
        }
        for (element in value) {
            if (!Character.isWhitespace(element)) {
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun split(str: String?, delimiter: String): List<String> {

        val value = str ?: return emptyList()

        if (value.isEmpty()) {
            return emptyList()
        }

        val split = LinkedList<String>()

        var index = value.indexOf(delimiter)
        var lastIndex = 0
        while (index != -1) {

            split.add(value.substring(lastIndex, index))

            lastIndex = index + delimiter.length

            index = value.indexOf(delimiter, lastIndex)
        }

        // add the final string after the last delimiter
        split.add(value.substring(lastIndex, value.length))

        return ArrayList(split)
    }

    @JvmStatic
    fun split(str: String?, delimiter: Char): List<String> {

        val value = str ?: return emptyList()

        if (value.isEmpty()) {
            return emptyList()
        }

        val split = LinkedList<String>()

        var index = value.indexOf(delimiter)
        var lastIndex = 0
        while (index != -1) {

            split.add(value.substring(lastIndex, index))

            lastIndex = index + 1

            index = value.indexOf(delimiter, lastIndex)
        }

        // add the final string after the last delimiter
        split.add(value.substring(lastIndex, value.length))

        return ArrayList(split)
    }

    /**
     * Returns a CharSequence concatenating the specified CharSequences using the specified delimiter,
     * retaining their spans if any.
     *
     * This is mostly borrowed from TextUtils.concat();
     */
    @JvmStatic
    fun joinSpannables(delimiter: CharSequence, vararg text: CharSequence): CharSequence {
        if (text.isEmpty()) {
            return ""
        }

        if (text.size == 1) {
            return text[0]
        }

        var spanned = false
        for (element in text) {
            if (element is Spanned) {
                spanned = true
                break
            }
        }

        val sb = StringBuilder()
        for (i in text.indices) {
            if (i > 0) {
                sb.append(delimiter)
            }
            sb.append(text[i])
        }

        if (!spanned) {
            return sb.toString()
        }

        val ss = SpannableString(sb)
        var off = 0
        for (element in text) {
            val len = element.length

            if (element is Spanned) {
                TextUtils.copySpansFrom(element, 0, len, Any::class.java, ss, off)
            }

            off += len + delimiter.length
        }

        return SpannedString(ss)
    }

    @JvmStatic
    fun nullToEmpty(str: CharSequence?): String = str?.toString() ?: ""

    /**
     * find the nth index of a char c in a string.  returns -1 if there aren't that many chars in the string
     */
    @JvmStatic
    fun getNthIndexOf(c: Char, str: String, n: Int): Int {

        if (n < 1) {
            throw IllegalArgumentException("n must be >= 1: $n")
        }

        var index = str.indexOf(c)
        var count = 0
        while (index != -1 && ++count < n) {
            index = str.indexOf(c, index + 1)
        }

        return index
    }

    /**
     * Pad a string on the left until it reaches the designated height
     */
    @JvmStatic
    fun padLeft(str: CharSequence, padding: Char, upTo: Int): CharSequence {

        val stringBuilder = if (str is StringBuilder) str else StringBuilder(str)

        while (stringBuilder.length < upTo) {
            stringBuilder.insert(0, padding)
        }

        return stringBuilder
    }

    @JvmStatic
    fun lengthFunction(): Function<String, Int> = Function { obj -> obj.length }

    @JvmStatic
    fun emptyToNull(name: CharSequence?): String? =
        if (TextUtils.isEmpty(name)) null else name.toString()

    @JvmStatic
    fun count(str: String, substring: String): Int {
        var index = 0
        var count = 0
        while (index != -1) {
            index = str.indexOf(substring, index)
            if (index != -1) {
                count++
                index += substring.length
            }
        }
        return count
    }
}
