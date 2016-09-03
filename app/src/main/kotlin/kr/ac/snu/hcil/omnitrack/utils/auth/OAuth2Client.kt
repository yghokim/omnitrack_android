package kr.ac.snu.hcil.omnitrack.utils.auth

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.support.v4.app.FragmentActivity
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.misfit.MisfitApi
import kr.ac.snu.hcil.omnitrack.ui.components.common.activity.WebServiceLoginActivity
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.framed.Header
import org.json.JSONObject

/**
 * Created by Young-Ho Kim on 2016-09-01.
 */
class OAuth2Client(val authorizationUrl: String, val tokenRequestUrl: String, val clientId: String, val clientSecret: String, val activityRequestCode: Int) {

    interface OAuth2ResultListener{

        fun onFailed(error: String?){

        }

        fun onSuccess(credential: Credential){

        }
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

    fun authorize(activity: FragmentActivity, scopes: Array<String>, scopeSeparator: String=" ", resultHandler: OAuth2ResultListener) {
        this.resultHandler = resultHandler
        val uri = HttpUrl.parse(authorizationUrl).newBuilder()
                .addQueryParameter(AuthConstants.PARAM_CLIENT_ID, clientId)
                .addQueryParameter(AuthConstants.PARAM_RESPONSE_TYPE, AuthConstants.VALUE_RESPONSE_TYPE_CODE)
                .addQueryParameter(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                .addQueryParameter(AuthConstants.PARAM_SCOPE, scopes.joinToString(scopeSeparator))
                .build()

        println(uri.toString())
        activity.startActivityForResult(WebServiceLoginActivity.makeIntent(uri.toString(), activity), activityRequestCode)
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


    inner class TokenExchangeTask(val handler: (Credential?) -> Unit) : AsyncTask<String, Void?, Credential?>() {
        override fun onPostExecute(result: Credential?) {
            super.onPostExecute(result)
            handler.invoke(null)
        }

        override fun doInBackground(vararg p0: String): Credential? {
            val uri = MisfitApi.makeUriBuilderRoot().addPathSegments(MisfitApi.SUBURL_EXCHANGE).build()

            val requestBody = FormBody.Builder()
                    .add(AuthConstants.PARAM_CODE, p0[0])
                    .add(AuthConstants.PARAM_CLIENT_ID, clientId)
                    .add(AuthConstants.PARAM_GRANT_TYPE, "authorization_code")
                    .add(AuthConstants.PARAM_REDIRECT_URI, AuthConstants.VALUE_REDIRECT_URI)
                    .build()

            val request = Request.Builder()
                    .url(uri)
                    .addHeader(AuthConstants.HEADER_AUTHORIZATION, AuthConstants.makeBasicHeader(clientId, clientSecret))
                    .post(requestBody)
                    .build()
            try {
                val response = OkHttpClient().newCall(request).execute()
                val json = JSONObject(response.body().string())
                if(json.has(AuthConstants.PARAM_ACCESS_TOKEN)) {
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