package ovh.litapp.keepscore.serialization

/**
 * Representation of a saved XML file of games, summarized.
 *
 * @author nolan
 */
class GamesBackupSummary {

    var version: Int = 0
    var isAutomatic: Boolean = false
    var filename: String? = null
    var gameCount: Int = 0
    var dateSaved: Long = 0
}
