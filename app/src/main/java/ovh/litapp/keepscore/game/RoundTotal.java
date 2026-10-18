package ovh.litapp.keepscore.game;

/**
 * The current round of a game and the sum of every player's score for that round, as shown by the
 * round-total strip.
 */
public class RoundTotal {

    private final int round;
    private final int total;

    public RoundTotal(int round, int total) {
        this.round = round;
        this.total = total;
    }

    public int getRound() {
        return round;
    }

    public int getTotal() {
        return total;
    }
}
