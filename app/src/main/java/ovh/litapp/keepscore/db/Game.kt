package ovh.litapp.keepscore.db

import android.os.Parcel
import android.os.Parcelable
import java.util.Comparator
import ovh.litapp.keepscore.util.CollectionUtil
import ovh.litapp.keepscore.util.LongUtil

/** A saved game: when it started and was saved, its name and its players' scores. */
class Game : Parcelable, Cloneable {

    var name: String? = null
    var id: Int = -1
    var dateStarted: Long = 0
    var dateSaved: Long = 0
    var playerScores: MutableList<PlayerScore> = ArrayList()

    constructor()

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        dateStarted = parcel.readLong()
        dateSaved = parcel.readLong()
        name = parcel.readString()
        val scores = ArrayList<PlayerScore>()
        while (true) {
            val playerScore = parcel.readParcelable<PlayerScore>(PlayerScore::class.java.classLoader)
            if (playerScore == null) {
                break
            }
            scores.add(playerScore)
        }
        playerScores = scores
    }

    override fun toString(): String =
        "Game [dateSaved=$dateSaved, dateStarted=$dateStarted, id=$id, name=$name, " +
            "playerScores=${playerScores?.size ?: 0}]"

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeLong(dateStarted)
        dest.writeLong(dateSaved)
        dest.writeString(name)

        for (playerScore in playerScores) {
            dest.writeParcelable(playerScore, 0)
        }
    }

    public override fun clone(): Game {
        val game = Game()
        game.dateSaved = dateSaved
        game.dateStarted = dateStarted
        game.id = id
        game.name = name
        game.playerScores = CollectionUtil.transform(
            playerScores,
            CollectionUtil.Function { obj: PlayerScore -> obj.clone() },
        ).toMutableList()
        return game
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + id
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val otherGame = other as Game
        if (id == -1 && otherGame.id == -1) { // not saved yet
            return this === other // use simple equality comparison
        }
        return id == otherGame.id
    }

    /**
     * Make a clone of this Game that can be saved cleanly to the database as a new game.
     */
    fun makeCleanCopy(): Game {
        val newGame = clone()

        // reset everything except the player names and game name
        newGame.id = -1
        newGame.dateStarted = System.currentTimeMillis()
        newGame.dateSaved = 0

        for (playerScore in newGame.playerScores) {
            playerScore.id = -1
        }

        return newGame
    }

    companion object {

        @JvmStatic
        fun byRecentlySaved(): Comparator<Game> =
            Comparator { object1, object2 -> LongUtil.compare(object2.dateSaved, object1.dateSaved) }

        @JvmField
        val CREATOR: Parcelable.Creator<Game> = object : Parcelable.Creator<Game> {
            override fun createFromParcel(parcel: Parcel): Game = Game(parcel)

            override fun newArray(size: Int): Array<Game?> = arrayOfNulls(size)
        }
    }
}
