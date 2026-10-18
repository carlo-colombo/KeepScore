package ovh.litapp.keepscore

import android.content.Intent
import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor

/**
 * Tests for the result [GameActivity] reads back from the "Edit Players" screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrganizePlayersActivityTest {

    private fun player(name: String, score: Long, number: Int) = PlayerScore().apply {
        this.name = name
        this.score = score
        playerNumber = number
        playerColor = PlayerColor.BUILT_INS[number % PlayerColor.BUILT_INS.size]
        history = arrayListOf(Delta(0L, score.toInt()))
    }

    /** Reads the player list back the way a receiving activity would, through a parcel. */
    private fun Intent.playersFromParcel(): List<PlayerScore>? {
        val parcel = Parcel.obtain()
        try {
            writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return Intent.CREATOR.createFromParcel(parcel).getParcelableArrayListExtra(
                OrganizePlayersActivity.EXTRA_PLAYER_SCORES,
                PlayerScore::class.java,
            )
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun theResultCarriesThePlayersInOrderUnderTheExtraGameActivityReads() {
        val players = listOf(player("a", 30, 0), player("b", 10, 1), player("c", 20, 2))

        val readBack = playerScoresResult(players).playersFromParcel()

        assertEquals(listOf("a", "b", "c"), readBack?.map { it.name })
        assertEquals(listOf(0, 1, 2), readBack?.map { it.playerNumber })
        assertEquals(listOf(30L, 10L, 20L), readBack?.map { it.score })
        assertEquals(players.map { it.playerColor }, readBack?.map { it.playerColor })
    }
}
