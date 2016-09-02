package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import android.os.AsyncTask
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-02.
 */
object RescueTimeApi {
    enum class Mode {
        ApiKey, AccessToken
    }

    const val PARAM_API_KEY = "key"

    const val URL_ROOT = "rescuetime.com"
    const val SUBURL_ANALYTICS_API = "anapi"
    const val SUBURL_DATA = "data"
    const val SUBURL_SUMMARY = "daily_summary_feed"

    const val SUMMARY_VARIABLE_PRODUCTIVITY = "productivity_pulse"
    const val SUMMARY_VARIABLE_TOTAL_HOURS = "total_hours"

    val usageDurationCalculator = object : ISummaryCalculator<Double> {
        override fun calculate(list: List<JSONObject>, startDate: Date, endDate: Date): Double? {
            return if (list.size > 0)
                list.map { it.getDouble(SUMMARY_VARIABLE_TOTAL_HOURS) }.sum()
            else null
        }

    }

    val productivityCalculator = object : ISummaryCalculator<Double> {
        override fun calculate(list: List<JSONObject>, startDate: Date, endDate: Date): Double? {
            return if (list.size > 0) {
                list.map { it.getDouble(SUMMARY_VARIABLE_PRODUCTIVITY) }.sum() / list.size
            } else null
        }

    }

    fun queryUsageDuration(mode: Mode, startDate: Date, endDate: Date, resultHandler: (Double?) -> Unit) {
        SummaryCalculationTask<Double>(makeQueryRequest(mode, SUBURL_SUMMARY), startDate, endDate, usageDurationCalculator, resultHandler)
                .execute()
    }


    fun queryProductivityScore(mode: Mode, startDate: Date, endDate: Date, resultHandler: (Double?) -> Unit) {
        SummaryCalculationTask<Double>(makeQueryRequest(mode, SUBURL_SUMMARY), startDate, endDate, productivityCalculator, resultHandler)
                .execute()
    }

    private fun makeQueryRequest(mode: Mode, subUrl: String, parameters: Map<String, String> = mapOf()): Request {
        val uriBuilder = HttpUrl.Builder().scheme("https").host(URL_ROOT).addPathSegment(SUBURL_ANALYTICS_API).addPathSegments(subUrl)

        for (paramEntry in parameters) {
            uriBuilder.addQueryParameter(paramEntry.key, paramEntry.value)
        }

        if (mode == Mode.ApiKey)
            uriBuilder.addQueryParameter(PARAM_API_KEY, RescueTimeService.getStoredApiKey())

        println("query ${uriBuilder.build().toString()}")

        return Request.Builder()
                .url(uriBuilder.build())
                .get()
                .build()
    }

    interface ISummaryCalculator<T> {
        fun calculate(list: List<JSONObject>, startDate: Date, endDate: Date): T?
    }

    class SummaryCalculationTask<T>(val request: Request, val startDate: Date, val endDate: Date, val converter: ISummaryCalculator<T>, val resultHandler: (T?) -> Unit) : AsyncTask<Void?, Void?, T?>() {
        override fun onCancelled() {
            super.onCancelled()
            resultHandler.invoke(null)
        }

        override fun onPostExecute(result: T?) {
            super.onPostExecute(result)
            resultHandler.invoke(result)
        }

        override fun doInBackground(vararg p0: Void?): T? {

            val result = OkHttpClient().newCall(request).execute()

            val resultJsonString = result.body().string()
            if (resultJsonString != null) {
                println(resultJsonString)
                //find data of encompassing
                try {
                    val array = JSONArray(resultJsonString)

                    val list = ArrayList<JSONObject>()

                    for (i in 0..array.length() - 1) {
                        val element = array.getJSONObject(i)
                        val date = AuthConstants.DATE_FORMAT.parse(element.getString("date"))
                        if (date >= startDate && date < endDate) {
                            list.add(element)
                        }
                    }

                    return converter.calculate(list, startDate, endDate)
                } catch(e: Exception) {
                    e.printStackTrace()
                    return null
                }
            } else return null
        }

    }
}