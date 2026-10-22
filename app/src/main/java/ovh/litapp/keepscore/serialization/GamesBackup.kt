package ovh.litapp.keepscore.serialization

import ovh.litapp.keepscore.db.Game

/**
 * Representation of a saved XML file of games
 *
 * @author nolan
 */
class GamesBackup {

    var filename: String? = null
    var gameCount: Int = 0
    var version: Int = 0
    var dateSaved: Long = 0
    var games: MutableList<Game> = mutableListOf()

    private var automatic: Boolean = false

    // Explicit accessors instead of a property: Kotlin call sites still call `setAutomatic(...)`
    // while Java call sites call `setAutomatic(...)` / `isAutomatic()`, exactly as before.
    fun isAutomatic(): Boolean = automatic

    fun setAutomatic(automatic: Boolean) {
        this.automatic = automatic
    }
}
