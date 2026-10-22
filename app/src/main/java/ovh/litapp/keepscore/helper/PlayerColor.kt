package ovh.litapp.keepscore.helper

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat
import ovh.litapp.keepscore.R

/**
 * Colors applied to individual players, for the vast majority of board games that have a unique color
 * per player.
 *
 * @author nolan
 */
sealed class PlayerColor {

    abstract fun toBackgroundDrawable(context: Context): Drawable?

    abstract fun toColor(context: Context): Int

    abstract fun toChartColor(context: Context): Int

    class BuiltInPlayerColor(
        private val backgroundResId: Int,
        private val colorResId: Int,
        val ordinal: Int,
    ) : PlayerColor() {

        override fun toBackgroundDrawable(context: Context): Drawable? =
            ContextCompat.getDrawable(context, backgroundResId)

        override fun toColor(context: Context): Int =
            context.resources.getColor(colorResId, context.theme)

        override fun toChartColor(context: Context): Int =
            if (colorResId == R.color.player_color_10) {
                // white is hard to see on the chart; use a cream color instead
                context.resources.getColor(R.color.player_color_10_chart_line, context.theme)
            } else {
                toColor(context)
            }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + backgroundResId
            result = prime * result + ordinal
            return result
        }

        override fun equals(other: Any?): Boolean =
            other is BuiltInPlayerColor && ordinal == other.ordinal

        override fun toString(): String =
            "BuiltInPlayerColor [backgroundResId=$backgroundResId, ordinal=$ordinal]"
    }

    class CustomPlayerColor(val color: Int) : PlayerColor() {

        override fun toBackgroundDrawable(context: Context): Drawable {
            // build up a nice dynamic state list drawable with rounded corners
            // as an aside, the selected state is not necessary, because it's never shown in the
            // 4x4 grid
            val normal = ContextCompat.getDrawable(context, R.drawable.player_color_template)!!
                .mutate() as GradientDrawable
            val pressed = ContextCompat.getDrawable(context, R.drawable.player_color_template_pressed)!!
                .mutate() as GradientDrawable

            normal.setColor(color)
            pressed.setColor(color)

            val states = StateListDrawable()
            states.addState(intArrayOf(-android.R.attr.state_pressed), normal)
            states.addState(intArrayOf(android.R.attr.state_pressed), pressed)

            return states
        }

        override fun toColor(context: Context): Int = color

        override fun toChartColor(context: Context): Int = color

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + color
            return result
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null || javaClass != other.javaClass) {
                return false
            }
            val otherColor = other as CustomPlayerColor
            return color == otherColor.color
        }
    }

    companion object {

        @JvmField
        val BUILT_INS: Array<BuiltInPlayerColor> = arrayOf(
            BuiltInPlayerColor(R.drawable.player_color_selector_01, R.color.player_color_01, 0),
            BuiltInPlayerColor(R.drawable.player_color_selector_02, R.color.player_color_02, 1),
            BuiltInPlayerColor(R.drawable.player_color_selector_03, R.color.player_color_03, 2),
            BuiltInPlayerColor(R.drawable.player_color_selector_04, R.color.player_color_04, 3),
            BuiltInPlayerColor(R.drawable.player_color_selector_05, R.color.player_color_05, 4),
            BuiltInPlayerColor(R.drawable.player_color_selector_06, R.color.player_color_06, 5),
            BuiltInPlayerColor(R.drawable.player_color_selector_07, R.color.player_color_07, 6),
            BuiltInPlayerColor(R.drawable.player_color_selector_08, R.color.player_color_08, 7),
            BuiltInPlayerColor(R.drawable.player_color_selector_09, R.color.player_color_09, 8),
            BuiltInPlayerColor(R.drawable.player_color_selector_10, R.color.player_color_10, 9),
            BuiltInPlayerColor(R.drawable.player_color_selector_11, R.color.player_color_11, 10),
            BuiltInPlayerColor(R.drawable.player_color_selector_12, R.color.player_color_12, 11),
            BuiltInPlayerColor(R.drawable.player_color_selector_13, R.color.player_color_13, 12),
            BuiltInPlayerColor(R.drawable.player_color_selector_14, R.color.player_color_14, 13),
            BuiltInPlayerColor(R.drawable.player_color_selector_15, R.color.player_color_15, 14),
            BuiltInPlayerColor(R.drawable.player_color_selector_16, R.color.player_color_16, 15),
        )

        @JvmStatic
        fun serialize(playerColor: PlayerColor): String =
            when (playerColor) {
                is BuiltInPlayerColor -> playerColor.ordinal.toString()
                is CustomPlayerColor -> "#" + Integer.toHexString(playerColor.color)
            }

        @JvmStatic
        fun deserialize(str: String): PlayerColor =
            if (str.startsWith("#")) {
                CustomPlayerColor(Color.parseColor(str))
            } else {
                BUILT_INS[str.toInt()]
            }
    }
}
