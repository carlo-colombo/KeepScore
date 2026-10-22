package ovh.litapp.keepscore.util

import android.util.SparseArray

/**
 * Inspired by Google Guava, some helper functions for creating SparseArrays.
 *
 * @author nolan
 */
object SparseArrays {

    @JvmStatic
    fun <E> create(): SparseArray<E> = SparseArray()

    @JvmStatic
    fun <E> create(key: Int, value: E): SparseArray<E> {
        val result = SparseArray<E>()

        result.put(key, value)
        return result
    }
}
