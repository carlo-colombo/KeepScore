package ovh.litapp.keepscore.util

import android.text.Spannable
import android.text.Spanned

object SpannableUtil {

    /**
     * Set a span over the entire spannable.
     */
    @JvmStatic
    fun setWholeSpan(spannable: Spannable, what: Any) {
        spannable.setSpan(what, 0, spannable.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }
}
