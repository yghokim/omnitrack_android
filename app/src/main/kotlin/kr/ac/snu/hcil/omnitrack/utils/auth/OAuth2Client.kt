package kr.ac.snu.hcil.omnitrack.utils.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.support.v4.app.FragmentActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import okhttp3.*
import org.json.JSONObject
import rx.Observable
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */
class OAuth2Client(val config: OAuth2Config, val activityRequestCode: Int) {

    class OAuth2Config() {
        var authorizationUrl: String = ""
        var tokenRequestUrl: String = ""
        var revokeUrl: String = ""
        var clientId: String = ""
        var clientSecret: String = ""
        var scope: String = ""
        var redirectUri: String = AuthConstants.VALUE_REDIRECT_URI
    }

    interface OAuth2ResultListener {

        fun onFailed(error: String?)

        fun onSuccess(credential: Credential)
    }

    interface OAuth2CredentialRefreshedListener {
        fun onCredentialRefreshed(newCredential: Credential)
    }

    interface OAuth2RequestConverter<T> {
        fun process(requestResultStrings: Array<String>): T
    }

    data class Credential(val accessToken: String, val refreshToken: String, val expiresIn: Int) {
        fun store(pref: SharedPreferences, prefix: String) {
            pref.edit()
                    .putString(prefix + '_' + AuthConstants.PARAM_ACCESS_TOKEN, accessToken)
                    .putString(prefix + '_' + AuthConstants.PARAM_REFRESH_TOKEN, refreshToken)
                    .putInt(prefix + '_' + AuthConstants.PARAM_EXPIRES_IN, expiresIn)
                    .apply()
        }

        fun remove(pref: SharedPreferences, prefix: String) {
            pref.edit()
                    .remove(prefix + '_' + AuthConstants.PARAM_ACCESS_TOKEN)
                    .remove(prefix + '_' + AuthConstants.PARAM_REFRESH_TOKEN)
                    .remove(prefix + '_' + AuthConstants.PARAM_EXPIRES_IN)
                    .apply()

        }

        companion object {
            fun restore(pref: SharedPreferences, prefix: String): Credential? {
                if (pref.contains(prefix + '_' + AuthConstants.PARAM_ACCESS_TOKEN)) {
                    return Credential(
                            pref.getString(prefix + '_' + AuthConstants.PARAM_ACCESS_TOKEN, ""),
                            pref.getString(prefix + '_' + AuthConstants.PARAM_REFRESH_TOKEN, ""),
                            pref.getInt(prefix + '_' + AuthConstants.PARAM_EXPIRES_IN, 0)
                    )
                } else return null
            }
        }
    }

    private var resultHandler: OAuth2ResultListener? = null

    fun authorize(activity: FragmentActivity, resultHandler: OAuth2ResultListener, serviceName: String? = null) {
        this.resultHandler = resultHandler
        val uri = HttpUrl.parse(config.authorizationUrl).newBuilder()
                .addQueryParameter(AuthConstants.PARAM_CLIENT_ID, config.clientId)
                .addQueryParameter(AuthConstants.PARAM_RESPONSE_TYPE, AuthConstants.VALUE_RESPONSE_TYPE_CODE)
                .addQueryParameter(AuthConstants.PARAM_REDIRECT_URI, config.redirectUri)
                .addQueryParameter(AuthConstants.PARAM_SCOPE, config.scope)
                .build()

        println(uri.toString())
        activity.startActivityForResult(WebServiceLoginActivity.makeIntent(uri.toString(), serviceName ?: "Service", activity), activityRequestCode)
    }

    fun signOut(credential: Credential, resultHandler: (Boolean) -> Unit) {
        RevokeTask(resultHandler).execute(credential)
    }

    fun handleLoginActivityResult(data: Intent) {
        val code = data.getStringExtra(AuthConstants.PARAM_CODE)
        TokenExchangeTask {
            credential ->
            if (credential != null) {
                resultHandler?.onSuccess(credential)
            } else {
                resultHandler?.onFailed("Token exchanged failed.")
            }
        }.execute(code)
    }

    private fun refreshToken(credential: Credential): Credential? {
        val uri = HttpUrl.parse(config.tokenRequestUrl)

        println("trying to refresh token with refresh token ${credential.refreshToken}")

        val requestBody = FormBody.Builder()
                .add(AuthConstants.PARAM_REFRESH_TOKEN, credential.refreshToken)
                .add(AuthConstants.PARAM_GRANT_TYPE, "refresh_token")
                .build()

        val request = makeRequestBuilderWithAuthHeader(uri)
                .post(requestBody)
                .build()


        try {
            val response = OkHttpClient().newCall(request).execute()
            val json = JSONObject(response.body().string())
            println(json)
            if (json.has(AuthConstants.PARAM_ACCESS_TOKEN)) {
                println("token refreshing was successful")
                return Credential(json.getString(AuthConstants.PARAM_ACCESS_TOKEN),
                        json.getString(AuthConstants.PARAM_REFRESH_TOKEN),
                        json.getInt(AuthConstants.PARAM_EXPIRES_IN))
            } else return null
        } catch(e: Exception) {
            e.printStackTrace()
            return null
        }
    }


    private fun makeRequestBuilderWithAuthHeader(url: HttpUrl): Request.Builder {
        return makeRequestBuilderWithAuthHeader(url.toString())
    }

    private fun makeRequestBuilderWithAuthHeader(url: String): Request.Builder {
        return Request.Builder()
                .url(url)
                .addHeader(AuthConstants.HEADER_AUTHORIZATION, AuthConstants.makeBasicHeader(config.clientId, config.clientSecret))
    }

    private fun makeRequestBuilderWithTokenHeader(url: String, credential: Credential): Request.Builder {
        return Request.Builder()
                .url(url)
                .addHeader(AuthConstants.HEADER_AUTHORIZATION, "Bearer ${credential.accessToken}")
    }

    fun <T> getRequest(credential: Credential, converter: OAuth2RequestConverter<T>, credentialRefreshedListener: OAuth2CredentialRefreshedListener?, vararg requestUrls: String): Observable<Result<T>> {
        return Observable.defer {
            val result = requestAwait(credential, converter, credentialRefreshedListener, *requestUrls)
            return@defer Observable.just(Result(result))
        }.subscribeOn(Schedulers.io())
    }

    private fun <T> requestAwait(credential: Credential, converter: OAuth2RequestConverter<T>, credentialRefreshedListener: OAuth2CredentialRefreshedListener?, vararg urls: String): T? {
        if (NetworkHelper.isConnectedToInternet()) {
            try {

                val result = ArrayList<String>()
                for (url in urls) {
                    println("fetching ${url}")
                    var response = requestAwait(credential, url)

                    println("fetching finished")
                    if (response.code() == 401) {

                        //token expired
                        println(response.body().string())
                        println("token expired. try refreshing..")

                        val newCredential = refreshToken(credential)

                        if (newCredential != null) {
                            println("token was refreshed successfully. Made new credential.")
                            credentialRefreshedListener?.onCredentialRefreshed(newCredential)
                            response = requestAwait(newCredential, url)
                        } else {
                            println("new credential is null. token refresh failed.")
                            continue
                        }
                    }
                    result.add(response.body().string())
                }
                return converter.process(result.toTypedArray())
            } catch(e: Exception) {
                e.printStackTrace()
                return null
            }
        } else return null
    }

    private fun requestAwait(credential: Credential, requestUrl: String): Response {
        val request = makeRequestBuilderWithTokenHeader(requestUrl, credential)
                .get()
                .build()
        return OkHttpClient().newCall(request).execute()
    }

    inner class RevokeTask(val handler: (Boolean) -> Unit) : AsyncTask<Credential, Void?, Boolean>() {
        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            handler.invoke(result)
        }

        override fun doInBackground(vararg credentials: Credential): Boolean {
            val requestBody = FormBody.Builder()
                    .add("token", credentials[0].accessToken)
                    .build()

            val request = makeRequestBuilderWithAuthHeader(HttpUrl.parse(config.revokeUrl))
                    .post(requestBody)
                    .build()

            if (NetworkHelper.isConnectedToInternet()) {
                try {
                    val response = OkHttpClient().newCall(request).execute()
                    return response.code() == 200
                } catch(e: Exception) {
                    e.printStackTrace()
                    return false
                }
            } else return false

        }


    }

    inner class TokenExchangeTask(val handler: (Credential?) -> Unit) : AsyncTask<String, Void?, Credential?>() {
        override fun onPostExecute(result: Credential?) {
            super.onPostExecute(result)
            handler.invoke(result)
        }

        override fun doInBackground(vararg p0: String): Credential? {
            val uri = HttpUrl.parse(config.tokenRequestUrl)

            val requestBody = FormBody.Builder()
                    .add(AuthConstants.PARAM_CODE, p0[0])
                    .add(AuthConstants.PARAM_CLIENT_ID, config.clientId)
                    .add(AuthConstants.PARAM_GRANT_TYPE, "authorization_code")
                    .add(AuthConstants.PARAM_REDIRECT_URI, config.redirectUri)
                    // .add(AuthConstants.PARAM_EXPIRES_IN, "2592000" ) //use long period when you sure that there is no error in the token process.
                    .build()

            val request = makeRequestBuilderWithAuthHeader(uri)
                    .post(requestBody)
                    .build()

            if (NetworkHelper.isConnectedToInternet()) {
                try {
                    val response = OkHttpClient().newCall(request).execute()
                    val json = JSONObject(response.body().string())
                    println(json)
                    if (json.has(AuthConstants.PARAM_ACCESS_TOKEN)) {
                        println("successfully exchanged code to credential.")
                        val accessToken = json.getString(AuthConstants.PARAM_ACCESS_TOKEN)
                        val refreshToken = if (json.has(AuthConstants.PARAM_REFRESH_TOKEN)) {
                            json.getString(AuthConstants.PARAM_REFRESH_TOKEN)
                        } else ""
                        val expiresIn = if (json.has(AuthConstants.PARAM_EXPIRES_IN)) {
                            json.getInt(AuthConstants.PARAM_EXPIRES_IN)
                        } else Int.MAX_VALUE
                        return Credential(accessToken, refreshToken, expiresIn)
                    } else return null
                } catch(e: Exception) {
                    return null
                }
            } else return null

        }

    }
}