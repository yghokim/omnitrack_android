package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import android.accounts.NetworkErrorException
import android.support.v4.app.FragmentActivity
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import rx.Observable
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
        activity.startActivityForResult(WebServiceLoginActivity.makeIntent(uri.toString(), OTApplication.getString(R.string.service_misfit_name), activity), OTExternalService.requestCodeDict[MisfitService])
    }

    fun getLatestSleepOnDayRequest(token: String, start: Date, end: Date): Observable<Result<TimeSpan>> {
        return getApiRequest(token, mapOf(
                QUERY_START_DATE to AuthConstants.DATE_TIME_FORMAT.format(start),
                QUERY_END_DATE to AuthConstants.DATE_TIME_FORMAT.format(end)
        ), SUBURL_ACTIVITY_SLEEP).map {
            resultObject ->
            if (resultObject.has("sleeps")) {
                val sleeps = resultObject!!.getJSONArray("sleeps")
                if (sleeps.length() > 0) {
                    val last = sleeps.getJSONObject(sleeps.length() - 1)
                    val startTimeString = last.getString("startTime")
                    val duration = last.getInt("duration")
                    val startTime = AuthConstants.DATE_TIME_FORMAT.parse(startTimeString)
                    Result((TimeSpan.fromPoints(startTime.time, startTime.time + duration * 1000)))
                } else Result<TimeSpan>(null)
            } else throw Exception("Result JSON doe not have 'sleeps' element.")
        }
    }

    fun getStepsOnDayRequest(token: String, start: Date, end: Date): Observable<Result<Int>> {
        return getApiRequest(token, mapOf(
                QUERY_START_DATE to AuthConstants.DATE_TIME_FORMAT.format(start),
                QUERY_END_DATE to AuthConstants.DATE_TIME_FORMAT.format(end)
        ), SUBURL_ACTIVITY_SUMMARY).map {
            resultObject ->
            if (resultObject.has("steps")) {
                Result(resultObject.getInt("steps"))
            } else {
                println("misfit step count failed")
                throw Exception("Result JSON doe not have 'steps' element.")
            }
        }
    }

    fun getApiRequest(token: String, parameters: Map<String, String>, activity: String): Observable<JSONObject> {

        return Observable.defer {
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
                try {
                    val response = OkHttpClient().newCall(request).execute()
                    val responseBody = response.body().string()
                    println(responseBody)
                    return@defer Observable.just(JSONObject(responseBody))
                } catch(e: Exception) {
                    OTApplication.logger.writeSystemLog("Misfit API Exception: ${e.message}", "MisfitAPI")
                    return@defer Observable.error<JSONObject>(e)
                }
            } else return@defer Observable.error<JSONObject>(NetworkErrorException("Network is not on."))
        }
    }

    fun getTokenExchangeRequest(requestCode: String): Observable<String> {
        return Observable.defer {
            val uri = makeUriBuilderRoot().addPathSegments(SUBURL_EXCHANGE).build()
            val requestBody = FormBody.Builder()
                    .add(AuthConstants.PARAM_CODE, requestCode)
                    .add(AuthConstants.PARAM_CLIENT_ID, OTApplication.app.resources.getString(R.string.misfit_app_key))
                    .add(AuthConstants.PARAM_CLIENT_SECRET, OTApplication.app.resources.getString(R.string.misfit_app_secret))
                    .add(AuthConstants.PARAM_GRANT_TYPE, "authorization_code")
                    .add(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                    .build()

            val request = Request.Builder()
                    .url(uri)
                    .post(requestBody)
                    .build()

            try {
                val response = OkHttpClient().newCall(request).execute()
                val result = JSONObject(response.body().string())
                if (result.has(AuthConstants.PARAM_ACCESS_TOKEN)) {
                    return@defer Observable.just(result.getString(AuthConstants.PARAM_ACCESS_TOKEN))
                } else return@defer Observable.error<String>(Exception("token empty"))
            } catch(e: Exception) {
                OTApplication.logger.writeSystemLog("Misfit Token Exchange Exception: ${e.message}", "MisfitAPI")
                return@defer Observable.error<String>(e)
            }
        }
    }
}
