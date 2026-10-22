package ovh.litapp.keepscore.db

import android.text.TextUtils
import ovh.litapp.keepscore.util.CollectionUtil
import ovh.litapp.keepscore.util.Pair
import ovh.litapp.keepscore.util.StringUtil

/**
 * Represents an amount by which a score changed, and at one time the change was effected.
 *
 * @author nolan
 */
data class Delta(var timestamp: Long, var value: Int) {

    /** Undoes the join of [toJoinedStrings]: comma-separated values and their timestamps. */
    companion object {

        @JvmStatic
        fun fromJoinedStrings(scoresStr: String, timestampsStr: String): MutableList<Delta> {
            val scores = StringUtil.split(scoresStr, ',')
            val timestamps = StringUtil.split(timestampsStr, ',')

            val result = ArrayList<Delta>(scores.size)

            for (i in scores.indices) {
                val score = scores[i].toInt()
                // the string could be empty, i.e. the scores were recorded before we started
                // recording timestamps
                val timestamp = if (i < timestamps.size) timestamps[i].toLong() else 0L
                result.add(Delta(timestamp, score))
            }
            return result
        }

        @JvmStatic
        fun fromJoinedScores(scoresStr: String): MutableList<Delta> {
            val scores = CollectionUtil.stringsToInts(StringUtil.split(scoresStr, ','))
            return CollectionUtil.transform(scores, FROM_VALUE).toMutableList()
        }

        @JvmStatic
        fun toJoinedStrings(deltas: List<Delta>?): Pair<String, String> {
            val entries = deltas.orEmpty()

            val values = TextUtils.join(",", CollectionUtil.transform(entries, GET_VALUE))
            val timestamps = TextUtils.join(",", CollectionUtil.transform(entries, GET_TIMESTAMP))

            return Pair.create(values, timestamps)
        }

        @JvmField
        val GET_VALUE: CollectionUtil.Function<Delta, Int> =
            CollectionUtil.Function { obj -> obj.value }

        @JvmField
        val GET_TIMESTAMP: CollectionUtil.Function<Delta, Long> =
            CollectionUtil.Function { obj -> obj.timestamp }

        @JvmField
        val FROM_VALUE: CollectionUtil.Function<Int, Delta> =
            CollectionUtil.Function { value -> Delta(0L, value) }
    }
}
