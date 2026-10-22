package ovh.litapp.keepscore.util

import java.util.Comparator

import ovh.litapp.keepscore.util.CollectionUtil.Function

/**
 * As the old joke goes:
 *
 * Q) Why is Java such a manly language?
 *
 * A) Because it forces every developer to grow a Pair.
 *
 * @author nolan
 */
class Pair<E, T> private constructor(val first: E, val second: T) {

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + (first?.hashCode() ?: 0)
        result = 31 * result + (second?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Pair<*, *>) return false
        if (first != other.first) return false
        return second == other.second
    }

    companion object {

        @JvmStatic
        fun <E, T> create(first: E, second: T): Pair<E, T> = Pair(first, second)

        @JvmStatic
        fun <E : Comparable<E>, T> byFirst(): Comparator<Pair<E, T>> =
            Comparator { lhs, rhs -> lhs.first.compareTo(rhs.first) }

        @JvmStatic
        fun <E, T : Comparable<T>> bySecond(): Comparator<Pair<E, T>> =
            Comparator { lhs, rhs -> lhs.second.compareTo(rhs.second) }

        @JvmStatic
        fun <E, T> getFirstFunction(): Function<Pair<E, T>, E> = Function { obj -> obj.first }

        @JvmStatic
        fun <E, T> getSecondFunction(): Function<Pair<E, T>, T> = Function { obj -> obj.second }
    }
}
