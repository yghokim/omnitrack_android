package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import rx.Observable
import java.util.*

/**
 * Created by younghokim on 16. 9. 3..
 */
object FitbitStepCountMeasureFactory : OTMeasureFactory() {

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator {
        return CONFIGURATOR_STEP_ATTRIBUTE
    }

    override val supportedConditionerTypes: IntArray = CONDITIONERS_FOR_SINGLE_NUMERIC_VALUE

    override val exampleAttributeType: Int = OTAttribute.TYPE_NUMBER

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return FitbitStepMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitStepMeasure(serialized)
    }

    override val service: OTExternalService = FitbitService

    override val descResourceId: Int = R.string.measure_fitbit_steps_desc
    override val nameResourceId: Int = R.string.measure_fitbit_steps_name


    class FitbitStepMeasure : OTRangeQueriedMeasure {

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT

        override val factory: OTMeasureFactory = FitbitStepCountMeasureFactory

        val converter = object : OAuth2Client.OAuth2RequestConverter<Int?> {
            override fun process(requestResultStrings: Array<String>): Int? {
                val json = JSONObject(requestResultStrings.first())
                println("convert $json")
                if (json.has("summary")) {
                    val steps = json.getJSONObject("summary").getInt("steps")
                    return steps

                } else return null
            }

        }

        constructor() : super()
        constructor(serialized: String) : super(serialized)


        override fun getValueRequest(start: Long, end: Long): Observable<Result<out Any>> {
            return FitbitService.getRequest(
                    converter,
                    FitbitService.makeRequestUrlWithCommandAndDate(FitbitService.REQUEST_COMMAND_SUMMARY, Date(start)))
                    as Observable<Result<out Any>>
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            else return other is FitbitStepMeasure
        }

        override fun hashCode(): Int {
            return factoryCode.hashCode()
        }
    }
}