package ovh.litapp.keepscore.data

import ovh.litapp.keepscore.db.Game

/**
 * Representation of the result of loading a GamesBackup.
 *
 * @author nolan
 */
class LoadGamesBackupResult {

    var filename: String? = null
    var numFound: Int = 0
    var numDuplicates: Int = 0
    var loadedGames: MutableList<Game> = ArrayList()
}
