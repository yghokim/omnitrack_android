package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.calculation.AConditioner
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.ObservableMapDelegate

/**
 * Created by younghokim on 16. 9. 5..
 */
class OTDataTrigger(objectId: String?, dbId: Long?, user: OTUser, name: String, trackerObjectIds: Array<String>, isOn: Boolean, action: Int, lastTriggeredTime: Long, serializedProperties: String? = null) : OTTrigger(objectId, dbId, user, name, trackerObjectIds, isOn, action, lastTriggeredTime, serializedProperties) {
    override val configIconId: Int = R.drawable.event_dark
    override val configTitleId: Int = R.string.trigger_desc_event

    override val descriptionResourceId: Int = R.string.trigger_desc_event
    override val typeNameResourceId: Int = R.string.trigger_name_event

    override val typeId: Int = TYPE_DATA_THRESHOLD


    var measure: OTMeasureFactory.OTMeasure?
        get() {
            val s = serializedMeasure
            if (s.isNullOrBlank()) return null
            else {
                val factory = OTExternalService.getMeasureFactoryByCode(typeCode = measureFactoryCode)
                if (factory != null) {
                    return factory.makeMeasure(s)
                } else return null
            }
        }
        set(value) {
            if(measure != value) {
                if (value == null) {
                    measureFactoryCode = ""
                    serializedMeasure = ""
                } else {
                    measureFactoryCode = value.factoryCode
                    serializedMeasure = value.getSerializedString()
                }

                onMeasureChanged()
            }
        }

    var conditioner: AConditioner?
        get() {
            val s = serializedConditioner
            if (s.isNullOrBlank()) return null
            else {
                return AConditioner.makeInstance(conditionerType, s)
            }
        }
        set(value) {
            if(conditioner != value) {
                if (value == null) {
                    conditionerType = -1
                    serializedConditioner = ""
                } else {
                    conditionerType = value.typeCode
                    serializedConditioner = value.getSerializedString()
                }

                onConditionerChanged()
            }
        }


    private var measureFactoryCode: String by ObservableMapDelegate("", properties) {
        isDirtySinceLastSync = true
    }

    private var serializedMeasure: String by ObservableMapDelegate("", properties) {
        isDirtySinceLastSync = true
    }

    private var conditionerType: Int by ObservableMapDelegate(-1, properties) {
        isDirtySinceLastSync = true
    }

    private var serializedConditioner: String by ObservableMapDelegate("", properties) {
        isDirtySinceLastSync = true
    }


    override fun handleActivationOnSystem(context: Context) {
        if (isOn) {
            OTDataTriggerManager.onEventTriggerOn(this)
        }
    }

    override fun handleOff() {
        OTDataTriggerManager.onEventTriggerOff(this)
    }

    override fun handleOn() {
        if(conditioner== null || measure == null)
        {
            isOn = false
        }
        else {
            OTDataTriggerManager.onEventTriggerOn(this)
        }
    }

    private fun onMeasureChanged(){
        OTDataTriggerManager.onEventTriggerOff(this)
        OTDataTriggerManager.onEventTriggerOn(this)
    }

    private fun onConditionerChanged(){
        OTDataTriggerManager.onEventTriggerOff(this)
        OTDataTriggerManager.onEventTriggerOn(this)
    }
}