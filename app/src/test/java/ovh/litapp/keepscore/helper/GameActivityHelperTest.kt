package ovh.litapp.keepscore.helper

import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.LandscapeGameActivity
import ovh.litapp.keepscore.PortraitGameActivity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameActivityHelperTest {

    @Test
    fun eightPlayersUseLandscapeActivityAndLargerGamesUsePortraitActivity() {
        assertSame(LandscapeGameActivity::class.java, GameActivityHelper.getGameActivityClass(8))
        assertSame(PortraitGameActivity::class.java, GameActivityHelper.getGameActivityClass(9))
        assertSame(PortraitGameActivity::class.java, GameActivityHelper.getGameActivityClass(30))
    }
}
