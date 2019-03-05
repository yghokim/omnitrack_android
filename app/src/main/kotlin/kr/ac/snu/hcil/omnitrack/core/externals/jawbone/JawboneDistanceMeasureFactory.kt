package kr.ac.snu.hcil.omnitrack.core.externals.jawbone

import android.content.Context
import com.google.gson.JsonObject
import com.google.gson.stream.JsonReader
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by younghokim on 2017. 1. 25..
 */
class JawboneDistanceMeasureFactory(context: Context, service: JawboneUpService) : OTServiceMeasureFactory(context, service, "dist") {
    override val exampleAttributeType: Int = OTAttributeManager.TYPE_NUMBER

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_DISTANCE_ATTRIBUTE
    }

    override fun isAttachableTo(attribute: OTAttributeDAO): Boolean {
        return attribute.type == OTAttributeManager.TYPE_NUMBER
    }

    override fun getAttributeType() = OTAttributeManager.TYPE_NUMBER

    override val isRangedQueryAvailable: Boolean = true
    override val minimumGranularity: OTTimeRangeQuery.Granularity = OTTimeRangeQuery.Granularity.Minute
    override val isDemandingUserInput: Boolean = false


    override fun makeMeasure(): OTMeasure {
        return JawboneDistanceMeasure(this)
    }

    override fun makeMeasure(reader: JsonReader): OTMeasure {
        return JawboneDistanceMeasure(this)
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return JawboneDistanceMeasure(this)
    }

    override fun serializeMeasure(measure: OTMeasure): String {
        return "{}"
    }

    override val descResourceId: Int = R.string.measure_jawbone_distance_desc
    override val nameResourceId: Int = R.string.measure_jawbone_distance_name
    override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_FLOAT

    class JawboneDistanceMeasure(factory: JawboneDistanceMeasureFactory) : AJawboneMoveMeasure(factory) {

        override fun extractValueFromItem(item: JsonObject): Float {
            return item.getAsJsonObject("details").get("distance").asFloat
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