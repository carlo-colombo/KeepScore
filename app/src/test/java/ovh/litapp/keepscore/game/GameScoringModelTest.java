package ovh.litapp.keepscore.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ovh.litapp.keepscore.data.RecordedChange;
import ovh.litapp.keepscore.data.RecordedChange.Type;
import ovh.litapp.keepscore.db.Delta;
import ovh.litapp.keepscore.db.PlayerScore;

/**
 * Tests for the undo/redo stacks and the derived scoreboard values in {@link GameScoringModel}.
 */
public class GameScoringModelTest {

    private static final long UPDATE_DELAY_MS = 10000L;

    @Test
    public void recordChangeEnablesUndoAndClearsRedo() {
        GameScoringModel model = new GameScoringModel(new ArrayList<PlayerScore>());
        assertFalse(model.canUndo());
        assertFalse(model.canRedo());

        model.recordChange(change(0, Type.AddNew, 5, 1000L));
        assertTrue(model.canUndo());
        assertFalse(model.canRedo());

        model.undoBatch();
        assertFalse(model.canUndo());
        assertTrue(model.canRedo());

        // a new change wipes out the redo stack
        model.recordChange(change(0, Type.AddNew, 5, 1000L));
        assertTrue(model.canUndo());
        assertFalse(model.canRedo());
    }

    @Test
    public void undoBatchPopsABatchedActionMostRecentFirst() {
        GameScoringModel model = new GameScoringModel(new ArrayList<PlayerScore>());
        // one + tap followed by two quick taps, as PlayerScoreModel records them
        model.recordChange(change(0, Type.AddNew, 1, 1000L));
        model.recordChange(change(0, Type.ModifyLast, 2, 2000L));
        model.recordChange(change(0, Type.ModifyLast, 3, 3000L));

        List<RecordedChange> undo = model.undoBatch();

        assertEquals(3, undo.size());
        assertEquals(3000L, undo.get(0).getDelta().getTimestamp());
        assertEquals(2000L, undo.get(1).getDelta().getTimestamp());
        assertEquals(1000L, undo.get(2).getDelta().getTimestamp());
        assertFalse(model.canUndo());
        assertTrue(model.canRedo());

        List<RecordedChange> redo = model.redoBatch();

        assertEquals(3, redo.size());
        assertEquals(1000L, redo.get(0).getDelta().getTimestamp());
        assertEquals(2000L, redo.get(1).getDelta().getTimestamp());
        assertEquals(3000L, redo.get(2).getDelta().getTimestamp());
        assertTrue(model.canUndo());
        assertFalse(model.canRedo());
    }

    @Test
    public void undoBatchStopsAtUnacceptableTransition() {
        GameScoringModel model = new GameScoringModel(new ArrayList<PlayerScore>());
        // two separate batches; undoing must not merge them into one action
        model.recordChange(change(0, Type.AddNew, 1, 1000L));
        model.recordChange(change(0, Type.AddNew, 2, 2000L));

        List<RecordedChange> undo = model.undoBatch();

        assertEquals(1, undo.size());
        assertEquals(2000L, undo.get(0).getDelta().getTimestamp());
        assertTrue(model.canUndo());
    }

    @Test
    public void undoBatchStopsAtDifferentPlayer() {
        GameScoringModel model = new GameScoringModel(new ArrayList<PlayerScore>());
        model.recordChange(change(0, Type.AddNew, 1, 1000L));
        model.recordChange(change(1, Type.AddNew, 2, 2000L));

        List<RecordedChange> undo = model.undoBatch();

        assertEquals(1, undo.size());
        assertEquals(1, undo.get(0).getPlayerNumber());
        assertTrue(model.canUndo());
    }

    @Test
    public void undoStackExpiresAtCapacity() {
        GameScoringModel model = new GameScoringModel(new ArrayList<PlayerScore>());
        for (int i = 0; i <= GameScoringModel.UNDO_STACK_SIZE; i++) {
            // ModifyLast -> ModifyLast is acceptable, so a full stack pops in one undo
            model.recordChange(change(0, Type.ModifyLast, 1, i));
        }

        List<RecordedChange> undo = model.undoBatch();

        assertEquals(GameScoringModel.UNDO_STACK_SIZE, undo.size());
        assertFalse(model.canUndo());
    }

    @Test
    public void transitionRulesMatchTheLegacySets() {
        assertTrue(GameScoringModel.isAcceptableUndoOrRedoTransition(true, Type.ModifyLast, Type.AddNew));
        assertTrue(GameScoringModel.isAcceptableUndoOrRedoTransition(true, Type.DeleteLastZero, Type.ModifyLast));
        assertTrue(GameScoringModel.isAcceptableUndoOrRedoTransition(true, Type.ModifyLast, Type.DeleteLastZero));
        assertFalse(GameScoringModel.isAcceptableUndoOrRedoTransition(true, Type.AddNew, Type.ModifyLast));

        assertTrue(GameScoringModel.isAcceptableUndoOrRedoTransition(false, Type.AddNew, Type.ModifyLast));
        assertTrue(GameScoringModel.isAcceptableUndoOrRedoTransition(false, Type.AddNew, Type.DeleteLastZero));
        assertTrue(GameScoringModel.isAcceptableUndoOrRedoTransition(false, Type.ModifyLast, Type.ModifyLast));
        assertFalse(GameScoringModel.isAcceptableUndoOrRedoTransition(false, Type.AddNew, Type.AddNew));
    }

    @Test
    public void computeRoundTotalUsesTheLongestHistory() {
        PlayerScore alice = newPlayerScore(0);
        alice.setHistory(new ArrayList<Delta>(Arrays.asList(new Delta(1L, 5), new Delta(2L, 3))));
        PlayerScore bob = newPlayerScore(1);
        bob.setHistory(new ArrayList<Delta>(Arrays.asList(new Delta(1L, 2))));

        RoundTotal roundTotal = new GameScoringModel(Arrays.asList(alice, bob)).computeRoundTotal();

        // round 2; Bob has no entry for it, so it counts as zero
        assertEquals(2, roundTotal.getRound());
        assertEquals(3, roundTotal.getTotal());
    }

    @Test
    public void computeRoundTotalSumsShortHistoriesForTheFirstRound() {
        PlayerScore alice = newPlayerScore(0);
        alice.setHistory(new ArrayList<Delta>(Arrays.asList(new Delta(1L, 1))));
        PlayerScore bob = newPlayerScore(1);
        bob.setHistory(new ArrayList<Delta>(Arrays.asList(new Delta(1L, 2))));

        RoundTotal roundTotal = new GameScoringModel(Arrays.asList(alice, bob)).computeRoundTotal();

        assertEquals(1, roundTotal.getRound());
        assertEquals(3, roundTotal.getTotal());
    }

    @Test
    public void computeRoundTotalIsZeroWithoutHistory() {
        RoundTotal roundTotal = new GameScoringModel(
                Arrays.asList(newPlayerScore(0), newPlayerScore(1))).computeRoundTotal();

        assertEquals(0, roundTotal.getRound());
        assertEquals(0, roundTotal.getTotal());
    }

    @Test
    public void highlightedPlayerIsTheMostRecentlyUpdated() {
        PlayerScore alice = newPlayerScore(0);
        PlayerScore bob = newPlayerScore(1);
        bob.setLastUpdate(500L);

        assertEquals(1, new GameScoringModel(Arrays.asList(alice, bob)).getHighlightedPlayerIndex());
    }

    @Test
    public void highlightedPlayerIsTheEarliestOnATie() {
        PlayerScore alice = newPlayerScore(0);
        PlayerScore bob = newPlayerScore(1);
        alice.setLastUpdate(500L);
        bob.setLastUpdate(500L);

        assertEquals(0, new GameScoringModel(Arrays.asList(alice, bob)).getHighlightedPlayerIndex());
    }

    @Test
    public void highlightedPlayerIsNoneWhenNoPlayerWasUpdated() {
        assertEquals(-1, new GameScoringModel(
                Arrays.asList(newPlayerScore(0), newPlayerScore(1))).getHighlightedPlayerIndex());
    }

    @Test
    public void undoAndRedoRestoreScoresThroughTheModels() {
        PlayerScore alice = newPlayerScore(0);
        PlayerScore bob = newPlayerScore(1);
        PlayerScoreModel aliceModel = new PlayerScoreModel(alice);
        PlayerScoreModel bobModel = new PlayerScoreModel(bob);
        GameScoringModel game = new GameScoringModel(Arrays.asList(alice, bob));

        game.recordChange(aliceModel.applyDelta(5, 1000L, UPDATE_DELAY_MS));
        game.recordChange(bobModel.applyDelta(3, 1000L, UPDATE_DELAY_MS));

        // undo only touches the most recently changed player
        List<RecordedChange> undo = game.undoBatch();
        assertEquals(1, undo.size());
        bobModel.revertChange(undo.get(0));
        assertEquals(5L, alice.getScore());
        assertEquals(0L, bob.getScore());

        List<RecordedChange> redo = game.redoBatch();
        assertEquals(1, redo.size());
        bobModel.reexecuteChange(redo.get(0));
        assertEquals(5L, alice.getScore());
        assertEquals(3L, bob.getScore());
    }

    private PlayerScore newPlayerScore(int playerNumber) {
        PlayerScore playerScore = new PlayerScore();
        playerScore.setPlayerNumber(playerNumber);
        playerScore.setScore(0L);
        playerScore.setHistory(new ArrayList<Delta>());
        return playerScore;
    }

    private RecordedChange change(int playerNumber, Type type, int value, long timestamp) {
        return new RecordedChange(playerNumber, type, new Delta(timestamp, value));
    }
}
