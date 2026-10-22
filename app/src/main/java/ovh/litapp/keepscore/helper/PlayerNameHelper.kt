package ovh.litapp.keepscore.helper

import android.content.Context
import ovh.litapp.keepscore.db.GameDBHelper
import ovh.litapp.keepscore.util.StringUtil

object PlayerNameHelper {

    /** The names of players from previous games, for the name field's suggestions. */
    @JvmStatic
    fun getPlayerNameSuggestions(context: Context): List<String> {
        // populate player name suggestions from previous games by grabbing the names from the
        // database
        var dbHelper: GameDBHelper? = null
        try {
            dbHelper = GameDBHelper(context)
            val suggestions = dbHelper.findDistinctPlayerNames()

            // filter out null/empty/whitespace names
            val filteredSuggestions = suggestions
                .filterNot { StringUtil.isEmptyOrWhitespace(it) }
                .map { it.trim() }

            // sort, case insensitive
            return filteredSuggestions.sortedWith(String.CASE_INSENSITIVE_ORDER)
        } finally {
            dbHelper?.close()
        }
    }
}
