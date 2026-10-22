package ovh.litapp.keepscore.util

class StopWatch(private val name: String) {

    private var startTime: Long = 0

    init {
        if (UtilLogger.DEBUG_MODE) {
            startTime = System.currentTimeMillis()
        }
    }

    fun log(log: UtilLogger) {
        if (UtilLogger.DEBUG_MODE) {
            log.d("%s took %d ms", name, System.currentTimeMillis() - startTime)
        }
    }
}
