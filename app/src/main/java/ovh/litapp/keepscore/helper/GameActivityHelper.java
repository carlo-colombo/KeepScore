package ovh.litapp.keepscore.helper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import ovh.litapp.keepscore.GameActivity;
import ovh.litapp.keepscore.LandscapeGameActivity;
import ovh.litapp.keepscore.NewGameActivity;
import ovh.litapp.keepscore.PortraitGameActivity;
import ovh.litapp.keepscore.db.Game;
import ovh.litapp.keepscore.db.GameSummary;

public class GameActivityHelper {

    public static void openGame(Context context, GameSummary game) {
        Intent intent = new Intent(context, getGameActivityClass(game.getPlayerNames().size()));
        intent.putExtra(GameActivity.EXTRA_GAME_ID, game.getId());

        context.startActivity(intent);
    }
    
    public static void newGame(Context context, String[] playerNames, String[] playerColors) {
        Intent intent = new Intent(context, getGameActivityClass(playerNames.length));

        intent.putExtra(GameActivity.EXTRA_PLAYER_NAMES, playerNames);
        intent.putExtra(GameActivity.EXTRA_PLAYER_COLORS, playerColors);

        context.startActivity(intent);
    }
    
    public static void openGameWithClearTop(Activity context, Game game) {
        Intent intent = new Intent(context, getGameActivityClass(game.getPlayerScores().size()));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(GameActivity.EXTRA_GAME, game);

        context.startActivity(intent);

        CompatibilityHelper.overridePendingTransition(context, android.R.anim.fade_in,
                android.R.anim.fade_out);
    }
    
    static Class<?> getGameActivityClass(int numPlayers) {
        return numPlayers > NewGameActivity.TYPICAL_MAX_NUMBER_OF_PLAYERS
                ? PortraitGameActivity.class
                : LandscapeGameActivity.class;
    }
    
}
