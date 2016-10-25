package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import android.os.AsyncTask
import android.support.v4.app.FragmentActivity
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */

object MisfitApi {

    const val URL_ROOT = "api.misfitwearables.com"
    const val SUBURL_LOGIN = "auth/dialog/authorize"
    const val SUBURL_EXCHANGE = "auth/tokens/exchange"
    const val SUBURL_ACTIVITY = "move/resource/v1/user/me/activity"
    const val SUBURL_ACTIVITY_SUMMARY = "summary"
    const val SUBURL_ACTIVITY_SLEEP = "sleeps"

    const val QUERY_START_DATE = "start_date"
    const val QUERY_END_DATE = "end_date"


    init {

    }

    fun makeUriBuilderRoot(): HttpUrl.Builder {
        return HttpUrl.Builder().scheme("https").host(URL_ROOT)
    }

    fun authorize(activity: FragmentActivity) {
        val uri = makeUriBuilderRoot().addPathSegments(SUBURL_LOGIN)
                .addQueryParameter(AuthConstants.PARAM_CLIENT_ID, activity.resources.getString(R.string.misfit_app_key))
                .addQueryParameter(AuthConstants.PARAM_RESPONSE_TYPE, AuthConstants.VALUE_RESPONSE_TYPE_CODE)
                .addQueryParameter(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                .addQueryParameter(AuthConstants.PARAM_SCOPE, "tracking,sleeps")
                .build()

        println(uri.toString())
        activity.startActivityForResult(WebServiceLoginActivity.makeIntent(uri.toString(), activity), OTExternalService.requestCodeDict[MisfitService])
    }

    fun exchangeToToken(code: String, handler: (String?) -> Unit) {
        TokenExchangeTask(handler).execute(code)
    }

    fun getLatestSleepOnDayAsync(token: String, start: Date, end: Date, handler: (TimeSpan?) -> Unit) {
        APIRequestTask(
                mapOf(
                        QUERY_START_DATE to AuthConstants.DATE_TIME_FORMAT.format(start),
                        QUERY_END_DATE to AuthConstants.DATE_TIME_FORMAT.format(end)
                ),
                SUBURL_ACTIVITY_SLEEP)
        {
            resultObject ->
            if (resultObject?.has("sleeps") ?: false) {
                val sleeps = resultObject!!.getJSONArray("sleeps")
                if (sleeps.length() > 0) {
                    val last = sleeps.getJSONObject(sleeps.length() - 1)
                    val startTimeString = last.getString("startTime")
                    val duration = last.getInt("duration")

                    val startTime = AuthConstants.DATE_TIME_FORMAT.parse(startTimeString)
                    handler.invoke(TimeSpan.fromPoints(startTime.time, startTime.time + duration * 1000))

                } else handler.invoke(null)
            } else handler.invoke(null)
        }.execute(token)
    }

    fun getStepsOnDayAsync(token: String, start:Date, end: Date, handler: (Int?)->Unit)
    {
        APIRequestTask(
                mapOf(
                        QUERY_START_DATE to AuthConstants.DATE_TIME_FORMAT.format(start),
                        QUERY_END_DATE to AuthConstants.DATE_TIME_FORMAT.format(end)
                ),
                SUBURL_ACTIVITY_SUMMARY)
        {
            resultObject ->
                try{
                    val steps = resultObject!!.getInt("steps")
                    handler.invoke(steps)
                }
                catch(e: Exception) {
                    e.printStackTrace()
                    println("misfit step count failed")
                    handler.invoke(null)
                }
        }.execute(token)
    }

    private class APIRequestTask(val parameters: Map<String, String>, val activity: String, val handler: ((JSONObject?) -> Unit)? = null) : AsyncTask<String, Void?, String?>() {

        override fun doInBackground(vararg p0: String): String? {
            val token = p0[0]
            val uriBuilder = makeUriBuilderRoot().addPathSegments(SUBURL_ACTIVITY).addPathSegment(activity)

            for (paramEntry in parameters) {
                uriBuilder.addQueryParameter(paramEntry.key, paramEntry.value)
            }

            uriBuilder.addQueryParameter(AuthConstants.PARAM_ACCESS_TOKEN, token)

            println("query ${uriBuilder.build().toString()}")

            val request = Request.Builder()
                    .url(uriBuilder.build())
                    .get()
                    .build()

            if (NetworkHelper.isConnectedToInternet()) {
                val response = OkHttpClient().newCall(request).execute()
                val responseBody = response.body().string()
                println(responseBody)
                return responseBody
            } else return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                handler?.invoke(JSONObject(result))
            } else handler?.invoke(null)
        }

    }

    private class TokenExchangeTask(val handler: (String?) -> Unit) : AsyncTask<String, Void?, String?>() {
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            handler.invoke(result)
        }

        override fun doInBackground(vararg p0: String): String? {
            val uri = makeUriBuilderRoot().addPathSegments(SUBURL_EXCHANGE).build()
            val requestBody = FormBody.Builder()
                    .add(AuthConstants.PARAM_CODE, p0[0])
                    .add(AuthConstants.PARAM_CLIENT_ID, OTApplication.app.resources.getString(R.string.misfit_app_key))
                    .add(AuthConstants.PARAM_CLIENT_SECRET, OTApplication.app.resources.getString(R.string.misfit_app_secret))
                    .add(AuthConstants.PARAM_GRANT_TYPE, "authorization_code")
                    .add(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                    .build()

            val request = Request.Builder()
                    .url(uri)
                    .post(requestBody)
                    .build()

            val response = OkHttpClient().newCall(request).execute()
            val result = JSONObject(response.body().string())
            if (result.has(AuthConstants.PARAM_ACCESS_TOKEN)) {
                return result.getString(AuthConstants.PARAM_ACCESS_TOKEN)
            } else return null
        }

    }

}
