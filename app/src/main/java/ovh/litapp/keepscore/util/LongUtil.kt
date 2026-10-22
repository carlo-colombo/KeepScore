package ovh.litapp.keepscore.util

object LongUtil {

    /**
     * Borrowed from Google Guava!
     *
     * Compares the two specified `long` values. The sign of the value
     * returned is the same as that of `((Long) a).compareTo(b)`.
     *
     * @param a the first `long` to compare
     * @param b the second `long` to compare
     * @return a negative value if `a` is less than `b`; a positive
     * value if `a` is greater than `b`; or zero if they are equal
     */
    @JvmStatic
    fun compare(a: Long, b: Long): Int = if (a < b) -1 else if (a > b) 1 else 0
}
