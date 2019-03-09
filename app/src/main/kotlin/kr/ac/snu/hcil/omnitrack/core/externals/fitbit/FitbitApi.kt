package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import kr.ac.snu.hcil.android.common.net.AuthConstants
import kr.ac.snu.hcil.android.common.net.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.time.TimeHelper
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by younghokim on 2016. 12. 23..
 */
object FitbitApi {

    const val REQUEST_INTRADAY_RESOURCE_PATH_HEART_RATE = "activities/heart"
    const val REQUEST_INTRADAY_RESOURCE_PATH_STEPS = "activities/steps"
    const val REQUEST_INTRADAY_RESOURCE_PATH_DISTANCE = "activities/distance"


    const val REQUEST_COMMAND_SUMMARY = "activities"
    const val REQUEST_COMMAND_SLEEP = "sleep"


    const val REQUEST_URL_SIMPLE_COMMAND_DATE_FORMAT = "https://api.fitbit.com/1/user/-/%s/date/%s.json"
    const val REQUEST_URL_INTRADAY_COMMAND_FORMAT = "https://api.fitbit.com/1/user/-/%s/date/%s/1d/1min/time/%s/%s.json"
    val REQUEST_TIME_FORMAT = SimpleDateFormat("HH:mm")


    fun makeDailyRequestUrl(command: String, date: Date): String {
        return String.format(REQUEST_URL_SIMPLE_COMMAND_DATE_FORMAT, command, AuthConstants.DATE_FORMAT.format(date))
    }

    fun makeIntraDayRequestUrls(resourcePath: String, start: Long, end: Long): Array<String> {
        return TimeHelper.sliceToDate(start, end).map {
            String.format(REQUEST_URL_INTRADAY_COMMAND_FORMAT, resourcePath, AuthConstants.DATE_FORMAT.format(it.first), REQUEST_TIME_FORMAT.format(it.first), REQUEST_TIME_FORMAT.format(it.second))
        }.toTypedArray()
    }

    abstract class AIntraDayConverter<ValueType, ResultType>(val dataSetKey: String) : OAuth2Client.OAuth2RequestConverter<ResultType?> {
        override fun process(requestResultStrings: Array<String>): ResultType? {

            if (requestResultStrings.isEmpty()) return null
            else {
                val values = ArrayList<ValueType>()

                for (requestResultString in requestResultStrings) {
                    println(requestResultString)
                    val json = JSONObject(requestResultString)
                    val intradayDataSet = json.getJSONObject(dataSetKey).getJSONArray("dataset")
                    val count = intradayDataSet.length()

                    if (count == 0) {
                        continue
                    } else {
                        for (i in 0 until count) {
                            values += extractValueFromDatum(intradayDataSet.getJSONObject(i))
                        }
                    }
                }

                if (values.isNotEmpty())
                    return processValues(values)
                else return null
            }
        }

        abstract fun extractValueFromDatum(datum: JSONObject): ValueType


        abstract fun processValues(values: List<ValueType>): ResultType
    }
}