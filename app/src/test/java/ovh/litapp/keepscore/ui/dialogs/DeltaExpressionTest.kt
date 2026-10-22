package ovh.litapp.keepscore.ui.dialogs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the calculator behaviour the legacy additional-deltas dialog had: the characters the
 * field took, the shortcut that hid the delta buttons and the `=` evaluation.
 */
class DeltaExpressionTest {

    @Test
    fun expressionEvaluationRoundsUpLikeTheLegacyCalculator() {
        assertEquals("12", evaluateDeltaExpression("3*4"))
        assertEquals("3", evaluateDeltaExpression("10/4"))
        assertEquals("0", evaluateDeltaExpression("0"))
        assertEquals("-7", evaluateDeltaExpression("-7.5"))
        assertEquals("7", evaluateDeltaExpression("6.5+0.25"))
    }

    @Test
    fun unparsableExpressionsReturnNull() {
        assertNull(evaluateDeltaExpression(""))
        assertNull(evaluateDeltaExpression("2+"))
        assertNull(evaluateDeltaExpression("abc"))
        assertNull(evaluateDeltaExpression("2=3"))
    }

    @Test
    fun inputKeepsOnlyTheCharactersTheLegacyFieldAllowed() {
        assertEquals("12", applyDeltaInput("1a2").text)
        assertEquals("13", applyDeltaInput("1e3").text)
        assertEquals("3*(4+5)", applyDeltaInput("3*(4+5)").text)
        // the legacy digits list allowed spaces, so they survive like any other allowed character
        assertEquals(" -5 ", applyDeltaInput(" -5 ").text)
        assertFalse(applyDeltaInput("12").calculationFailed)
    }

    @Test
    fun typedEqualsEvaluatesTheExpression() {
        val transition = applyDeltaInput("3*4=")

        assertEquals("12", transition.text)
        assertFalse(transition.calculationFailed)
    }

    @Test
    fun typedEqualsOnAnInvalidExpressionKeepsTheTextWithoutIt() {
        val transition = applyDeltaInput("2+=")

        assertEquals("2+", transition.text)
        assertTrue(transition.calculationFailed)
    }

    @Test
    fun operatorsMakeTheFieldAnExpression() {
        assertFalse(isDeltaExpression("5"))
        assertFalse(isDeltaExpression(""))
        assertTrue(isDeltaExpression("-5"))
        assertTrue(isDeltaExpression("3*4"))
        assertTrue(isDeltaExpression("10/2"))
        assertTrue(isDeltaExpression("1+1"))
    }
}
