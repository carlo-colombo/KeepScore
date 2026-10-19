package ovh.litapp.keepscore.ui.nameplayers

import ovh.litapp.keepscore.helper.PlayerColor

/** Pure name-entry rules shared by the Compose screen and its unit tests. */
object NamePlayersModel {
    const val MIN_PLAYERS = 2
    const val MAX_PLAYERS = 30

    fun normalizePlayerCount(requested: Int): Int = requested.coerceIn(MIN_PLAYERS, MAX_PLAYERS)

    fun defaultPlayerColorSerializations(numPlayers: Int): List<String> =
        List(numPlayers) { index ->
            PlayerColor.serialize(PlayerColor.BUILT_INS[index % PlayerColor.BUILT_INS.size])
        }
}
