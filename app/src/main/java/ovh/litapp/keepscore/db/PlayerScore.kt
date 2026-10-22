package ovh.litapp.keepscore.db

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import java.util.Comparator
import ovh.litapp.keepscore.R
import ovh.litapp.keepscore.helper.PlayerColor
import ovh.litapp.keepscore.helper.PreferenceHelper

/**
 * Main representation for a player in a particular game and his/her score and score history.
 *
 * @author nolan
 */
class PlayerScore : Parcelable, Cloneable {

    var playerColor: PlayerColor = PlayerColor.BUILT_INS[0]
    var id: Int = -1
    var name: String? = null
    var score: Long = 0
    var playerNumber: Int = 0
    var history: MutableList<Delta> = ArrayList()
    var lastUpdate: Long = 0

    constructor()

    constructor(parcel: Parcel) {
        id = parcel.readInt()
        name = parcel.readString()
        score = parcel.readLong()
        playerNumber = parcel.readInt()
        history = Delta.fromJoinedStrings(
            parcel.readString().orEmpty(),
            parcel.readString().orEmpty(),
        )
        lastUpdate = parcel.readLong()
        playerColor = PlayerColor.deserialize(parcel.readString().orEmpty())
    }

    override fun toString(): String =
        "PlayerScore [history=$history, id=$id, name=$name, playerNumber=$playerNumber, " +
            "score=$score]"

    fun isAtDefault(context: Context): Boolean =
        history.isEmpty() &&
            score == PreferenceHelper.getIntPreference(
                R.string.CONSTANT_pref_initial_score,
                R.string.CONSTANT_pref_initial_score_default,
                context,
            ).toLong()

    fun toDisplayName(context: Context): CharSequence =
        toDisplayName(name, playerNumber, context)

    public override fun clone(): PlayerScore {
        val copy = PlayerScore()
        copy.history = ArrayList(history)
        copy.id = id
        copy.name = name
        copy.playerNumber = playerNumber
        copy.score = score
        copy.lastUpdate = lastUpdate
        copy.playerColor = playerColor
        return copy
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(name)
        dest.writeLong(score)
        dest.writeInt(playerNumber)
        val historyAsStrings = Delta.toJoinedStrings(history)
        dest.writeString(historyAsStrings.first)
        dest.writeString(historyAsStrings.second)
        dest.writeLong(lastUpdate)
        dest.writeString(PlayerColor.serialize(playerColor))
    }

    companion object {

        @JvmStatic
        fun sortByPlayerNumber(): Comparator<PlayerScore> =
            Comparator { left, right -> left.playerNumber - right.playerNumber }

        @JvmStatic
        fun sortByScore(): Comparator<PlayerScore> =
            Comparator { lhs, rhs -> java.lang.Long.valueOf(lhs.score).compareTo(rhs.score) }

        @JvmStatic
        fun toDisplayName(playerName: String?, playerNumber: Int, context: Context): CharSequence {
            if (!TextUtils.isEmpty(playerName)) {
                return playerName!!
            }

            return StringBuilder(context.getString(R.string.text_player))
                .append(' ')
                .append(playerNumber + 1)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<PlayerScore> = object : Parcelable.Creator<PlayerScore> {
            override fun createFromParcel(parcel: Parcel): PlayerScore = PlayerScore(parcel)

            override fun newArray(size: Int): Array<PlayerScore?> = arrayOfNulls(size)
        }
    }
}
