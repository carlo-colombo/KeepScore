package ovh.litapp.keepscore.game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import ovh.litapp.keepscore.data.RecordedChange;
import ovh.litapp.keepscore.data.RecordedChange.Type;
import ovh.litapp.keepscore.db.Delta;
import ovh.litapp.keepscore.db.PlayerScore;

/**
 * The scoring state for a single player in a game.
 *
 * <p>Owns every mutation of a {@link PlayerScore}'s score and history, plus the timestamp of the
 * last increment that decides whether a new + or - tap batches into the current history entry or
 * starts a new one. The batching rules live here rather than in the view so they can be exercised
 * by plain JUnit tests; callers are responsible for refreshing the UI and for passing the returned
 * {@link RecordedChange} to the undo recorder.
 */
public class PlayerScoreModel {

    private final PlayerScore playerScore;

    // Timestamps are written on the increment background thread while the UI thread reads them, so
    // keep the same atomic access the legacy PlayerView used.
    private final AtomicLong lastIncremented = new AtomicLong(0);

    public PlayerScoreModel(PlayerScore playerScore) {
        this.playerScore = playerScore;
    }

    public PlayerScore getPlayerScore() {
        return playerScore;
    }

    /**
     * Timestamp of the last increment, or 0 when the current history entry is no longer being
     * batched into.
     */
    public long getLastIncremented() {
        return lastIncremented.get();
    }

    /** Marks the current history entry as no longer being batched into, e.g. after "confirm". */
    public void resetLastIncremented() {
        lastIncremented.set(0);
    }

    /**
     * Applies {@code value} to the player's score. When the previous increment was within
     * {@code updateDelayMs} of {@code currentTime}, the value is batched into the last history
     * entry (dropping that entry if the batch reaches zero); otherwise a new history entry is
     * appended. Returns the change to record for undo/redo.
     */
    public RecordedChange applyDelta(int value, long currentTime, long updateDelayMs) {
        long lastIncrementedTime = lastIncremented.getAndSet(currentTime);
        List<Delta> history = playerScore.getHistory();
        RecordedChange recordedChange;

        if (currentTime - lastIncrementedTime > updateDelayMs || history.isEmpty()) {
            Delta delta = new Delta(currentTime, value);
            history.add(delta);
            recordedChange = new RecordedChange(playerScore.getPlayerNumber(), Type.AddNew, delta);
        } else {
            int lastIndex = history.size() - 1;
            int newValue = history.get(lastIndex).getValue() + value;
            if (newValue == 0) {
                // don't add "0" to the list; just delete the last history item
                Delta deletedDelta = history.remove(lastIndex);
                // reset the batch time so we don't update the previous value later
                lastIncremented.set(0);
                recordedChange = new RecordedChange(playerScore.getPlayerNumber(), Type.DeleteLastZero,
                        deletedDelta);
            } else {
                history.set(lastIndex, new Delta(currentTime, newValue));
                recordedChange = new RecordedChange(playerScore.getPlayerNumber(), Type.ModifyLast,
                        new Delta(currentTime, value));
            }
        }

        playerScore.setScore(playerScore.getScore() + value);
        return recordedChange;
    }

    /**
     * Adds a zero-valued history entry, e.g. for a round in which the player scored nothing. Returns
     * the change to record for undo/redo.
     */
    public RecordedChange addZero(long currentTime) {
        Delta delta = new Delta(currentTime, 0);
        playerScore.getHistory().add(delta);
        lastIncremented.set(0);
        return new RecordedChange(playerScore.getPlayerNumber(), Type.AddNew, delta);
    }

    /**
     * Removes the last history entry and subtracts its value from the score. Returns the change to
     * record for undo/redo, or {@code null} when the history is already empty.
     */
    public RecordedChange deleteLast() {
        List<Delta> history = playerScore.getHistory();
        RecordedChange recordedChange = null;
        if (history != null && !history.isEmpty()) {
            Delta removed = history.remove(history.size() - 1);
            playerScore.setScore(playerScore.getScore() - removed.getValue());
            recordedChange = new RecordedChange(playerScore.getPlayerNumber(), Type.DeleteLast, removed);
        }
        lastIncremented.set(0);
        return recordedChange;
    }

    /** Resets the score to {@code initialScore} and empties the history, e.g. for a rematch. */
    public void reset(int initialScore) {
        playerScore.setScore(initialScore);
        playerScore.setHistory(new ArrayList<Delta>());
        lastIncremented.set(0);
    }

    /** Undoes a change recorded by {@link #applyDelta}, {@link #addZero} or {@link #deleteLast}. */
    public void revertChange(RecordedChange recordedChange) {
        Delta changeDelta = recordedChange.getDelta();
        switch (recordedChange.getType()) {
        case AddNew:
            playerScore.getHistory().remove(playerScore.getHistory().size() - 1);
            playerScore.setScore(playerScore.getScore() - changeDelta.getValue());
            break;
        case DeleteLast:
        case DeleteLastZero:
            playerScore.getHistory().add(changeDelta);
            playerScore.setScore(playerScore.getScore() + changeDelta.getValue());
            break;
        case ModifyLast:
        default:
            int lastIdx = playerScore.getHistory().size() - 1;
            Delta lastDelta = playerScore.getHistory().get(lastIdx);
            lastDelta.setValue(lastDelta.getValue() - changeDelta.getValue());
            playerScore.setScore(playerScore.getScore() - changeDelta.getValue());
            break;
        }
    }

    /** Redoes a change previously undone by {@link #revertChange}. */
    public void reexecuteChange(RecordedChange recordedChange) {
        Delta changeDelta = recordedChange.getDelta();
        switch (recordedChange.getType()) {
        case AddNew:
            playerScore.getHistory().add(changeDelta);
            playerScore.setScore(playerScore.getScore() + changeDelta.getValue());
            break;
        case DeleteLast:
        case DeleteLastZero:
            playerScore.getHistory().remove(playerScore.getHistory().size() - 1);
            playerScore.setScore(playerScore.getScore() - changeDelta.getValue());
            break;
        case ModifyLast:
        default:
            int lastIdx = playerScore.getHistory().size() - 1;
            Delta lastDelta = playerScore.getHistory().get(lastIdx);
            lastDelta.setValue(lastDelta.getValue() + changeDelta.getValue());
            playerScore.setScore(playerScore.getScore() + changeDelta.getValue());
            break;
        }
    }
}
