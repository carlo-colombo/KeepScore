package ovh.litapp.keepscore.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

import ovh.litapp.keepscore.util.CollectionUtil.Function
import ovh.litapp.keepscore.util.CollectionUtil.FunctionWithIndex
import ovh.litapp.keepscore.util.CollectionUtil.Predicate

/**
 * Tests for [CollectionUtil], which the remaining Java code calls through its static members
 * (`transform`, `maxWithTies`, ...) and nested function types.
 */
class CollectionUtilTest {

    @Test
    fun transformAppliesTheFunctionToEveryElement() {
        assertEquals(
            listOf(1, 2, 3),
            CollectionUtil.transform(listOf("a", "bb", "ccc"), Function<String, Int> { it.length }),
        )
    }

    @Test
    fun transformWithIndicesPassesTheIndex() {
        assertEquals(
            listOf("0:a", "1:b"),
            CollectionUtil.transformWithIndices(
                listOf("a", "b"),
                FunctionWithIndex<String, String> { obj, index -> "$index:$obj" },
            ),
        )
    }

    @Test
    fun maxReturnsTheLargestValueOrNullForAnEmptyList() {
        assertEquals(12L, maxScore(players))
        assertNull(maxScore(emptyList()))
    }

    @Test
    fun maxWithTiesReturnsEveryTiedPlayer() {
        val tied = listOf(Player("Alice", 12L), Player("Bob", 3L), Player("Carol", 12L))

        assertEquals(listOf("Alice", "Carol"), maxScoringPlayers(tied).map { it.name })
    }

    @Test
    fun minWithTiesReturnsEveryTiedPlayer() {
        val tied = listOf(Player("Alice", 12L), Player("Bob", 3L), Player("Carol", 3L))

        assertEquals(listOf("Bob", "Carol"), minScoringPlayers(tied).map { it.name })
    }

    @Test
    fun nullToEmptyReplacesNullWithAnEmptyList() {
        assertEquals(emptyList<String>(), CollectionUtil.nullToEmpty<String?>(null))
        assertEquals(listOf("a"), CollectionUtil.nullToEmpty(listOf("a")))
    }

    @Test
    fun stringsToIntsAndLongsParseEveryElement() {
        assertEquals(listOf(1, -2), CollectionUtil.stringsToInts(listOf("1", "-2")))
        assertEquals(listOf(1L, -2L), CollectionUtil.stringsToLongs(listOf("1", "-2")))
    }

    @Test
    fun reversedCopyReversesWithoutTouchingTheInput() {
        val input = listOf("a", "b", "c")

        assertEquals(listOf("c", "b", "a"), CollectionUtil.reversedCopy(input))
        assertEquals(listOf("a", "b", "c"), input)
    }

    @Test
    fun filterAllAndAnyUseThePredicate() {
        val positive = Predicate<Int> { it > 0 }

        assertEquals(listOf(1, 2), CollectionUtil.filter(listOf(1, -1, 2), positive))
        assertTrue(CollectionUtil.all(listOf(1, 2), positive))
        assertFalse(CollectionUtil.all(listOf(1, -2), positive))
        assertTrue(CollectionUtil.any(listOf(-1, 2), positive))
        assertFalse(CollectionUtil.any(listOf(-1, -2), positive))
    }

    @Test
    fun sumAddsEveryElementOrFunctionResult() {
        assertEquals(6, CollectionUtil.sum(listOf(1, 2, 3)))
        assertEquals(8, CollectionUtil.sum(players, Function<Player, Int> { it.name.length }))
    }

    @Test
    fun maxValueUsesTheDefaultWhenTheListIsEmpty() {
        assertEquals(0, CollectionUtil.maxValue(emptyList(), Function<Player, Int> { it.name.length }, 0))
    }

    private fun maxScore(list: List<Player>): Long? =
        CollectionUtil.max(list, Function<Player, Long> { it.score })

    private fun maxScoringPlayers(list: List<Player>): List<Player> =
        CollectionUtil.maxWithTies(list, Function<Player, Long> { it.score })

    private fun minScoringPlayers(list: List<Player>): List<Player> =
        CollectionUtil.minWithTies(list, Function<Player, Long> { it.score })

    private data class Player(val name: String, val score: Long)

    private companion object {
        private val players = listOf(Player("Alice", 12L), Player("Bob", 3L))
    }
}
