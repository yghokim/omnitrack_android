package kr.ac.snu.hcil.android.common.containers

/**
 * Created by younghokim on 2016. 12. 22..
 */
open class Nullable<T>(val datum: T? = null) {
    operator fun component1(): T? = datum
}