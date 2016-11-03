package kr.ac.snu.hcil.omnitrack.utils

import android.text.Html
import android.text.Spanned

/**
 * Created by younghokim on 16. 8. 16..
 */
object TextHelper {


    fun stringWithFallback(value: String?, nullFallback: String, blankFallback: String): String {
        return if (value != null) {
            if (value.isEmpty()) {
                blankFallback
            } else value
        } else {
            nullFallback
        }
    }

    fun stringWithFallback(value: String?, nullOrBlankFallback: String): String {
        return stringWithFallback(value, nullOrBlankFallback, nullOrBlankFallback)
    }

    fun fromHtml(source: String): Spanned {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(source)
        }
    }
}