package kr.ac.snu.hcil.omnitrack.utils.auth

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkNotConnectedException
import okhttp3.*
import org.json.JSONObject
import rx_activity_result2.RxActivityResult
import java.util.*

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */
class OAuth2Client(val context: Context, val config: OAuth2Config) {

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

    fun authorize(activity: Activity, serviceName: String? = null): Single<Credential> {
        val uri = HttpUrl.parse(config.authorizationUrl)!!.newBuilder()
                .addQueryParameter(AuthConstants.PARAM_CLIENT_ID, config.clientId)
                .addQueryParameter(AuthConstants.PARAM_RESPONSE_TYPE, AuthConstants.VALUE_RESPONSE_TYPE_CODE)
                .addQueryParameter(AuthConstants.PARAM_REDIRECT_URI, config.redirectUri)
                .addQueryParameter(AuthConstants.PARAM_SCOPE, config.scope)
                .build()

        return RxActivityResult.on(activity)
                .startIntent(WebServiceLoginActivity.makeIntent(uri.toString(), serviceName
                        ?: "Service", null, activity))
                .firstOrError().flatMap { result ->
                    println("RxActivityResult : activity result")
                    val data = result.data()
                    val resultCode = result.resultCode()
                    if (resultCode == Activity.RESULT_OK) {
                        val code = data.getStringExtra(AuthConstants.PARAM_CODE)
                        return@flatMap exchangeToken(code)
                    } else {
                        return@flatMap Single.error<Credential>(Exception("Authentication process was canceled by user."))
                    }
                }


        //activity.startActivityForResult(WebServiceLoginActivity.makeIntent(uri.toString(), serviceName ?: "Service", activity), activityRequestCode)
    }

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
        } catch (e: Exception) {
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

    fun <T> getRequest(credential: Credential, converter: OAuth2RequestConverter<T>, credentialRefreshedListener: OAuth2CredentialRefreshedListener?, vararg requestUrls: String): Single<Nullable<T>> {
        return ReactiveNetwork.checkInternetConnectivity().flatMapCompletable { connected ->
            if (connected) Completable.complete() else Completable.error(NetworkNotConnectedException())
        }.andThen(Single.defer {
            val result = requestAwait(credential, converter, credentialRefreshedListener, *requestUrls)
            return@defer Single.just(Nullable(result))
        }.subscribeOn(Schedulers.io()))
    }


    fun signOut(credential: Credential): Completable {
        return ReactiveNetwork.checkInternetConnectivity().flatMapCompletable { connected ->
            if (connected) {
                Completable.defer {
                    val requestBody = FormBody.Builder()
                            .add("token", credential.accessToken)
                            .build()

                    val request = makeRequestBuilderWithAuthHeader(HttpUrl.parse(config.revokeUrl)!!)
                            .post(requestBody)
                            .build()
                    try {
                        val response = OkHttpClient().newCall(request).execute()
                        if (response.code() == 200) {
                            return@defer Completable.complete()
                        } else {
                            println("result code is not 200. sign out failed.")
                            println(response.message())
                            return@defer Completable.error(IllegalStateException(response.message()))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@defer Completable.error(e)
                    }
                }.subscribeOn(Schedulers.io())
            } else {
                Completable.error(NetworkNotConnectedException())
            }
        }
    }

    private fun <T> requestAwait(credential: Credential, converter: OAuth2RequestConverter<T>, credentialRefreshedListener: OAuth2CredentialRefreshedListener?, vararg urls: String): T? {
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
    }

    private fun requestAwait(credential: Credential, requestUrl: String): Response {
        val request = makeRequestBuilderWithTokenHeader(requestUrl, credential)
                .get()
                .build()
        return OkHttpClient().newCall(request).execute()
    }

    private fun exchangeToken(requestCode: String): Single<Credential> {
        return ReactiveNetwork.checkInternetConnectivity().flatMapCompletable { connected ->
            if (connected) {
                Completable.complete()
            } else Completable.error(NetworkNotConnectedException())
        }.andThen(Single.defer {
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
                    return@defer Single.just(Credential(accessToken, refreshToken, expiresIn))
                } else Single.error<Credential>(IllegalArgumentException("Access token was not received."))
            } catch (e: Exception) {
                Single.error<Credential>(e)
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }
}