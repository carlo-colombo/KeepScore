package ovh.litapp.keepscore.serialization

import android.content.ContentResolver
import android.net.Uri
import android.util.Xml

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringWriter
import java.util.zip.GZIPInputStream

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer

import ovh.litapp.keepscore.db.Delta
import ovh.litapp.keepscore.db.Game
import ovh.litapp.keepscore.db.PlayerScore
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.SdcardHelper.Format
import ovh.litapp.keepscore.helper.XmlHelper
import ovh.litapp.keepscore.util.CollectionUtil
import ovh.litapp.keepscore.util.Pair
import ovh.litapp.keepscore.util.StringUtil
import ovh.litapp.keepscore.util.UtilLogger

/**
 * helper classes for serializing/deserializing Games and PlayerScores.
 *
 * The XML/GZIP format is a compatibility contract with released versions: tag names, the
 * null/empty attributes and the version numbers below must not change.
 *
 * @author nolan
 */
object GamesBackupSerializer {

    /** Version without "automatic" - i.e., everything was manual */
    const val VERSION_ONE = 1

    /** Version where "automatic" was added, to distinguish automatic backups from manual backups */
    const val VERSION_TWO = 2

    /**
     * Version where "backupFilename" was added, for cases where the filename could not easily be determined
     * (e.g. from Gmail attachments)
     */
    const val VERSION_THREE = 3

    /** Version where the lastUpdate was added. */
    const val VERSION_FOUR = 4

    /** Version where colors and player history timestamps were added. */
    const val VERSION_FIVE = 5

    const val CURRENT_VERSION = VERSION_FIVE

    private const val ATTRIBUTE_NULL = "isNull"
    private const val ATTRIBUTE_EMPTY = "isEmpty"

    /** The text written for a null or empty value; the attribute above carries the real meaning. */
    private const val NULL_TEXT = "null"

    private val log = UtilLogger(GamesBackupSerializer::class.java)

    private enum class Tag {
        PlayerScore, Game, GamesBackup, gameCount, version, automatic, backupFilename, dateGameSaved,
        dateBackupSaved, dateGameStarted, gameName, playerName, score, playerNumber, history, historyTimestamps,
        lastUpdate, color, Games, PlayerScores,
    }

    /**
     * Don't read the entire file; just read the game count and other basic, summarized information.
     */
    @JvmStatic
    fun readGamesBackupSummary(uri: Uri, format: Format, contentResolver: ContentResolver): GamesBackupSummary {

        val result = GamesBackupSummary()

        var infoReceived = 0

        try {

            var reader: BufferedReader? = null
            var parserEvent: Int
            try {
                val parserFactory = XmlPullParserFactory.newInstance()
                val parser = parserFactory.newPullParser()
                var inputStream = contentResolver.openInputStream(uri)
                if (format == Format.GZIP) { // new, gzipped format
                    inputStream = GZIPInputStream(inputStream)
                }
                reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"), 0x1000)
                parser.setInput(reader)
                parserEvent = parser.eventType
                var tag: Tag? = null
                var text: String? = null

                while (parserEvent != XmlPullParser.END_DOCUMENT) {
                    parserEvent = parser.next()
                    when (parserEvent) {
                        XmlPullParser.START_TAG -> tag = Tag.valueOf(parser.name)
                        XmlPullParser.TEXT -> text = parser.text
                        XmlPullParser.END_TAG -> when (tag) {
                            Tag.gameCount -> {
                                result.gameCount = text!!.toInt()
                                infoReceived++
                            }

                            Tag.version -> {
                                result.version = text!!.toInt()
                                infoReceived++
                                if (result.version < VERSION_TWO) {
                                    // no automatic vs. manual distinction in version one
                                    result.isAutomatic = false
                                    infoReceived++
                                }
                                if (result.version < VERSION_THREE) {
                                    // filename not stored in XML file itself until version three
                                    result.filename = uri.lastPathSegment
                                    infoReceived++
                                }
                            }

                            Tag.automatic -> {
                                result.isAutomatic = text!!.toBoolean()
                                infoReceived++
                            }

                            Tag.dateBackupSaved -> {
                                result.dateSaved = text!!.toLong()
                                infoReceived++
                            }

                            Tag.backupFilename -> {
                                result.filename = text
                                infoReceived++
                            }

                            else -> Unit
                        }
                    }

                    if (infoReceived == 5) {
                        // this is all the info required to create a summary
                        return result
                    }
                }
            } finally {
                reader?.close()
            }
        } catch (e: NumberFormatException) {
            log.e(e, "unexpected exception for $uri")
            throw RuntimeException(e)
        } catch (e: IOException) {
            log.e(e, "unexpected exception for $uri")
            throw RuntimeException(e)
        } catch (e: XmlPullParserException) {
            log.e(e, "unexpected exception for $uri")
            throw RuntimeException(e)
        }
        throw RuntimeException("failed to find summary for $uri")
    }

    @JvmStatic
    fun deserialize(xmlData: String): GamesBackup {
        var parserEvent: Int

        var tag: Tag? = null

        val gamesBackup = GamesBackup()
        gamesBackup.games = ArrayList()
        var game: Game? = null
        var playerScore: PlayerScore? = null
        var attributes: MutableMap<String, String>? = null

        try {
            // calls service (referenced in url) to request XML serialized data
            val parser = XmlHelper.loadData(xmlData)
            parserEvent = parser.eventType

            while (parserEvent != XmlPullParser.END_DOCUMENT) {
                when (parserEvent) {
                    XmlPullParser.START_TAG -> {
                        tag = Tag.valueOf(parser.name)
                        when (tag) {
                            Tag.Game -> {
                                game = Game()
                                game.playerScores = ArrayList()
                            }

                            Tag.PlayerScore -> playerScore = PlayerScore()
                            else -> Unit
                        }
                        // null or empty marker
                        if (parser.attributeCount != -1) {
                            attributes = getAttributes(parser)
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        tag = Tag.valueOf(parser.name)
                        when (tag) {
                            Tag.Game -> gamesBackup.games.add(game!!)
                            Tag.PlayerScore -> game!!.playerScores.add(playerScore!!)
                            else -> Unit
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text

                        if (!StringUtil.isEmptyOrWhitespace(text)) {
                            handleText(text, tag, attributes!!, gamesBackup, game, playerScore)
                        }
                    }
                }

                parserEvent = parser.next()
            }
        } catch (e: XmlPullParserException) {
            log.e(e, "unexpected")
        } catch (e: IOException) {
            log.e(e, "unexpected")
        }

        applyVersionFixes(gamesBackup)

        // return de-serialized game backup
        return gamesBackup
    }

    private fun applyVersionFixes(gamesBackup: GamesBackup) {

        // fix for older versions of keepscore without colors
        if (gamesBackup.version < VERSION_FIVE) {
            for (game in gamesBackup.games) {
                for (playerScore in game.playerScores) {
                    playerScore.playerColor = PlayerColor.BUILT_INS[playerScore.playerNumber]
                }
            }
        }
    }

    private fun handleText(
        text: String,
        tag: Tag?,
        attributes: Map<String, String>,
        gamesBackup: GamesBackup,
        game: Game?,
        playerScore: PlayerScore?,
    ) {

        when (tag) {
            Tag.gameCount -> gamesBackup.gameCount = text.toInt()
            Tag.backupFilename -> gamesBackup.filename = text
            Tag.version -> gamesBackup.version = text.toInt()
            Tag.automatic -> gamesBackup.setAutomatic(text.toBoolean())
            Tag.dateBackupSaved -> gamesBackup.dateSaved = text.toLong()
            Tag.dateGameSaved -> game!!.dateSaved = text.toLong()
            Tag.dateGameStarted -> game!!.dateStarted = text.toLong()
            Tag.gameName -> game!!.name = getTextOrNullOrEmpty(attributes, text)
            Tag.playerName -> playerScore!!.name = getTextOrNullOrEmpty(attributes, text)
            Tag.playerNumber -> playerScore!!.playerNumber = text.toInt()
            Tag.history -> playerScore!!.history = Delta.fromJoinedScores(getTextOrNullOrEmpty(attributes, text))
            Tag.historyTimestamps -> {
                // update the existing list; assume this tag always comes after the "history" tag
                val timestamps = CollectionUtil.stringsToLongs(
                    StringUtil.split(getTextOrNullOrEmpty(attributes, text), ','),
                )
                val history = playerScore!!.history
                for (i in history.indices) {
                    history[i].timestamp = timestamps[i]
                }
            }

            Tag.color -> playerScore!!.playerColor = PlayerColor.deserialize(text)
            Tag.score -> playerScore!!.score = text.toLong()
            Tag.lastUpdate -> playerScore!!.lastUpdate = text.toLong()
            else -> Unit
        }
    }

    private fun getTextOrNullOrEmpty(attributes: Map<String, String>, text: String): String? {
        if (attributes[ATTRIBUTE_NULL]?.toBoolean() == true) {
            return null
        } else if (attributes[ATTRIBUTE_EMPTY]?.toBoolean() == true) {
            return ""
        }
        return text
    }

    @JvmStatic
    fun serialize(gamesBackup: GamesBackup): String {
        val rawXml = serializeAsRawXml(gamesBackup)

        // pretty-print the xml
        return XmlHelper.prettyPrint(rawXml)
    }

    private fun serializeAsRawXml(gamesBackup: GamesBackup): String {
        val serializer = Xml.newSerializer()

        val writer = StringWriter()
        try {
            serializer.setOutput(writer)
            serializer.startDocument("UTF-8", true)
            serializer.startTag("", Tag.GamesBackup.name)
            addTag(serializer, Tag.gameCount, gamesBackup.gameCount)
            addTag(serializer, Tag.version, gamesBackup.version)
            addTag(serializer, Tag.automatic, gamesBackup.isAutomatic())
            addTag(serializer, Tag.backupFilename, gamesBackup.filename)
            addTag(serializer, Tag.dateBackupSaved, gamesBackup.dateSaved)
            serializer.startTag("", Tag.Games.name)
            for (game in gamesBackup.games) {
                serializer.startTag("", Tag.Game.name)
                addTag(serializer, Tag.dateGameSaved, game.dateSaved)
                addTag(serializer, Tag.dateGameStarted, game.dateStarted)
                addTag(serializer, Tag.gameName, game.name)

                serializer.startTag("", Tag.PlayerScores.name)
                for (playerScore in game.playerScores) {
                    serializer.startTag("", Tag.PlayerScore.name)

                    addTag(serializer, Tag.playerName, playerScore.name)
                    addTag(serializer, Tag.score, playerScore.score)
                    addTag(serializer, Tag.playerNumber, playerScore.playerNumber)
                    val historyAsStrings: Pair<String, String> = Delta.toJoinedStrings(playerScore.history)
                    addTag(serializer, Tag.history, historyAsStrings.first)
                    addTag(serializer, Tag.historyTimestamps, historyAsStrings.second)
                    addTag(serializer, Tag.lastUpdate, playerScore.lastUpdate.toString())
                    addTag(serializer, Tag.color, PlayerColor.serialize(playerScore.playerColor))

                    serializer.endTag("", Tag.PlayerScore.name)
                }
                serializer.endTag("", Tag.PlayerScores.name)
                serializer.endTag("", Tag.Game.name)
            }
            serializer.endTag("", Tag.Games.name)
            serializer.endTag("", Tag.GamesBackup.name)

            serializer.endDocument()
            return writer.toString()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    /**
     * Convenience method for adding tags
     */
    private fun addTag(serializer: XmlSerializer, tag: Tag, value: Any?) {
        serializer.startTag("", tag.name)
        if (value == null) {
            // explicitly mark nulls with an attribute
            serializer.attribute("", ATTRIBUTE_NULL, true.toString())
            serializer.text(NULL_TEXT)
        } else if (value == "") {
            // explicitly mark empty strings
            serializer.attribute("", ATTRIBUTE_EMPTY, true.toString())
            serializer.text(NULL_TEXT)
        } else {
            serializer.text(value.toString())
        }
        serializer.endTag("", tag.name)
    }

    private fun getAttributes(parser: XmlPullParser): MutableMap<String, String> {
        val attrs = HashMap<String, String>()
        for (i in 0 until parser.attributeCount) {
            attrs[parser.getAttributeName(i)] = parser.getAttributeValue(i)
        }
        return attrs
    }
}
