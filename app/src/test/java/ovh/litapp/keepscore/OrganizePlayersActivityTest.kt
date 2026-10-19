package ovh.litapp.keepscore

import android.content.Intent
import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        history = arrayListOf(
            Delta(1_700_000_000L + number, score.toInt()),
            Delta(1_700_000_100L + number, -number - 1),
        )
        lastUpdate = 1_700_000_200L + number
    }

    /** Copies the result Intent through a real Android parcel round-trip. */
    private fun Intent.parcelRoundTrip(): Intent {
        val parcel = Parcel.obtain()
        try {
            writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return Intent.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun resultIntentParcelPreservesPlayerFieldsInOrderUnderTheExpectedExtraKey() {
        val players = listOf(player("a", 30, 0), player("b", 10, 1), player("c", 20, 2))
        val result = playerScoresResult(players)

        assertEquals("playerScores", OrganizePlayersActivity.EXTRA_PLAYER_SCORES)
        val parcelledIntent = result.parcelRoundTrip()
        assertTrue(parcelledIntent.hasExtra("playerScores"))
        val parcelledResult = parcelledIntent.getParcelableArrayListExtra(
            OrganizePlayersActivity.EXTRA_PLAYER_SCORES,
            PlayerScore::class.java,
        )
        assertEquals(listOf("a", "b", "c"), parcelledResult?.map { it.name })
        assertEquals(listOf(0, 1, 2), parcelledResult?.map { it.playerNumber })
        assertEquals(listOf(30L, 10L, 20L), parcelledResult?.map { it.score })
        assertEquals(players.map { it.history }, parcelledResult?.map { it.history })
        assertEquals(players.map { it.lastUpdate }, parcelledResult?.map { it.lastUpdate })
        assertEquals(players.map { it.playerColor }, parcelledResult?.map { it.playerColor })
    }
}
