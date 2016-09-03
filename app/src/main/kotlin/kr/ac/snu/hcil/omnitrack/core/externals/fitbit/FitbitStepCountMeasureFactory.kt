package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTItemBuilder
import kr.ac.snu.hcil.omnitrack.core.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
object FitbitStepCountMeasureFactory : OTMeasureFactory() {
    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true

    override fun makeMeasure(): OTMeasure {
        return FitbitStepMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitStepMeasure(serialized)
    }

    override val service: OTExternalService = FitbitService

    override val descResourceId: Int = R.string.measure_fitbit_steps_desc
    override val nameResourceId: Int = R.string.measure_fitbit_steps_name


    class FitbitStepMeasure : OTMeasure {

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_BIGDECIMAL

        override val factory: OTMeasureFactory = FitbitStepCountMeasureFactory

        val converter = object : OAuth2Client.OAuth2RequestConverter<Int?> {
            override fun process(requestResultString: String): Int? {
                println(requestResultString)
                val json = JSONObject(requestResultString)
                if (json.has("summary")) {
                    val steps = json.getJSONObject("summary").getInt("steps")
                    return steps

                } else return null
            }

        }

        constructor() : super()
        constructor(serialized: String) : super(serialized)


        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun requestValueAsync(builder: OTItemBuilder, query: OTTimeRangeQuery?, handler: (Any?) -> Unit) {
            val range = query!!.getRange(builder)
            FitbitService.request(
                    FitbitService.makeRequestUrlWithCommandAndDate(FitbitService.REQUEST_COMMAND_SUMMARY, Date(range.first)),
                    converter)
            {
                result ->
                handler.invoke(result)
            }
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

    }
}