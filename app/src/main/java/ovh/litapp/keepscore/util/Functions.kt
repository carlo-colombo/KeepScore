package ovh.litapp.keepscore.util

import java.util.Date
import java.util.GregorianCalendar

import ovh.litapp.keepscore.db.PlayerScore

import ovh.litapp.keepscore.util.CollectionUtil.Function

/**
 * Reusable functions over the app's models. The constants are `@JvmField` values because Java call
 * sites read them as static fields (`Functions.PLAYER_SCORE_TO_HISTORY_SIZE`, `Functions.TODAY_START`).
 */
object Functions {

    @JvmStatic
    fun <X, Y, Z> chain(function1: Function<X, Y>, function2: Function<Y, Z>): Function<X, Z> =
        Function { obj -> function2.apply(function1.apply(obj)) }

    @JvmField
    val PLAYER_SCORE_TO_SCORE: Function<PlayerScore, Long> = Function { obj -> obj.score }

    @JvmField
    val PLAYER_SCORE_TO_HISTORY_SIZE: Function<PlayerScore, Int> = Function { obj -> obj.history.size }

    @JvmField
    val INTEGER_TO_LENGTH_WITH_SIGN: Function<Int, Int> =
        Function { obj -> IntegerUtil.toCharSequenceWithSign(obj).length }

    @JvmField
    val TODAY_START: Function<Date, Date> = Function { currentDate ->
        getClosestMidnightBefore(currentDate, 0L)
    }

    @JvmField
    val YESTERDAY_START: Function<Date, Date> = Function { currentDate ->
        getClosestMidnightBefore(currentDate, 1000L * 60 * 60 * 24)
    }

    @JvmField
    val DAY_BEFORE_YESTERDAY_START: Function<Date, Date> = Function { currentDate ->
        getClosestMidnightBefore(currentDate, 1000L * 60 * 60 * 24 * 2)
    }

    @JvmField
    val ONE_WEEK_AGO_START: Function<Date, Date> = Function { currentDate ->
        getClosestMidnightBefore(currentDate, 1000L * 60 * 60 * 24 * 7)
    }

    @JvmField
    val ONE_MONTH_AGO_START: Function<Date, Date> = Function { currentDate ->
        getClosestMidnightBefore(currentDate, 1000L * 60 * 60 * 24 * 30)
    }

    @JvmField
    val ONE_YEAR_AGO_START: Function<Date, Date> = Function { currentDate ->
        getClosestMidnightBefore(currentDate, 1000L * 60 * 60 * 24 * 365)
    }

    @JvmField
    val NOW: Function<Date, Date> = Function { currentDate ->
        val date = Date()
        date.time = currentDate.time
        date
    }

    @JvmField
    val THE_EPOCH: Function<Date, Date> = Function { _ -> Date(0L) }

    private fun getClosestMidnightBefore(date: Date, duration: Long): Date {

        val newDate = GregorianCalendar()
        val dateMinusDuration = Date(date.time - duration)
        newDate.time = dateMinusDuration

        newDate.set(GregorianCalendar.HOUR_OF_DAY, 0)
        newDate.set(GregorianCalendar.MINUTE, 0)
        newDate.set(GregorianCalendar.SECOND, 0)
        newDate.set(GregorianCalendar.MILLISECOND, 0)

        return newDate.time
    }
}
