package ovh.litapp.keepscore.ui.organize

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random
import ovh.litapp.keepscore.OrganizePlayersActivity
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor

/**
 * One player of the "Edit Players" screen.
 *
 * [playerScore] is the instance the game handed in, so the activity result stays a list of those
 * objects; the other properties are the display state the list reads and every change is written
 * straight through to it. [key] survives reordering, which is what the lazy list and the
 * reorderable state key their items by.
 */
internal class OrganizePlayer internal constructor(
    val playerScore: PlayerScore,
    val key: Long,
) {

    var name: String by mutableStateOf(playerScore.name.orEmpty())
        private set

    var playerNumber: Int by mutableIntStateOf(playerScore.playerNumber)
        private set

    var playerColor: PlayerColor by mutableStateOf(playerScore.playerColor ?: PlayerColor.BUILT_INS[0])
        private set

    internal fun rename(newName: String) {
        name = newName
        playerScore.name = newName
    }

    internal fun recolor(newColor: PlayerColor) {
        playerColor = newColor
        playerScore.playerColor = newColor
    }

    internal fun renumber(newNumber: Int) {
        playerNumber = newNumber
        playerScore.playerNumber = newNumber
    }
}

/**
 * The players shown by [OrganizePlayersScreen], in display order.
 *
 * Every operation that changes the order renumbers the players, because the game screens address
 * them by [PlayerScore.playerNumber]. The [PlayerScore] instances are the caller's, so
 * [toResultList] hands back exactly what the game expects.
 */
internal class OrganizePlayersModel(initialPlayers: List<PlayerScore>) {

    private var nextKey = 0L
    private var sortsDescending = false

    val players = mutableStateListOf<OrganizePlayer>()

    /** Players deleted since the screen opened that had a score history, in deletion order. */
    val deletedPlayersWithHistory = mutableListOf<PlayerScore>()

    /** Adding is not offered once the game screens can no longer lay the players out. */
    val canAddPlayer: Boolean
        get() = players.size < OrganizePlayersActivity.MAX_NUM_PLAYERS

    /** The game screens need at least [OrganizePlayersActivity.MIN_NUM_PLAYERS] players. */
    val canDeletePlayer: Boolean
        get() = players.size > OrganizePlayersActivity.MIN_NUM_PLAYERS

    init {
        initialPlayers.forEach { players.add(OrganizePlayer(it, nextKey++)) }
    }

    /**
     * Appends a player with the default score, an empty history and the next built-in colour, the
     * way the legacy "Add Player" action did.
     */
    fun addPlayer(name: String, initialScore: Long) {
        val number = players.size
        val playerScore = PlayerScore()
        playerScore.id = -1
        playerScore.name = name
        playerScore.playerNumber = number
        playerScore.score = initialScore
        playerScore.history = ArrayList()
        playerScore.playerColor = PlayerColor.BUILT_INS[number % PlayerColor.BUILT_INS.size]
        players.add(OrganizePlayer(playerScore, nextKey++))
    }

    fun deletePlayer(player: OrganizePlayer) {
        if (!canDeletePlayer) {
            return
        }
        players.remove(player)
        if (player.playerScore.history?.isNotEmpty() == true) {
            deletedPlayersWithHistory.add(player.playerScore)
        }
        renumber()
    }

    /** Moves the player at [fromIndex] to [toIndex], as the drag-and-drop reorder asks. */
    fun movePlayer(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex || fromIndex !in players.indices || toIndex !in players.indices) {
            return
        }
        players.add(toIndex, players.removeAt(fromIndex))
        renumber()
    }

    /** Sorts by score, alternating between ascending and descending like the legacy menu item. */
    fun sortByScore() {
        val byScore = PlayerScore.sortByScore()
        val comparator = Comparator<OrganizePlayer> { left, right ->
            if (sortsDescending) {
                byScore.compare(right.playerScore, left.playerScore)
            } else {
                byScore.compare(left.playerScore, right.playerScore)
            }
        }
        players.sortWith(comparator)
        sortsDescending = !sortsDescending
        renumber()
    }

    fun shuffle(random: Random = Random.Default) {
        players.shuffle(random)
        renumber()
    }

    /** The players in display order, ready for the activity result. */
    fun toResultList(): ArrayList<PlayerScore> = ArrayList(players.map { it.playerScore })

    private fun renumber() {
        players.forEachIndexed { index, player -> player.renumber(index) }
    }
}
