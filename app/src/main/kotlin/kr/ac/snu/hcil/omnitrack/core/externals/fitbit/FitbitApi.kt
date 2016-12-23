package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import android.text.format.DateUtils
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import kr.ac.snu.hcil.omnitrack.utils.setHourOfDay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by younghokim on 2016. 12. 23..
 */
object FitbitApi {

    const val REQUEST_INTRADAY_RESOURCE_PATH_HEART_RATE = "activities/heart"
    const val REQUEST_INTRADAY_RESOURCE_PATH_STEPS = "activities/steps"
    const val REQUEST_INTRADAY_RESOURCE_PATH_DISTANCES = "activities/distance"


    const val REQUEST_COMMAND_SUMMARY = "activities"
    const val REQUEST_COMMAND_SLEEP = "sleep"


    const val REQUEST_URL_SIMPLE_COMMAND_DATE_FORMAT = "https://api.fitbit.com/1/user/-/%s/date/%s.json"
    const val REQUEST_URL_INTRADAY_COMMAND_FORMAT = "https://api.fitbit.com/1/user/-/%s/date/%s/1d/1min/time/%s/%s.json"
    val REQUEST_TIME_FORMAT = SimpleDateFormat("HH:mm")


    fun makeDailyRequestUrl(command: String, date: Date): String {
        return String.format(REQUEST_URL_SIMPLE_COMMAND_DATE_FORMAT, command, AuthConstants.DATE_FORMAT.format(date))
    }

    fun makeIntraDayRequestUrls(resourcePath: String, start: Long, end: Long): Array<String> {
        val urls = ArrayList<String>()

        val points: Array<Long?>

        val startCal = Calendar.getInstance()
        startCal.timeInMillis = start

        val dateDiff = (end / DateUtils.DAY_IN_MILLIS).toInt() - (start / DateUtils.DAY_IN_MILLIS).toInt()

        if (dateDiff <= 0) {
            points = arrayOf(start, end)
        } else {
            points = arrayOfNulls<Long>(if (end % DateUtils.DAY_IN_MILLIS == 0L) {
                (2 + dateDiff - 1)
            } else {
                2 + dateDiff
            })

            points[0] = start

            startCal.setHourOfDay(0, true)
            for (i in 1..dateDiff) {
                points[i] = startCal.timeInMillis + i * DateUtils.DAY_IN_MILLIS
            }

            if (end % DateUtils.DAY_IN_MILLIS != 0L) {
                points[points.size - 1] = end
            }
        }

        var startDate: Date
        var endDate: Date
        for (i in 0..(points.size - 2)) {
            startDate = Date(points[i]!!)
            endDate = if (i + 1 < points.size - 1) {
                Date(points[i + 1]!! - DateUtils.MINUTE_IN_MILLIS)
            } else {
                Date(points[i + 1]!!)
            }

            urls.add(
                    String.format(REQUEST_URL_INTRADAY_COMMAND_FORMAT, resourcePath, AuthConstants.DATE_FORMAT.format(startDate), REQUEST_TIME_FORMAT.format(startDate), REQUEST_TIME_FORMAT.format(endDate))
            )
        }


        return urls.toTypedArray()
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
                        for (i in 0..count - 1) {
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