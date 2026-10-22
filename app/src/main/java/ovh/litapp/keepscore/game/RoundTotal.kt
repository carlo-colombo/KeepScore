package ovh.litapp.keepscore.game

/**
 * The current round of a game and the sum of every player's score for that round, as shown by the
 * round-total strip.
 */
data class RoundTotal(val round: Int, val total: Int)
