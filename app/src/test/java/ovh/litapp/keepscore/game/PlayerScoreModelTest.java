package ovh.litapp.keepscore.game;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import ovh.litapp.keepscore.data.RecordedChange;
import ovh.litapp.keepscore.data.RecordedChange.Type;
import ovh.litapp.keepscore.db.Delta;
import ovh.litapp.keepscore.db.PlayerScore;

/**
 * Tests for the score/history mutation and batching rules in {@link PlayerScoreModel}.
 */
public class PlayerScoreModelTest {

    private static final long UPDATE_DELAY_MS = 10000L;

    @Test
    public void firstDeltaAppendsHistoryEntry() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        RecordedChange change = model.applyDelta(5, 1000L, UPDATE_DELAY_MS);

        assertEquals(Type.AddNew, change.getType());
        assertEquals(5, change.getDelta().getValue());
        assertEquals(1000L, change.getDelta().getTimestamp());
        assertEquals(1, change.getPlayerNumber());
        assertEquals(Arrays.asList(new Delta(1000L, 5)), playerScore.getHistory());
        assertEquals(5L, playerScore.getScore());
        assertEquals(1000L, model.getLastIncremented());
    }

    @Test
    public void deltaAtDelayBoundaryBatchesIntoLastEntry() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        RecordedChange change = model.applyDelta(2, 1000L + UPDATE_DELAY_MS, UPDATE_DELAY_MS);

        assertEquals(Type.ModifyLast, change.getType());
        assertEquals(new Delta(11000L, 2), change.getDelta());
        assertEquals(Arrays.asList(new Delta(11000L, 7)), playerScore.getHistory());
        assertEquals(7L, playerScore.getScore());
        assertEquals(11000L, model.getLastIncremented());
    }

    @Test
    public void deltaWithinDelayBatchesIntoLastEntry() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        RecordedChange change = model.applyDelta(2, 2000L, UPDATE_DELAY_MS);

        assertEquals(Type.ModifyLast, change.getType());
        assertEquals(2, change.getDelta().getValue());
        assertEquals(2000L, change.getDelta().getTimestamp());
        // the stored entry holds the batch total and its timestamp moves to the latest delta
        assertEquals(Arrays.asList(new Delta(2000L, 7)), playerScore.getHistory());
        assertEquals(7L, playerScore.getScore());
        assertEquals(2000L, model.getLastIncremented());
    }

    @Test
    public void deltaAfterDelayStartsNewEntry() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        RecordedChange change = model.applyDelta(2, 1000L + UPDATE_DELAY_MS + 1, UPDATE_DELAY_MS);

        assertEquals(Type.AddNew, change.getType());
        assertEquals(new Delta(11001L, 2), change.getDelta());
        assertEquals(Arrays.asList(new Delta(1000L, 5), new Delta(11001L, 2)), playerScore.getHistory());
        assertEquals(7L, playerScore.getScore());
        assertEquals(11001L, model.getLastIncremented());
    }

    @Test
    public void deltaThatBatchesToZeroDeletesLastEntry() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        RecordedChange change = model.applyDelta(-5, 2000L, UPDATE_DELAY_MS);

        assertEquals(Type.DeleteLastZero, change.getType());
        assertEquals(5, change.getDelta().getValue());
        assertTrue(playerScore.getHistory().isEmpty());
        assertEquals(0L, playerScore.getScore());
        // the batch ends, so the next delta starts a new entry
        assertEquals(0L, model.getLastIncremented());
    }

    @Test
    public void addZeroAppendsZeroEntryAndEndsBatch() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        RecordedChange change = model.addZero(1000L);

        assertEquals(Type.AddNew, change.getType());
        assertEquals(0, change.getDelta().getValue());
        assertEquals(Arrays.asList(new Delta(1000L, 0)), playerScore.getHistory());
        assertEquals(0L, playerScore.getScore());
        assertEquals(0L, model.getLastIncremented());

        RecordedChange next = model.applyDelta(3, UPDATE_DELAY_MS + 1, UPDATE_DELAY_MS);
        assertEquals(Type.AddNew, next.getType());
        assertEquals(2, playerScore.getHistory().size());
    }

    @Test
    public void deleteLastRemovesEntryAndScore() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        RecordedChange change = model.deleteLast();

        assertEquals(Type.DeleteLast, change.getType());
        assertEquals(5, change.getDelta().getValue());
        assertTrue(playerScore.getHistory().isEmpty());
        assertEquals(0L, playerScore.getScore());
        assertEquals(0L, model.getLastIncremented());
    }

    @Test
    public void deleteLastOnEmptyHistoryReturnsNull() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        assertNull(model.deleteLast());
        assertTrue(playerScore.getHistory().isEmpty());
    }

    @Test
    public void revertThenReexecuteAddNewRestoresState() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        RecordedChange change = model.applyDelta(5, 1000L, UPDATE_DELAY_MS);

        model.revertChange(change);
        assertTrue(playerScore.getHistory().isEmpty());
        assertEquals(0L, playerScore.getScore());

        model.reexecuteChange(change);
        assertEquals(Arrays.asList(new Delta(1000L, 5)), playerScore.getHistory());
        assertEquals(5L, playerScore.getScore());
    }

    @Test
    public void revertThenReexecuteModifyLastRestoresState() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        RecordedChange change = model.applyDelta(2, 2000L, UPDATE_DELAY_MS);

        model.revertChange(change);
        assertEquals(Arrays.asList(new Delta(2000L, 5)), playerScore.getHistory());
        assertEquals(5L, playerScore.getScore());

        model.reexecuteChange(change);
        assertEquals(Arrays.asList(new Delta(2000L, 7)), playerScore.getHistory());
        assertEquals(7L, playerScore.getScore());
    }

    @Test
    public void revertThenReexecuteDeleteLastRestoresState() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        RecordedChange change = model.deleteLast();

        model.revertChange(change);
        assertEquals(Arrays.asList(new Delta(1000L, 5)), playerScore.getHistory());
        assertEquals(5L, playerScore.getScore());

        model.reexecuteChange(change);
        assertTrue(playerScore.getHistory().isEmpty());
        assertEquals(0L, playerScore.getScore());
    }

    @Test
    public void revertThenReexecuteDeleteLastZeroRestoresState() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        RecordedChange change = model.applyDelta(-5, 2000L, UPDATE_DELAY_MS);

        model.revertChange(change);
        assertEquals(Arrays.asList(new Delta(1000L, 5)), playerScore.getHistory());
        assertEquals(5L, playerScore.getScore());

        model.reexecuteChange(change);
        assertTrue(playerScore.getHistory().isEmpty());
        assertEquals(0L, playerScore.getScore());
    }

    @Test
    public void resetLastIncrementedEndsTheCurrentBatch() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 100000L, UPDATE_DELAY_MS);
        assertEquals(100000L, model.getLastIncremented());

        model.resetLastIncremented();

        assertEquals(0L, model.getLastIncremented());
        RecordedChange next = model.applyDelta(2, 100001L, UPDATE_DELAY_MS);
        assertEquals(Type.AddNew, next.getType());
        assertEquals(Arrays.asList(new Delta(100000L, 5), new Delta(100001L, 2)), playerScore.getHistory());
        assertEquals(7L, playerScore.getScore());
    }

    @Test
    public void resetClearsHistoryAndRestoresInitialScore() {
        PlayerScore playerScore = newPlayerScore();
        PlayerScoreModel model = new PlayerScoreModel(playerScore);

        model.applyDelta(5, 1000L, UPDATE_DELAY_MS);
        model.reset(10);

        assertEquals(10L, playerScore.getScore());
        assertTrue(playerScore.getHistory().isEmpty());
        assertEquals(0L, model.getLastIncremented());
    }

    private PlayerScore newPlayerScore() {
        PlayerScore playerScore = new PlayerScore();
        playerScore.setPlayerNumber(1);
        playerScore.setScore(0L);
        playerScore.setHistory(new ArrayList<Delta>());
        return playerScore;
    }
}
