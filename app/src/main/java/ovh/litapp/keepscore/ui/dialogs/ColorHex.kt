package ovh.litapp.keepscore.ui.dialogs

import java.util.Locale

/** The `#RRGGBB` form of [color], which ignores the alpha channel the legacy picker did not edit. */
internal fun hexColorString(color: Int): String = String.format(Locale.US, "#%06X", 0xFFFFFF and color)

/**
 * The colour of a hex string like `#RRGGBB` or `#AARRGGBB`, or null when [text] is not one.
 *
 * Only hex digits are accepted, so the `+` and `-` a `Long` parser would take are rejected.
 */
internal fun parseHexColor(text: String): Int? {
    val trimmed = text.trim()
    if (!trimmed.startsWith("#") || (trimmed.length != 7 && trimmed.length != 9)) {
        return null
    }
    val digits = trimmed.substring(1)
    if (!digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return null
    }
    val value = digits.toLong(16)
    return if (digits.length == 6) (0xFF000000L or value).toInt() else value.toInt()
}
