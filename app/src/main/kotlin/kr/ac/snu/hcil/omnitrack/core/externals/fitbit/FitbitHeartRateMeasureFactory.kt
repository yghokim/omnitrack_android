package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttribute
import kr.ac.snu.hcil.omnitrack.core.connection.OTTimeRangeQuery
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.serialization.SerializableTypedQueue
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-10-06.
 */
object FitbitHeartRateMeasureFactory : OTMeasureFactory() {


    const val REQUEST_URL_HEART_RATE_INTRADAY_COMMAND_FORMAT = "https://api.fitbit.com/1/user/-/activities/heart/date/%s/%s/1min/time/%s/%s.json"
    val REQUEST_TIME_FORMAT = SimpleDateFormat("HH:mm")

    fun makeRequestUrl(start: Date, end: Date): String {
        return String.format(REQUEST_URL_HEART_RATE_INTRADAY_COMMAND_FORMAT, AuthConstants.DATE_FORMAT.format(start), AuthConstants.DATE_FORMAT.format(end), REQUEST_TIME_FORMAT.format(start), REQUEST_TIME_FORMAT.format(end))
    }

    override val service: OTExternalService = FitbitService

    override fun isAttachableTo(attribute: OTAttribute<out Any>): Boolean {
        return attribute.typeId == OTAttribute.TYPE_NUMBER
    }

    override val isRangedQueryAvailable: Boolean = true
    override val isDemandingUserInput: Boolean = false

    override fun makeMeasure(): OTMeasure {
        return FitbitHeartRateMeasure()
    }

    override fun makeMeasure(serialized: String): OTMeasure {
        return FitbitHeartRateMeasure(serialized)
    }

    override val exampleAttributeType: Int
        get() = OTAttribute.TYPE_NUMBER

    override fun getExampleAttributeConfigurator(): IExampleAttributeConfigurator = OTMeasureFactory.CONFIGURATOR_FOR_HEART_RATE_ATTRIBUTE

    override val nameResourceId: Int = R.string.measure_fitbit_heart_rate_name
    override val descResourceId: Int = R.string.measure_fitbit_heart_rate_desc

    class FitbitHeartRateMeasure : OTRangeQueriedMeasure {
        companion object {
            val converter = object : OAuth2Client.OAuth2RequestConverter<Int?> {
                override fun process(requestResultStrings: Array<String>): Int? {

                    if (requestResultStrings.size == 0) return null
                    else {
                        var sum = 0
                        var totalCount = 0
                        for (requestResultString in requestResultStrings) {
                            println(requestResultString)
                            val json = JSONObject(requestResultString)
                            val intradayDataSet = json.getJSONObject("activities-heart-intraday").getJSONArray("dataset")
                            val count = intradayDataSet.length()
                            totalCount += count

                            if (count == 0) {
                                continue
                            } else {
                                for (i in 0..count - 1) {
                                    sum += intradayDataSet.getJSONObject(i).getInt("value")
                                }
                            }
                        }

                        if (totalCount > 0)
                            return Math.round(sum.toFloat() / totalCount)
                        else return null
                    }
                }
            }
        }

        override fun requestValueAsync(start: Long, end: Long, handler: (Any?) -> Unit) {
            val url = makeRequestUrl(Date(start), Date(end - 60000))
            println(url)
            FitbitService.request(url, converter) {
                result ->
                handler.invoke(result)
            }
        }

        override val dataTypeName: String = TypeStringSerializationHelper.TYPENAME_INT
        override val factory: OTMeasureFactory = FitbitHeartRateMeasureFactory

        override fun awaitRequestValue(query: OTTimeRangeQuery?): Any {
            return 0
        }

        override fun onSerialize(typedQueue: SerializableTypedQueue) {
        }

        override fun onDeserialize(typedQueue: SerializableTypedQueue) {
        }


        constructor() : super()
        constructor(serialized: String) : super(serialized)
    }
}