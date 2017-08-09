package kr.ac.snu.hcil.omnitrack.utils.auth

import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.SharedPreferences
import android.os.AsyncTask
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import kr.ac.snu.hcil.omnitrack.utils.Result
import kr.ac.snu.hcil.omnitrack.utils.convertToRx1Observable
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import okhttp3.*
import org.json.JSONObject
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx_activity_result2.RxActivityResult
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */
class OAuth2Client(val config: OAuth2Config) {

    class OAuth2Config {
        var authorizationUrl: String = ""
        var tokenRequestUrl: String = ""
        var revokeUrl: String = ""
        var clientId: String = ""
        var clientSecret: String = ""
        var scope: String = ""
        var redirectUri: String = AuthConstants.VALUE_REDIRECT_URI
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

    fun authorize(activity: Activity, serviceName: String? = null): Observable<Credential> {
        val uri = HttpUrl.parse(config.authorizationUrl)!!.newBuilder()
                .addQueryParameter(AuthConstants.PARAM_CLIENT_ID, config.clientId)
                .addQueryParameter(AuthConstants.PARAM_RESPONSE_TYPE, AuthConstants.VALUE_RESPONSE_TYPE_CODE)
                .addQueryParameter(AuthConstants.PARAM_REDIRECT_URI, config.redirectUri)
                .addQueryParameter(AuthConstants.PARAM_SCOPE, config.scope)
                .build()

        return RxActivityResult.on(activity)
                .startIntent(WebServiceLoginActivity.makeIntent(uri.toString(), serviceName ?: "Service", activity))
                .convertToRx1Observable()
                .flatMap {
                    result ->
                    println("RxActivityResult : activity result")
                    val data = result.data()
                    val resultCode = result.resultCode()
                    if (resultCode == Activity.RESULT_OK) {
                        val code = data.getStringExtra(AuthConstants.PARAM_CODE)
                        return@flatMap exchangeToken(code)
                    } else {
                        return@flatMap Observable.error<Credential>(Exception("Authentication process was canceled by user."))
                    }
                }


        //activity.startActivityForResult(WebServiceLoginActivity.makeIntent(uri.toString(), serviceName ?: "Service", activity), activityRequestCode)
    }

    fun signOut(credential: Credential, resultHandler: (Boolean) -> Unit) {
        RevokeTask(resultHandler).execute(credential)
    }

    /*
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
    }*/

    private fun refreshToken(credential: Credential): Credential? {
        val uri = HttpUrl.parse(config.tokenRequestUrl)!!

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
            val json = JSONObject(response.body()?.string() ?: "{\"error\": \"null response\"}")
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
                    result.add(response.body()!!.string())
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

            val request = makeRequestBuilderWithAuthHeader(HttpUrl.parse(config.revokeUrl)!!)
                    .post(requestBody)
                    .build()

            if (NetworkHelper.isConnectedToInternet()) {
                try {
                    val response = OkHttpClient().newCall(request).execute()
                    if (response.code() == 200) {
                        return true
                    } else {
                        println("result code is not 200. sign out failed.")
                        println(response.message())
                        return false
                    }
                } catch(e: Exception) {
                    e.printStackTrace()
                    return false
                }
            } else {
                println("internet is not connected. Sign out was failed.")
                return false
            }

        }


    }

    private fun exchangeToken(requestCode: String): rx.Observable<Credential> {
        return Observable.defer {
            val uri = HttpUrl.parse(config.tokenRequestUrl)!!

            val requestBody = FormBody.Builder()
                    .add(AuthConstants.PARAM_CODE, requestCode)
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
                    val json = JSONObject(response.body()!!.string())
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
                        return@defer Observable.just(Credential(accessToken, refreshToken, expiresIn))
                    } else Observable.error<Credential>(IllegalArgumentException("Access token was not received."))
                } catch(e: Exception) {
                    Observable.error<Credential>(e)
                }
            } else Observable.error<Credential>(NetworkErrorException("No internet connection"))
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    inner class TokenExchangeTask(val handler: (Credential?) -> Unit) : AsyncTask<String, Void?, Credential?>() {
        override fun onPostExecute(result: Credential?) {
            super.onPostExecute(result)
            handler.invoke(result)
        }

        override fun doInBackground(vararg p0: String): Credential? {
            val uri = HttpUrl.parse(config.tokenRequestUrl)!!

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
                    val json = JSONObject(response.body()!!.string())
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