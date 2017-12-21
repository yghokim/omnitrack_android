package kr.ac.snu.hcil.omnitrack.utils

/**
 * Created by younghokim on 2016. 12. 22..
 */
open class Nullable<T>(val datum: T? = null) {
    companion object {
        val NULL: Nullable<out Any> by lazy {
            Nullable(null)
        }
    }
    operator fun component1(): T? = datum
}