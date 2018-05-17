package kr.ac.snu.hcil.omnitrack.core.triggers.conditions

import com.google.gson.JsonObject

/**
 * Created by younghokim on 2017. 10. 18..
 */
abstract class ATriggerCondition(val type: Byte) : Cloneable {
    abstract fun getSerializedString(): String

    abstract fun isConfigurationValid(validationErrorMessages: MutableList<CharSequence>?): Boolean

    abstract fun writeEventLogContent(table: JsonObject)

    public override fun clone(): Any {
        return super.clone()
    }
}