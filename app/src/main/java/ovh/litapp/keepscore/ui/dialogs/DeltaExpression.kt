package ovh.litapp.keepscore.ui.dialogs

import de.congrace.exp4j.ExpressionBuilder
import java.text.DecimalFormat

/** The value the additional-deltas field opens with, selected so the first keystroke replaces it. */
internal const val DELTA_INITIAL_VALUE = "0"

/** The characters the legacy field accepted through its `android:digits` attribute. */
private const val DELTA_INPUT_CHARACTERS = "0123456789-+*/=() "

/** The characters that turn the field into an expression, which hides the shortcut buttons. */
private const val EXPRESSION_CHARACTERS = "+-*/"

/** The shortcut that evaluates the expression without pressing OK. */
private const val CALCULATE_CHARACTER = '='

/**
 * The text the additional-deltas field shows after a keystroke, and whether the expression the
 * `=` shortcut evaluated could not be parsed.
 *
 * The legacy field reported a parse failure with a toast and kept the text it could not evaluate,
 * so the dialog stays open for the user to fix it.
 */
internal class DeltaInputTransition(val text: String, val calculationFailed: Boolean)

/**
 * What pressing a key does to the additional-deltas field: keeps the characters the legacy field
 * allowed and, when the text contains `=`, evaluates what comes before it.
 */
internal fun applyDeltaInput(input: String): DeltaInputTransition {
    val sanitized = input.filter { it in DELTA_INPUT_CHARACTERS }
    val equalsIndex = sanitized.indexOf(CALCULATE_CHARACTER)
    if (equalsIndex < 0) {
        return DeltaInputTransition(sanitized, calculationFailed = false)
    }
    val expression = sanitized.removeRange(equalsIndex, equalsIndex + 1)
    val result = evaluateDeltaExpression(expression)
    return if (result == null) {
        DeltaInputTransition(expression, calculationFailed = true)
    } else {
        DeltaInputTransition(result, calculationFailed = false)
    }
}

/** Whether [input] is an expression, i.e. whether the legacy field hid its shortcut buttons. */
internal fun isDeltaExpression(input: String): Boolean = input.any { it in EXPRESSION_CHARACTERS }

/**
 * The value of [input] as a whole number, or null when it is not a parsable expression.
 *
 * The result is rounded up like the legacy calculator, so `10/4` becomes `3` and `-7.5` becomes
 * `-7`.
 */
internal fun evaluateDeltaExpression(input: String): String? =
    try {
        val value = Math.ceil(ExpressionBuilder(input).build().calculate())
        DecimalFormat("#").format(value)
    } catch (e: Exception) {
        null
    }
