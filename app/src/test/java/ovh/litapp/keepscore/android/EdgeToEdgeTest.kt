package ovh.litapp.keepscore.android

import org.junit.Assert.assertEquals
import org.junit.Test

class EdgeToEdgeTest {

    @Test
    fun topPaddingIncludesActionBarOnlyWhenRequested() {
        val statusBarInset = 48
        val actionBarHeight = 56

        assertEquals(104, EdgeToEdge.calculateTopPadding(statusBarInset, actionBarHeight, true))
        assertEquals(48, EdgeToEdge.calculateTopPadding(statusBarInset, actionBarHeight, false))
    }
}
