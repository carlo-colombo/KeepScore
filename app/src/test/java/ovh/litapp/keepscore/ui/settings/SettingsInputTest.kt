package ovh.litapp.keepscore.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ovh.litapp.keepscore.R

/**
 * Tests for the input filtering and validation that replace the legacy integer
 * EditTextPreference subclasses and their OnPreferenceChangeListeners.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsInputTest {

    @Test
    fun signedInputKeepsAtMostOneLeadingSign() {
        assertEquals("12", sanitizeIntegerInput("12", allowNegative = true))
        assertEquals("-12", sanitizeIntegerInput("-12", allowNegative = true))
        assertEquals("-12", sanitizeIntegerInput("-1-2", allowNegative = true))
        assertEquals("12", sanitizeIntegerInput("a1b2", allowNegative = true))
        assertEquals("34", sanitizeIntegerInput("3.4", allowNegative = true))
        assertEquals("5", sanitizeIntegerInput("+5", allowNegative = true))
        assertEquals("-", sanitizeIntegerInput("-", allowNegative = true))
        assertEquals("", sanitizeIntegerInput("", allowNegative = true))
    }

    @Test
    fun unsignedInputDropsTheSign() {
        assertEquals("12", sanitizeIntegerInput("12", allowNegative = false))
        assertEquals("12", sanitizeIntegerInput("-12", allowNegative = false))
        assertEquals("", sanitizeIntegerInput("-", allowNegative = false))
    }

    @Test
    fun toggleSignFlipsTheSign() {
        assertEquals("-5", toggleSign("5"))
        assertEquals("5", toggleSign("-5"))
        assertEquals("-", toggleSign(""))
    }

    @Test
    fun nonzeroValidationRejectsZeroAndNonNumbers() {
        assertEquals(R.string.toast_no_zeroes, integerValidationError(IntegerValidation.NON_ZERO, "0"))
        assertEquals(R.string.toast_no_zeroes, integerValidationError(IntegerValidation.NON_ZERO, ""))
        assertEquals(R.string.toast_no_zeroes, integerValidationError(IntegerValidation.NON_ZERO, "abc"))
        assertNull(integerValidationError(IntegerValidation.NON_ZERO, "5"))
        assertNull(integerValidationError(IntegerValidation.NON_ZERO, "-3"))
    }

    @Test
    fun initialScoreValidationOnlyRequiresANumber() {
        assertEquals(R.string.toast_valid_initial_score, integerValidationError(IntegerValidation.ANY_INTEGER, ""))
        assertEquals(R.string.toast_valid_initial_score, integerValidationError(IntegerValidation.ANY_INTEGER, "abc"))
        assertNull(integerValidationError(IntegerValidation.ANY_INTEGER, "0"))
        assertNull(integerValidationError(IntegerValidation.ANY_INTEGER, "-7"))
    }

    @Test
    fun updateDelayValidationBoundsTheValueToOneToSixHundred() {
        val error = R.string.toast_valid_update_delay_values
        assertEquals(error, integerValidationError(IntegerValidation.UPDATE_DELAY, "0"))
        assertEquals(error, integerValidationError(IntegerValidation.UPDATE_DELAY, "601"))
        assertEquals(error, integerValidationError(IntegerValidation.UPDATE_DELAY, "-1"))
        assertEquals(error, integerValidationError(IntegerValidation.UPDATE_DELAY, "abc"))
        assertEquals(error, integerValidationError(IntegerValidation.UPDATE_DELAY, ""))
        assertNull(integerValidationError(IntegerValidation.UPDATE_DELAY, "1"))
        assertNull(integerValidationError(IntegerValidation.UPDATE_DELAY, "600"))
    }
}
