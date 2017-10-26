package kr.ac.snu.hcil.omnitrack.core.triggers.conditions

/**
 * Created by younghokim on 2017. 10. 18..
 */
abstract class ATriggerCondition(val type: Byte) : Cloneable {
    abstract fun getSerializedString(): String?

    abstract fun isConfigurationValid(validationErrorMessages: MutableList<CharSequence>?): Boolean

    override public fun clone(): Any {
        return super.clone()
    }
}