package kr.ac.snu.hcil.omnitrack.core.externals.rescuetime

import android.os.Looper
import com.badoo.mobile.util.WeakHandler
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
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

    val mainHandler: WeakHandler by lazy {
        WeakHandler(Looper.getMainLooper())
    }

    fun queryUsageDuration(mode: Mode, startDate: Date, endDate: Date, resultHandler: (Double?) -> Unit) {
        querySummary(mode, startDate, endDate)
        {
            resultList ->
            if (resultList.size > 0) {
                val sum = resultList.map { it.getDouble(SUMMARY_VARIABLE_TOTAL_HOURS) }.sum()
                resultHandler.invoke(sum)
            } else {
                resultHandler.invoke(null)
            }
        }
    }


    fun queryProductivityScore(mode: Mode, startDate: Date, endDate: Date, resultHandler: (Double?) -> Unit) {
        querySummary(mode, startDate, endDate)
        {
            resultList ->
            if (resultList.size > 0) {
                val average = resultList.map { it.getDouble(SUMMARY_VARIABLE_PRODUCTIVITY) }.sum() / resultList.size
                resultHandler.invoke(average)
            } else {
                resultHandler.invoke(null)
            }
        }
    }

    fun querySummary(mode: Mode, startDate: Date, endDate: Date, resultHandler: (List<JSONObject>) -> Unit) {
        query(mode, SUBURL_SUMMARY, mapOf()) {
            resultJsonString ->
            if (resultJsonString != null) {
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

                    resultHandler.invoke(list)
                } catch(e: Exception) {
                    e.printStackTrace()
                    resultHandler.invoke(listOf())
                }
            }
        }
    }

    private fun query(mode: Mode, subUrl: String, parameters: Map<String, String>, resultHandler: (String?) -> Unit) {
        val uriBuilder = HttpUrl.Builder().scheme("https").host(URL_ROOT).addPathSegment(SUBURL_ANALYTICS_API).addPathSegments(subUrl)

        for (paramEntry in parameters) {
            uriBuilder.addQueryParameter(paramEntry.key, paramEntry.value)
        }

        if (mode == Mode.ApiKey)
            uriBuilder.addQueryParameter(PARAM_API_KEY, RescueTimeService.getStoredApiKey())

        println("query ${uriBuilder.build().toString()}")

        val request = Request.Builder()
                .url(uriBuilder.build())
                .get()
                .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                mainHandler.post {
                    resultHandler.invoke(null)
                }
            }

            override fun onResponse(call: Call?, response: Response?) {
                mainHandler.post {
                    resultHandler.invoke(response?.body()?.string())
                }
            }

        })
    }
}