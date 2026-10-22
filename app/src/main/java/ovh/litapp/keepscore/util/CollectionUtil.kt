package ovh.litapp.keepscore.util

import java.util.Collections

/**
 * Collection helpers. Declared as an `object` with `@JvmStatic` members and nested `fun interface`
 * function types so the remaining Java call sites (`CollectionUtil.transform(list, new Function<>() {})`,
 * `CollectionUtil.max(...)`, ...) keep the same Java-shaped API.
 */
object CollectionUtil {

    @JvmStatic
    fun <E> nullToEmpty(input: List<E>?): List<E> = input ?: emptyList()

    @JvmStatic
    fun <E, T : Comparable<T>> maxValue(list: List<E>, function: Function<E, T>): T? =
        maxValue(list, function, null)

    @JvmStatic
    fun <E, T : Comparable<T>> maxValue(list: List<E>, function: Function<E, T>, defaultValue: T?): T? {

        var max = defaultValue

        for (obj in list) {
            val value = function.apply(obj)
            if (max == null || value.compareTo(max) > 0) {
                max = value
            }
        }

        return max
    }

    @JvmStatic
    fun <E, T, K> compose(function1: Function<E, T>, function2: Function<T, K>): Function<E, K> =
        Function { obj -> function2.apply(function1.apply(obj)) }

    @JvmStatic
    fun <T> toStringFunction(): Function<T, String> = Function { obj -> obj.toString() }

    @JvmStatic
    fun stringsToLongs(list: List<String>): List<Long> {

        val result = ArrayList<Long>()

        for (str in list) {
            result.add(str.toLong())
        }

        return result
    }

    @JvmStatic
    fun stringsToInts(list: List<String>): List<Int> {

        val result = ArrayList<Int>()

        for (str in list) {
            result.add(str.toInt())
        }

        return result
    }

    @JvmStatic
    fun <T> reversedCopy(list: List<T>): List<T> {
        val result = ArrayList<T>(list.size)

        for (i in list.size - 1 downTo 0) {
            result.add(list[i])
        }

        return result
    }

    @JvmStatic
    fun <T> filter(list: List<T>, predicate: Predicate<T>): List<T> {

        val filtered = ArrayList<T>()

        for (obj in list) {
            if (predicate.apply(obj)) {
                filtered.add(obj)
            }
        }

        return filtered
    }

    @JvmStatic
    fun <E, T> transformWithIndices(list: List<E>, function: FunctionWithIndex<E, T>): List<T> {
        val result = ArrayList<T>()

        for (i in list.indices) {
            result.add(function.apply(list[i], i))
        }

        return result
    }

    @JvmStatic
    fun <E, T> transform(list: Collection<E>, function: Function<E, T>): List<T> {
        val result = ArrayList<T>()

        for (obj in list) {
            result.add(function.apply(obj))
        }

        return result
    }

    @JvmStatic
    fun sum(list: List<Int>): Int {
        var sum = 0
        for (element in list) {
            sum += element
        }
        return sum
    }

    @JvmStatic
    fun <E> sum(list: List<E>, function: Function<E, Int>): Int {
        var sum = 0
        for (element in list) {
            sum += function.apply(element)
        }
        return sum
    }

    /**
     * Same as `max()`, but considers that there may be ties, and presents all values that tie.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <E, T : Number> maxWithTies(list: List<E>, function: Function<E, T>): List<E> {
        var max: T? = null
        for (element in list) {
            val value = function.apply(element)
            if (max == null || (value as Comparable<T>).compareTo(max) > 0) {
                max = value
            }
        }

        val result = ArrayList<E>()
        for (element in list) {
            val value = function.apply(element)
            if ((value as Comparable<T>).compareTo(max!!) == 0) {
                result.add(element)
            }
        }

        return result
    }

    /**
     * Same as `min()`, but considers that there may be ties, and presents all values that tie.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <E, T : Number> minWithTies(list: List<E>, function: Function<E, T>): List<E> {
        var min: T? = null
        for (element in list) {
            val value = function.apply(element)
            if (min == null || (value as Comparable<T>).compareTo(min) < 0) {
                min = value
            }
        }

        val result = ArrayList<E>()
        for (element in list) {
            val value = function.apply(element)
            if ((value as Comparable<T>).compareTo(min!!) == 0) {
                result.add(element)
            }
        }

        return result
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <E, T : Number> max(list: List<E>, function: Function<E, T>): T? {
        var max: T? = null
        for (element in list) {
            val value = function.apply(element)
            if (max == null || (value as Comparable<T>).compareTo(max) > 0) {
                max = value
            }
        }
        return max
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> toArray(list: List<T>, clazz: Class<T>): Array<T> {
        val result = java.lang.reflect.Array.newInstance(clazz, list.size) as Array<T>
        for (i in list.indices) {
            result[i] = list[i]
        }
        return result
    }

    @JvmStatic
    fun <T> all(list: Collection<T>, predicate: Predicate<T>): Boolean {
        for (item in list) {
            if (!predicate.apply(item)) {
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun <T> any(list: Collection<T>, predicate: Predicate<T>): Boolean {
        for (item in list) {
            if (predicate.apply(item)) {
                return true
            }
        }
        return false
    }

    fun interface FunctionWithIndex<E, T> {

        fun apply(obj: E, index: Int): T
    }

    fun interface Function<E, T> {

        fun apply(obj: E): T
    }

    fun interface Predicate<T> {

        fun apply(obj: T): Boolean
    }
}
