package ovh.litapp.keepscore.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ovh.litapp.keepscore.data.RecordedChange;
import ovh.litapp.keepscore.data.RecordedChange.Type;
import ovh.litapp.keepscore.db.Delta;
import ovh.litapp.keepscore.db.PlayerScore;
import ovh.litapp.keepscore.util.DataExpiringStack;
import ovh.litapp.keepscore.util.Pair;

/**
 * The game-wide scoring state: the undo/redo history and the values derived from the players' score
 * histories that the scoreboard shows (the current round total and which player was updated last).
 *
 * <p>Undo/redo only decides <em>which</em> changes to revert or re-execute; applying them to a
 * player is the caller's job, which keeps the stack logic free of any view dependency.
 */
public class GameScoringModel {

    /** How many changes to keep in memory. */
    public static final int UNDO_STACK_SIZE = 500;

    @SuppressWarnings("unchecked")
    private static final Set<Pair<Type, Type>> ACCEPTABLE_UNDO_TRANSITIONS = new HashSet<Pair<Type, Type>>(
            Arrays.asList(Pair.create(Type.ModifyLast, Type.ModifyLast), Pair.create(Type.ModifyLast, Type.AddNew),
                    Pair.create(Type.DeleteLastZero, Type.AddNew), Pair.create(Type.DeleteLastZero, Type.ModifyLast),
                    Pair.create(Type.ModifyLast, Type.DeleteLastZero),
                    Pair.create(Type.DeleteLastZero, Type.DeleteLastZero)));
    @SuppressWarnings("unchecked")
    private static final Set<Pair<Type, Type>> ACCEPTABLE_REDO_TRANSITIONS = new HashSet<Pair<Type, Type>>(
            Arrays.asList(Pair.create(Type.ModifyLast, Type.ModifyLast), Pair.create(Type.AddNew, Type.ModifyLast),
                    Pair.create(Type.AddNew, Type.DeleteLastZero), Pair.create(Type.DeleteLastZero, Type.ModifyLast),
                    Pair.create(Type.ModifyLast, Type.DeleteLastZero),
                    Pair.create(Type.DeleteLastZero, Type.DeleteLastZero)));

    private final List<PlayerScore> playerScores;
    private final DataExpiringStack<RecordedChange> undoStack = new DataExpiringStack<RecordedChange>(
            UNDO_STACK_SIZE);
    private final DataExpiringStack<RecordedChange> redoStack = new DataExpiringStack<RecordedChange>(
            UNDO_STACK_SIZE);

    public GameScoringModel(List<PlayerScore> playerScores) {
        this.playerScores = playerScores;
    }

    /** Records a change so it can be undone, invalidating any redo history. */
    public void recordChange(RecordedChange recordedChange) {
        undoStack.pop(recordedChange);
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clearUndoRedo() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * Pops and returns the changes an undo should revert, most recently recorded first. Stops at the
     * first change that targets a different player or that is not an acceptable transition from the
     * previous one, because consecutive edits to one player form a single user action.
     */
    public List<RecordedChange> undoBatch() {
        return pollBatch(undoStack, redoStack, ACCEPTABLE_UNDO_TRANSITIONS);
    }

    /** Like {@link #undoBatch()}, but for redoing changes that were previously undone. */
    public List<RecordedChange> redoBatch() {
        return pollBatch(redoStack, undoStack, ACCEPTABLE_REDO_TRANSITIONS);
    }

    private List<RecordedChange> pollBatch(DataExpiringStack<RecordedChange> stackToPoll,
            DataExpiringStack<RecordedChange> stackToPush, Set<Pair<Type, Type>> acceptableTransitions) {

        List<RecordedChange> batch = new ArrayList<RecordedChange>();

        int lastPlayerNumber = -1;
        Type lastType = null;
        RecordedChange recordedChange;
        while ((recordedChange = stackToPoll.peek()) != null
                && (lastPlayerNumber == -1 || lastPlayerNumber == recordedChange.getPlayerNumber())
                // only apply multiple changes to the same PlayerScore
                && (lastType == null
                        || acceptableTransitions.contains(Pair.create(lastType, recordedChange.getType())))) {

            recordedChange = stackToPoll.poll();
            batch.add(recordedChange);
            stackToPush.pop(recordedChange);

            lastPlayerNumber = recordedChange.getPlayerNumber();
            lastType = recordedChange.getType();
        }

        return batch;
    }

    /**
     * Whether a change of {@code type} may follow a change of {@code lastType} in a single undo or
     * redo. "Modify last" edits are treated as part of the action they were batched into.
     */
    public static boolean isAcceptableUndoOrRedoTransition(boolean undo, Type lastType, Type type) {
        return undo ? ACCEPTABLE_UNDO_TRANSITIONS.contains(Pair.create(lastType, type))
                : ACCEPTABLE_REDO_TRANSITIONS.contains(Pair.create(lastType, type));
    }

    /**
     * The round the scoreboard is on (the longest player history) and the sum of every player's
     * score for that round. A player whose history is shorter counts as zero for the round.
     */
    public RoundTotal computeRoundTotal() {
        int round = 0;
        for (PlayerScore playerScore : playerScores) {
            round = Math.max(round, playerScore.getHistory().size());
        }

        if (round == 0) {
            return new RoundTotal(0, 0);
        }

        int total = 0;
        for (PlayerScore playerScore : playerScores) {
            List<Delta> history = playerScore.getHistory();
            if (history.size() >= round) {
                total += history.get(round - 1).getValue();
            }
        }
        return new RoundTotal(round, total);
    }

    /**
     * The index of the player whose score was updated most recently, or -1 when no player has been
     * updated, e.g. a game saved before updates were tracked. The earliest player wins ties.
     */
    public int getHighlightedPlayerIndex() {
        long maxLastUpdate = 0;
        int maxLastUpdateIdx = -1;

        for (int i = 0; i < playerScores.size(); i++) {
            PlayerScore playerScore = playerScores.get(i);
            if (playerScore.getLastUpdate() > maxLastUpdate) {
                maxLastUpdate = playerScore.getLastUpdate();
                maxLastUpdateIdx = i;
            }
        }

        return maxLastUpdateIdx;
    }
}
