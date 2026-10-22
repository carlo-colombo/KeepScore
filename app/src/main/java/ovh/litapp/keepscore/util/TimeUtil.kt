package ovh.litapp.keepscore.util

object TimeUtil {

    /**
     * Format seconds into a string like this: 01:01, 02:02, etc. (i.e. h:mm:ss)
     */
    @JvmStatic
    fun formatSeconds(time: Int): String {

        val hours = Math.round(Math.floor(time * 1.0 / 3600))

        val minutesStr = Math.round(Math.floor((time * 1.0 / 60) % 60)).toString()
        val secondsStr = (time % 60).toString()

        val result = StringBuilder()

        if (hours > 0) {
            result.append(hours).append(":")
            // pad minutes to 2 digits
            result.append(if (minutesStr.length == 1) "0" else "")
        }

        return result
            .append(minutesStr)
            .append(':')
            // pad seconds to 2 digits
            .append(if (secondsStr.length == 1) "0" else "")
            .append(secondsStr)
            .toString()
    }
}
