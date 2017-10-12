package kr.ac.snu.hcil.omnitrack.utils

/**
 * Created by younghokim on 2017. 10. 12..
 */
class DataRequestResult<T>(datum: T? = null, vararg errors: Pair<Exception, CharSequence>) : Nullable<T>() {
    val errorMessages = ArrayList<Pair<Exception, CharSequence>>(errors.size)

    init {
        errorMessages.addAll(errors)
    }
}