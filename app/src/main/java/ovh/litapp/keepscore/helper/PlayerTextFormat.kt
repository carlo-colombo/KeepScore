package ovh.litapp.keepscore.helper

import android.widget.LinearLayout
import ovh.litapp.keepscore.R

/** The scoreboard text sizes and paddings, which shrink as the game gets more players. */
enum class PlayerTextFormat(
    val playerScoreTextSize: Int,
    val plusMinusTextSize: Int,
    val playerNameTextSize: Int,
    val badgeTextSize: Int,
    val badgePaddingLeftRight: Int,
    val badgePaddingTopBottom: Int,
    val badgeOffset: Int,
    val plusMinusButtonHeight: Int,
    val plusMinusButtonHeightWithRoundTotals: Int,
    val onscreenDeltaButtonTextSize: Int,
    val onscreenDeltaButtonHeight: Int,
) {
    TwoToFourPlayers(
        R.dimen.player_score_2_to_6,
        R.dimen.player_plus_minus_button_2_to_6,
        R.dimen.player_name_2_to_4,
        R.dimen.player_badge_2_to_4,
        R.dimen.player_badge_padding_left_right_2_to_4,
        R.dimen.player_badge_padding_top_bottom_2_to_4,
        R.dimen.player_badge_offset_2_to_4,
        R.dimen.delta_button_height_2_to_4,
        R.dimen.delta_button_height_2_to_4_with_round_totals,
        R.dimen.player_onscreen_delta_button_text_size_2_to_4,
        R.dimen.onscreen_delta_button_height_2_to_4,
    ),
    FiveToSixPlayers(
        R.dimen.player_score_2_to_6,
        R.dimen.player_plus_minus_button_2_to_6,
        R.dimen.player_name_5_to_6,
        R.dimen.player_badge_5_to_6,
        R.dimen.player_badge_padding_left_right_5_to_6,
        R.dimen.player_badge_padding_top_bottom_5_to_6,
        R.dimen.player_badge_offset_5_to_6,
        R.dimen.delta_button_height_5_to_6,
        R.dimen.delta_button_height_5_to_6_with_round_totals,
        R.dimen.player_onscreen_delta_button_text_size_5_to_6,
        R.dimen.onscreen_delta_button_height_5_to_6,
    ),
    SevenToEightPlayers(
        R.dimen.player_score_7_to_8,
        R.dimen.player_plus_minus_button_7_to_8,
        R.dimen.player_name_7_to_8,
        R.dimen.player_badge_7_to_8,
        R.dimen.player_badge_padding_left_right_7_to_8,
        R.dimen.player_badge_padding_top_bottom_7_to_8,
        R.dimen.player_badge_offset_7_to_8,
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT,
        R.dimen.player_onscreen_delta_button_text_size_7_to_8,
        R.dimen.onscreen_delta_button_height_7_to_8,
    ),
    ;

    companion object {

        @JvmStatic
        fun forNumPlayers(numPlayers: Int): PlayerTextFormat =
            when (numPlayers) {
                2, 3, 4 -> TwoToFourPlayers
                5, 6 -> FiveToSixPlayers
                else -> SevenToEightPlayers
            }
    }
}
