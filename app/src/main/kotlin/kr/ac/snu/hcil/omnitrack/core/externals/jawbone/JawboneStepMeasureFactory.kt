package kr.ac.snu.hcil.omnitrack.core.externals.jawbone

import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.configured.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 2017. 1. 25..
 */
object JawboneStepMeasureFactory : OTMeasureFactory("step") {
    override val exampleAttributeType: Int = OTAttributeManager.TYPE_NUMBER

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_STEP_ATTRIBUTE
    }

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override fun getAttributeType() = OTAttributeManager.TYPE_NUMBER

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Minute
    override val isDemandingUserInput: Boolean = false


    override fun makeMeasure(): OTMeasure {
        return JawboneStepMeasure()
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return JawboneStepMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return JawboneStepMeasure()
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override fun getService(): OTExternalService {
        return JawboneUpService
    }

    override val descResourceId: Int = R.string.measure_steps_desc
    override val nameResourceId: Int = R.string.measure_steps_name

    class JawboneStepMeasure : AJawboneMoveMeasure() {

        override fun extractValueFromItem(item: JsonObject): Float {
            return item.getAsJsonObject("details").get("steps").asFloat
        }

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_FLOAT

        override val factory: OTMeasureFactory = JawboneStepMeasureFactory

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is JawboneStepMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}