package kr.ac.snu.hcil.omnitrack.utils

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
}