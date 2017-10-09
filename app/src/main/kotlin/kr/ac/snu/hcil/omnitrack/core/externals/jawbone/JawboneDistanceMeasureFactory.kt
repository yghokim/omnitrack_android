package kr.ac.snu.hcil.omnitrack.core.externals.jawbone

import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 2017. 1. 25..
 */
object JawboneDistanceMeasureFactory : OTMeasureFactory("dist") {
    override val exampleAttributeType: Int = OTAttributeManager.TYPE_NUMBER

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_DISTANCE_ATTRIBUTE
    }

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Minute
    override val isDemandingUserInput: Boolean = false


    override fun makeMeasure(): OTMeasure {
        return JawboneDistanceMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return JawboneDistanceMeasure(serialized)
    }

    override fun getService(): OTExternalService {
        return JawboneUpService
    }

    override val descResourceId: Int = R.string.measure_jawbone_distance_desc
    override val nameResourceId: Int = R.string.measure_jawbone_distance_name

    class JawboneDistanceMeasure : AJawboneMoveMeasure {

        override fun extractValueFromItem(item: JsonObject): Float {
            return item.getAsJsonObject("details").get("distance").asFloat
        }

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_FLOAT

        override val factory: OTMeasureFactory = JawboneDistanceMeasureFactory

        constructor() : super()
        constructor(serialized: String) : super(serialized)

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is JawboneDistanceMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}