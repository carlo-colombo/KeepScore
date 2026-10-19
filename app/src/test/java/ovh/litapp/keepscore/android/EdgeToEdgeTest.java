package ovh.litapp.keepscore.android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EdgeToEdgeTest {

    @Test
    public void topPaddingIncludesActionBarOnlyWhenRequested() {
        int statusBarInset = 48;
        int actionBarHeight = 56;

        assertEquals(104, EdgeToEdge.calculateTopPadding(statusBarInset, actionBarHeight, true));
        assertEquals(48, EdgeToEdge.calculateTopPadding(statusBarInset, actionBarHeight, false));
    }
}
