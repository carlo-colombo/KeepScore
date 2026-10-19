package ovh.litapp.keepscore.helper;

import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import ovh.litapp.keepscore.LandscapeGameActivity;
import ovh.litapp.keepscore.PortraitGameActivity;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameActivityHelperTest {

    @Test
    public void eightPlayersUseLandscapeActivityAndLargerGamesUsePortraitActivity() {
        assertSame(LandscapeGameActivity.class, GameActivityHelper.getGameActivityClass(8));
        assertSame(PortraitGameActivity.class, GameActivityHelper.getGameActivityClass(9));
        assertSame(PortraitGameActivity.class, GameActivityHelper.getGameActivityClass(30));
    }
}
