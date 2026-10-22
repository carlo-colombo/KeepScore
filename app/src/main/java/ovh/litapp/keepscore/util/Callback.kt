package ovh.litapp.keepscore.util

/**
 * Simple callback object.  Makes code prettier.
 *
 * Declared as a `fun interface` so both the remaining Java anonymous classes and Kotlin lambda
 * call sites (`Callback { input -> ... }`) keep working.
 */
fun interface Callback<E> {

    fun onCallback(input: E)
}
