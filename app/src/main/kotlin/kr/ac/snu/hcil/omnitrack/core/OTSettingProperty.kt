package kr.ac.snu.hcil.omnitrack.core

/**
 * Created by younghokim on 16. 7. 12..
 */
abstract class OTSettingProperty<T>(val name : String) {
    var initializedValue: T? = null
    var value: T? = null

    open var layoutId : Int = 0
        protected set

    fun initialize() {
        initializedValue = value
    }

    val isPropertyChanged: Boolean
        get() = initializedValue == value

}