package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.Context
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.auth.AuthConstants
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import rx_activity_result2.RxActivityResult
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */

class MisfitApi(val context: Context) {

    companion object {
        const val URL_ROOT = "api.misfitwearables.com"
        const val SUBURL_LOGIN = "auth/dialog/authorize"
        const val SUBURL_EXCHANGE = "auth/tokens/exchange"
        const val SUBURL_ACTIVITY = "move/resource/v1/user/me/activity"
        const val SUBURL_ACTIVITY_SUMMARY = "summary"
        const val SUBURL_ACTIVITY_SLEEP = "sleeps"

        const val QUERY_START_DATE = "start_date"
        const val QUERY_END_DATE = "end_date"

    }

    fun makeUriBuilderRoot(): HttpUrl.Builder {
        return HttpUrl.Builder().scheme("https").host(URL_ROOT)
    }

    /***
     * authrize and observe access token
     */
    fun authorize(activity: Activity): Single<String> {

        val uri = makeUriBuilderRoot().addPathSegments(SUBURL_LOGIN)
                .addQueryParameter(AuthConstants.PARAM_CLIENT_ID, BuildConfig.MISFIT_APP_KEY)
                .addQueryParameter(AuthConstants.PARAM_RESPONSE_TYPE, AuthConstants.VALUE_RESPONSE_TYPE_CODE)
                .addQueryParameter(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                .addQueryParameter(AuthConstants.PARAM_SCOPE, "tracking,sleeps")
                .build()
        return RxActivityResult.on(activity).startIntent(WebServiceLoginActivity.makeIntent(uri.toString(), activity.resources.getString(R.string.service_misfit_name), null, activity))
                .firstOrError()
                .flatMap { result ->
                    if (result.resultCode() == Activity.RESULT_OK) {
                        val code = result.data().getStringExtra(AuthConstants.PARAM_CODE)
                        if (code != null) {
                            getTokenExchangeRequest(code).subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                        } else {
                            Single.error<String>(Exception("access token was not received."))
                        }
                    } else {
                        Single.error<String>(Exception("login activity was canceled."))
                    }
                }
    }

    fun getLatestSleepOnDayRequest(token: String, start: Date, end: Date): Single<Nullable<TimeSpan>> {
        return getApiRequest(token, mapOf(
                QUERY_START_DATE to AuthConstants.DATE_TIME_FORMAT.format(start),
                QUERY_END_DATE to AuthConstants.DATE_TIME_FORMAT.format(end)
        ), SUBURL_ACTIVITY_SLEEP).map { resultObject ->
            if (resultObject.has("sleeps")) {
                val sleeps = resultObject.getJSONArray("sleeps")
                if (sleeps.length() > 0) {
                    val last = sleeps.getJSONObject(sleeps.length() - 1)
                    val startTimeString = last.getString("startTime")
                    val duration = last.getInt("duration")
                    val startTime = AuthConstants.DATE_TIME_FORMAT.parse(startTimeString)
                    Nullable((TimeSpan.fromPoints(startTime.time, startTime.time + duration * 1000)))
                } else Nullable<TimeSpan>(null)
            } else throw Exception("Result JSON doe not have 'sleeps' element.")
        }
    }

    fun getStepsOnDayRequest(token: String, start: Date, end: Date): Single<Nullable<Int>> {
        return getApiRequest(token, mapOf(
                QUERY_START_DATE to AuthConstants.DATE_TIME_FORMAT.format(start),
                QUERY_END_DATE to AuthConstants.DATE_TIME_FORMAT.format(end)
        ), SUBURL_ACTIVITY_SUMMARY).map { resultObject ->
            if (resultObject.has("steps")) {
                Nullable(resultObject.getInt("steps"))
            } else {
                println("misfit step count failed")
                throw Exception("Result JSON doe not have 'steps' element.")
            }
        }
    }

    fun getApiRequest(token: String, parameters: Map<String, String>, activity: String): Single<JSONObject> {

        return Single.defer {
            val uriBuilder = makeUriBuilderRoot().addPathSegments(SUBURL_ACTIVITY).addPathSegment(activity)

            for (paramEntry in parameters) {
                uriBuilder.addQueryParameter(paramEntry.key, paramEntry.value)
            }

            uriBuilder.addQueryParameter(AuthConstants.PARAM_ACCESS_TOKEN, token)

            println("query ${uriBuilder.build()}")

            val request = Request.Builder()
                    .url(uriBuilder.build())
                    .get()
                    .build()

            return@defer ReactiveNetwork.checkInternetConnectivity().flatMap { connected ->
                if (connected) {
                    try {
                        val response = OkHttpClient().newCall(request).execute()
                        val responseBody = response.body()!!.string()
                        println(responseBody)
                        return@flatMap Single.just(JSONObject(responseBody))
                    } catch (e: Exception) {
                        OTApp.logger.writeSystemLog("Misfit API Exception: ${e.message}", "MisfitAPI")
                        return@flatMap Single.error<JSONObject>(e)
                    }
                } else {
                    return@flatMap Single.error<JSONObject>(NetworkErrorException("Network is not on."))
                }
            }
        }
    }

    fun getTokenExchangeRequest(requestCode: String): Single<String> {
        return Single.defer {
            val uri = makeUriBuilderRoot().addPathSegments(SUBURL_EXCHANGE).build()
            val requestBody = FormBody.Builder()
                    .add(AuthConstants.PARAM_CODE, requestCode)
                    .add(AuthConstants.PARAM_CLIENT_ID, BuildConfig.MISFIT_APP_KEY)
                    .add(AuthConstants.PARAM_CLIENT_SECRET, BuildConfig.MISFIT_APP_SECRET)
                    .add(AuthConstants.PARAM_GRANT_TYPE, "authorization_code")
                    .add(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                    .build()

            val request = Request.Builder()
                    .url(uri)
                    .post(requestBody)
                    .build()

            try {
                val response = OkHttpClient().newCall(request).execute()
                val result = JSONObject(response.body()!!.string())
                if (result.has(AuthConstants.PARAM_ACCESS_TOKEN)) {
                    return@defer Single.just(result.getString(AuthConstants.PARAM_ACCESS_TOKEN))
                } else return@defer Single.error<String>(Exception("token empty"))
            } catch (e: Exception) {
                OTApp.logger.writeSystemLog("Misfit Token Exchange Exception: ${e.message}", "MisfitAPI")
                return@defer Single.error<String>(e)
            }
        }
    }
}
