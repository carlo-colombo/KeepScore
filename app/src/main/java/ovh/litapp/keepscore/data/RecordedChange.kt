package ovh.litapp.keepscore.data

import ovh.litapp.keepscore.db.Delta

/**
 * Simple POJO representing a change made to a PlayerScore's history
 *
 * @author nolan
 */
class RecordedChange(val playerNumber: Int, val type: Type, val delta: Delta) {

    enum class Type {
        /** New value is added to the score history */
        AddNew,

        /** The last value in the score history is modified */
        ModifyLast,

        /** The last value in the score history is deleted manually using the "delete last" menu item */
        DeleteLast,

        /**
         * The last value in the score history is deleted, but only because otherwise
         * a +0 would have been added, due to a + or - button being pressed
         */
        DeleteLastZero,
    }
}
