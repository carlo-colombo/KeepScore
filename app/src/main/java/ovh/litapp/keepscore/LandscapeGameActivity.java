package ovh.litapp.keepscore;

/**
 * A game played in landscape: the scoreboard draws the players on a two-column grid.
 *
 * The manifest locks this activity to landscape, and the screen picks the grid from the
 * configuration's orientation. A game of more than eight players is always opened in portrait
 * instead, because the landscape grid cannot lay them all out.
 */
public class LandscapeGameActivity extends GameActivity {
}
