package ovh.litapp.keepscore.helper

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import java.io.Writer
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.SortedSet
import java.util.TreeSet
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import ovh.litapp.keepscore.KeepScoreApplication
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.util.CSVUtil
import ovh.litapp.keepscore.util.CollectionUtil
import ovh.litapp.keepscore.util.Functions
import ovh.litapp.keepscore.util.UtilLogger

/**
 * Reads and writes the backups and spreadsheets in the app's own external files directory.
 *
 * The files used to live under `/sdcard/keepscore`, which needs storage permissions on modern
 * Android; `getExternalFilesDir` needs none and is exempt from scoped storage, so the same paths
 * keep working.
 */
object SdcardHelper {

    private const val BUFFER = 0x1000 // 4K

    private const val ROOT_DIR = "keepscore"

    // apparently this is a good format for Excel/LibreOffice
    private val CSV_DATE_FORMAT: DateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val log = UtilLogger(SdcardHelper::class.java)

    /**
     * Format to save the backups file in.
     *
     * Gzip is used to save space.
     *
     * XML is used because Gmail doesn't let you open zipped files from within the app.
     *
     * @author nolan
     */
    enum class Format {
        XML, GZIP,
    }

    /**
     * Location on the SD card under "/sdcard/keepscore" to save the backup file.
     *
     * The "backups" folder is used for backups that the user saves either manually or automatically
     * and wants to retrieve later.
     *
     * The "shares" folder is just used for backup files that the user wants to send to someone else.
     * So in principle, it's temporary storage.
     *
     * Ditto for the "spreadsheets" folder.
     *
     * @author nolan
     */
    enum class Location(val directoryName: String) {
        Backups("backups"),
        Shares("shares"),
        Spreadsheets("spreadsheets"),
    }

    @JvmStatic
    fun isAvailable(): Boolean = getRootDir() != null

    /**
     * App-specific external storage: needs no permission and is exempt from scoped-storage
     * restrictions, unlike the old `/sdcard/keepscore`.
     */
    private fun getRootDir(): File? {
        val context: Context = KeepScoreApplication.getInstance() ?: return null
        val base = context.getExternalFilesDir(null) ?: context.filesDir ?: return null
        return getOrCreateDir(base, ROOT_DIR)
    }

    @JvmStatic
    fun getDirectory(location: Location): File? {
        val rootDir = getRootDir() ?: return null
        return getOrCreateDir(rootDir, location.directoryName)
    }

    @JvmStatic
    fun backupExists(filename: String, location: Location): Boolean {
        val file = File(getDirectory(location), filename)
        return file.exists()
    }

    @JvmStatic
    fun list(location: Location): List<String> {
        val file = getDirectory(location)
        val filesAsArray = file?.list()
        return filesAsArray?.toList() ?: emptyList()
    }

    @JvmStatic
    fun open(uri: Uri, format: Format, contentResolver: ContentResolver): String {
        val stringBuilder = StringBuilder()
        var bufferedReader: BufferedReader? = null
        val buffer = CharArray(BUFFER)
        var charsRead: Int

        try {
            var fileInputStream: InputStream = contentResolver.openInputStream(uri) ?: return ""
            if (format == Format.GZIP) { // new, gzipped format
                fileInputStream = GZIPInputStream(fileInputStream)
            }
            val inputStreamReader = InputStreamReader(fileInputStream, Charsets.UTF_8)
            bufferedReader = BufferedReader(inputStreamReader, BUFFER)

            while (bufferedReader.read(buffer, 0, buffer.size).also { charsRead = it } != -1) {
                stringBuilder.append(buffer, 0, charsRead)
            }
        } catch (ex: IOException) {
            log.e(ex, "couldn't read file")
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close()
                } catch (e: IOException) {
                    log.e(e, "couldn't close buffered reader")
                }
            }
        }
        return stringBuilder.toString()
    }

    /**
     * Write CSV file with games on the X axis and players (and basic game data) on the Y axis.
     */
    @JvmStatic
    fun saveSpreadsheet(filename: String, games: List<Game>, context: Context) {
        // get all the unique player names so we can put them on the X axis
        val playerNames: SortedSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)

        // special value for players whose names weren't specified
        for (game in games) {
            for (playerScore in game.playerScores) {
                playerNames.add(playerScore.toDisplayName(context).toString())
            }
        }

        // sort by start date descending
        val sortedGames = ArrayList(games)
        sortedGames.sortWith(compareByDescending { it.dateStarted })

        val file = File(getDirectory(Location.Spreadsheets), filename)
        if (file.parentFile == null) {
            log.e("no spreadsheet directory available")
            return
        }

        var writer: BufferedWriter? = null
        try {
            writer = BufferedWriter(
                OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8),
            )

            // write the column names
            val columnNames = ArrayList(
                context.resources.getStringArray(R.array.share_spreadsheet_column_names).toList(),
            )

            columnNames.addAll(playerNames)
            writer.write(CSVUtil.convertToLine(columnNames))

            // write each game as a line in the CSV
            for (game in sortedGames) {
                val entries = ArrayList<String?>()

                // date started
                entries.add(CSV_DATE_FORMAT.format(Date(game.dateStarted)))
                // date saved
                entries.add(CSV_DATE_FORMAT.format(Date(game.dateSaved)))
                // play time, using duration format HH:MM:SS
                val duration = (game.dateSaved - game.dateStarted) / 1000
                entries.add(
                    String.format(
                        Locale.US,
                        "%02d:%02d:%02d",
                        duration / 3600,
                        (duration % 3600) / 60,
                        duration % 60,
                    ),
                )
                // num players
                entries.add(game.playerScores.size.toString())
                // num rounds
                entries.add(
                    CollectionUtil.max(
                        game.playerScores,
                        Functions.PLAYER_SCORE_TO_HISTORY_SIZE,
                    ).toString(),
                )
                // game name
                entries.add(game.name)

                val playerScoreToName = CollectionUtil.Function<PlayerScore, String> { obj ->
                    obj.toDisplayName(context).toString()
                }

                // player(s) with max
                entries.add(
                    TextUtils.join(
                        ", ",
                        CollectionUtil.transform(
                            CollectionUtil.maxWithTies(
                                game.playerScores,
                                Functions.PLAYER_SCORE_TO_SCORE,
                            ),
                            playerScoreToName,
                        ),
                    ),
                )
                // player(s) with min
                entries.add(
                    TextUtils.join(
                        ", ",
                        CollectionUtil.transform(
                            CollectionUtil.minWithTies(
                                game.playerScores,
                                Functions.PLAYER_SCORE_TO_SCORE,
                            ),
                            playerScoreToName,
                        ),
                    ),
                )

                // rest of columns are just all the player names, so add blank for irrelevant
                // players or the score for the actual players
                val playerScoreLookup = HashMap<String, Long>()
                for (playerScore in game.playerScores) {
                    playerScoreLookup[playerScore.toDisplayName(context).toString()] =
                        playerScore.score
                }

                for (playerName in playerNames) {
                    val score = playerScoreLookup[playerName]
                    entries.add(score?.let { java.lang.Long.toString(it) })
                }
                writer.write(CSVUtil.convertToLine(entries))
            }
        } catch (e: IOException) {
            log.e(e, "unexpected error")
            throw RuntimeException(e)
        } finally {
            if (writer != null) {
                try {
                    writer.close()
                } catch (e: IOException) {
                    log.e(e, "unexpected error")
                    throw RuntimeException(e)
                }
            }
        }
    }

    /** Save the xml in zipped format. */
    @JvmStatic
    fun save(filename: String, format: Format, location: Location, xmlData: String): Boolean {
        val directory = getDirectory(location) ?: run {
            log.e("no backup directory available")
            return false
        }
        val newFile = File(directory, filename)
        try {
            if (!newFile.exists()) {
                newFile.createNewFile()
            }
        } catch (ex: IOException) {
            log.e(ex, "couldn't create new file")
            return false
        }

        var out: OutputStream? = null
        var writer: Writer? = null
        try {
            // specifying BUFFER gets rid of an annoying warning message in the logs
            // overwrite rather than append: a repeat save of the same (timestamped)
            // name must not corrupt the file
            out = BufferedOutputStream(FileOutputStream(newFile, false), BUFFER)
            if (format == Format.GZIP) {
                out = GZIPOutputStream(out)
            }
            writer = OutputStreamWriter(out, Charsets.UTF_8)
            writer.write(xmlData)
        } catch (ex: FileNotFoundException) {
            log.e(ex, "unexpected exception")
            return false
        } catch (ex: UnsupportedEncodingException) {
            log.e(ex, "unexpected exception")
            return false
        } catch (ex: IOException) {
            log.e(ex, "unexpected exception")
            return false
        } finally {
            try {
                writer?.close()
                out?.close()
            } catch (ex: IOException) {
                log.e(ex, "unexpected exception")
                return false
            }
        }
        return true
    }

    @JvmStatic
    fun createSpreadsheetFilename(): String = createFilename("spreadsheet-", ".csv")

    @JvmStatic
    fun createBackupFilename(format: Format): String =
        createFilename("games-", if (format == Format.GZIP) ".xml.gz" else ".xml")

    /** Create a simple filename from the current date. */
    private fun createFilename(prefix: String, suffix: String): String {
        val date = Date()
        val calendar = GregorianCalendar()
        calendar.time = date

        val twoDigitDecimalFormat = DecimalFormat("00")
        val fourDigitDecimalFormat = DecimalFormat("0000")

        val year = fourDigitDecimalFormat.format(calendar.get(Calendar.YEAR))
        val month = twoDigitDecimalFormat.format(calendar.get(Calendar.MONTH) + 1)
        val day = twoDigitDecimalFormat.format(calendar.get(Calendar.DAY_OF_MONTH))
        val hour = twoDigitDecimalFormat.format(calendar.get(Calendar.HOUR_OF_DAY))
        val minute = twoDigitDecimalFormat.format(calendar.get(Calendar.MINUTE))
        val second = twoDigitDecimalFormat.format(calendar.get(Calendar.SECOND))

        val stringBuilder = StringBuilder(prefix)

        stringBuilder.append(year).append(month).append(day).append(hour).append(minute).append(second)

        stringBuilder.append(suffix)

        return stringBuilder.toString()
    }

    private fun getOrCreateDir(parent: File?, name: String): File? {
        if (parent == null) {
            return null
        }
        val file = File(parent, name)
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

    @JvmStatic
    fun getFile(shortFilename: String, location: Location): File? {
        val directory = getDirectory(location)
        return directory?.let { File(it, shortFilename) }
    }
}
