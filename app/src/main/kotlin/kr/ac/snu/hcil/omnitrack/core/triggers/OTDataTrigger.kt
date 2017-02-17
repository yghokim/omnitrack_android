package kr.ac.snu.hcil.omnitrack.core.triggers

import android.content.Context
import com.google.firebase.database.DataSnapshot
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.calculation.AConditioner
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.ObservableMapDelegate

/**
 * Created by younghokim on 16. 9. 5..
 */
class OTDataTrigger(objectId: String?, user: OTUser, name: String, trackerObjectIds: Array<Pair<String?, String>>?, isOn: Boolean, action: Int, lastTriggeredTime: Long, propertyData: DataSnapshot? = null) : OTTrigger(objectId, user, name, trackerObjectIds, isOn, action, lastTriggeredTime, propertyData) {

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
        value ->
        syncPropertyToDatabase("measureFactoryCode", value)
    }

    private var serializedMeasure: String by ObservableMapDelegate("", properties) {
        value ->
        syncPropertyToDatabase("serializedMeasure", value)
    }

    private var conditionerType: Int by ObservableMapDelegate(-1, properties) {
        value ->
        syncPropertyToDatabase("conditionerType", value)
    }

    private var serializedConditioner: String by ObservableMapDelegate("", properties) {
        value ->
        syncPropertyToDatabase("serializedConditioner", value)
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

    override fun detachFromSystem() {
        OTDataTriggerManager.onEventTriggerOff(this)
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