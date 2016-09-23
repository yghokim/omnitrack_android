package kr.ac.snu.hcil.omnitrack.utils.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.support.v4.app.FragmentActivity
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import okhttp3.*
import org.json.JSONObject

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
    }

    interface OAuth2ResultListener{

        fun onFailed(error: String?)

        fun onSuccess(credential: Credential)
    }

    interface OAuth2CredentialRefreshedListener {
        fun onCredentialRefreshed(newCredential: Credential)
    }

    interface OAuth2RequestConverter<T> {
        fun process(requestResultString: String): T
    }

    data class Credential(val accessToken: String, val refreshToken: String, val expiresIn: Int){
        fun store(pref: SharedPreferences, prefix: String)
        {
            pref.edit()
                    .putString(prefix+'_'+AuthConstants.PARAM_ACCESS_TOKEN, accessToken)
                    .putString(prefix+'_'+AuthConstants.PARAM_REFRESH_TOKEN, refreshToken)
                    .putInt(prefix+'_'+AuthConstants.PARAM_EXPIRES_IN, expiresIn)
                .apply()
        }

        fun remove(pref: SharedPreferences, prefix: String) {
            pref.edit()
                    .remove(prefix + '_' + AuthConstants.PARAM_ACCESS_TOKEN)
                    .remove(prefix + '_' + AuthConstants.PARAM_REFRESH_TOKEN)
                    .remove(prefix + '_' + AuthConstants.PARAM_EXPIRES_IN)
                    .apply()

        }

        companion object{
            fun restore(pref: SharedPreferences, prefix: String): Credential?{
                if(pref.contains(prefix+'_'+AuthConstants.PARAM_ACCESS_TOKEN))
                {
                    return Credential(
                            pref.getString(prefix+'_'+AuthConstants.PARAM_ACCESS_TOKEN,""),
                            pref.getString(prefix+'_'+AuthConstants.PARAM_REFRESH_TOKEN,""),
                            pref.getInt(prefix+'_'+AuthConstants.PARAM_EXPIRES_IN, 0)
                            )
                }
                else return null
            }
        }
    }

    private var resultHandler: OAuth2ResultListener? = null

    fun authorize(activity: FragmentActivity, resultHandler: OAuth2ResultListener) {
        this.resultHandler = resultHandler
        val uri = HttpUrl.parse(config.authorizationUrl).newBuilder()
                .addQueryParameter(AuthConstants.PARAM_CLIENT_ID, config.clientId)
                .addQueryParameter(AuthConstants.PARAM_RESPONSE_TYPE, AuthConstants.VALUE_RESPONSE_TYPE_CODE)
                .addQueryParameter(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                .addQueryParameter(AuthConstants.PARAM_SCOPE, config.scope)
                .build()

        println(uri.toString())
        activity.startActivityForResult(WebServiceLoginActivity.makeIntent(uri.toString(), activity), activityRequestCode)
    }

    fun signOut(credential: Credential, resultHandler: (Boolean) -> Unit) {
        RevokeTask(resultHandler).execute(credential)
    }

    fun handleLoginActivityResult(data: Intent)
    {
        val code = data.getStringExtra(AuthConstants.PARAM_CODE)
        TokenExchangeTask{
            credential->
                if(credential!=null)
                {
                    resultHandler?.onSuccess(credential)
                }
                else{
                    resultHandler?.onFailed("Token exchanged failed.")
                }
        }.execute(code)
    }

    private fun refreshToken(credential: Credential): Credential?{
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
            if(json.has(AuthConstants.PARAM_ACCESS_TOKEN)) {
                println("token refreshing was successful")
                return Credential(json.getString(AuthConstants.PARAM_ACCESS_TOKEN),
                        json.getString(AuthConstants.PARAM_REFRESH_TOKEN),
                        json.getInt(AuthConstants.PARAM_EXPIRES_IN))
            }
            else return null
        }catch(e: Exception)
        {
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

    fun <T> request(credential: Credential, requestUrl: String, converter: OAuth2RequestConverter<T>, credentialRefreshedListener: OAuth2CredentialRefreshedListener?, handler: ((T?) -> Unit)?) {
        RequestTask<T>(requestUrl, converter, credentialRefreshedListener, handler).execute(credential)
    }


    inner class RequestTask<T>(val requestUrl: String, val converter: OAuth2RequestConverter<T>, val credentialRefreshedListener: OAuth2CredentialRefreshedListener?, val handler: ((T?) -> Unit)?) : AsyncTask<Credential, Void?, T?>() {
        override fun onPostExecute(result: T?) {
            super.onPostExecute(result)
            handler?.invoke(result)
        }

        override fun doInBackground(vararg credentials: Credential): T? {
            try {
                var response = requestAwait(credentials[0])
                if (response.code() == 401) {

                    //token expired
                    println(response.body().string())
                    println("token expired. try refreshing..")

                    val newCredential = refreshToken(credentials[0])

                    if (newCredential != null) {
                        println("token was refreshed successfully. Made new credential.")
                        credentialRefreshedListener?.onCredentialRefreshed(newCredential)
                        response = requestAwait(newCredential)
                    } else{
                        println("new credential is null. token refresh failed.")
                        return null
                    }
                }

                val resultString = response.body().string()
                println(response.headers().names())

                Thread.sleep(4000)

                return converter.process(resultString)
            } catch(e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        private fun requestAwait(credential: Credential): Response {
            val request = makeRequestBuilderWithTokenHeader(requestUrl, credential)
                    .get()
                    .build()
            return OkHttpClient().newCall(request).execute()
        }
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

            try {
                val response = OkHttpClient().newCall(request).execute()
                return response.code() == 200
            } catch(e: Exception) {
                e.printStackTrace()
                return false
            }

        }


    }

    inner class TokenRefreshTask(val handler: ((Credential?) -> Unit)?) : AsyncTask<Credential, Void?, Credential?>() {

        override fun onPostExecute(result: Credential?) {
            super.onPostExecute(result)
            handler?.invoke(result)
        }

        override fun doInBackground(vararg credential: Credential): Credential? {
            return refreshToken(credential[0])
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
                    .add(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                   // .add(AuthConstants.PARAM_EXPIRES_IN, "2592000" ) //TODO use long period when you sure that there is no error in the token process.
                    .build()

            val request = makeRequestBuilderWithAuthHeader(uri)
                    .post(requestBody)
                    .build()

            try {
                val response = OkHttpClient().newCall(request).execute()
                val json = JSONObject(response.body().string())
                println(json)
                if(json.has(AuthConstants.PARAM_ACCESS_TOKEN)) {
                    println("successfully exchanged code to credential.")
                    return Credential(json.getString(AuthConstants.PARAM_ACCESS_TOKEN),
                            json.getString(AuthConstants.PARAM_REFRESH_TOKEN),
                            json.getInt(AuthConstants.PARAM_EXPIRES_IN))
                }
                else return null
            }catch(e: Exception)
            {
                return null
            }

        }

    }


/*
    class OAuth2LoginActivity: WebServiceLoginActivity(){
        override fun finishIfPossible(redirectedUrl: String) {
            val parsedUrl = HttpUrl.parse(redirectedUrl)
            val accessToken = parsedUrl.queryParameter(AuthConstants.PARAM_ACCESS_TOKEN)
            val expiresIn = parsedUrl.queryParameter(AuthConstants.PARAM_EXPIRES_IN)

            if (!accessToken.isNullOrBlank()) {
                val result = Intent()
                result.putExtra(AuthConstants.PARAM_ACCESS_TOKEN, accessToken)
                result.putExtra(AuthConstants.PARAM_EXPIRES_IN, expiresIn)
                setResult(Activity.RESULT_OK, result)
                finish()
            }

            //TODO error handling
        }

    }
*/
}