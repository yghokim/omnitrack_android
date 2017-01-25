package kr.ac.snu.hcil.omnitrack.core.externals.jawbone

import android.content.Context
import android.content.Intent
import android.support.v4.app.FragmentActivity
import com.jawbone.upplatformsdk.api.ApiManager
import com.jawbone.upplatformsdk.api.response.OauthAccessTokenResponse
import com.jawbone.upplatformsdk.oauth.OauthUtils
import com.jawbone.upplatformsdk.oauth.OauthWebViewActivity
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants.UP_PLATFORM_REFRESH_TOKEN
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response


/**
 * Created by younghokim on 2017. 1. 24..
 */
object JawboneUpService : OTExternalService("JawboneUpService", 9), Callback<OauthAccessTokenResponse> {

    private const val CLIENT_ID = "XLamBoVDTQY"
    private const val CLIENT_SECRET = "af60fc1d4d06c4de0286c2403702dc5a076c7132"
    private const val REDIRECT_URI = "up-platform://redirect"

    private val SCOPES: List<UpPlatformSdkConstants.UpPlatformAuthScope> by lazy {
        arrayListOf(
                UpPlatformSdkConstants.UpPlatformAuthScope.MOVE_READ,
                UpPlatformSdkConstants.UpPlatformAuthScope.WEIGHT_READ,
                UpPlatformSdkConstants.UpPlatformAuthScope.SLEEP_READ
        )
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_up
    override val descResourceId: Int = R.string.service_jawbone_desc
    override val nameResourceId: Int = R.string.service_jawbone_name

    internal var accessToken: String? = null


    override fun handleActivityActivationResultOk(resultData: Intent?) {

        val code = resultData?.getStringExtra(UpPlatformSdkConstants.ACCESS_CODE)
        if (code != null) {
            //first clear older accessToken, if it exists..
            ApiManager.getRequestInterceptor().clearAccessToken()

            ApiManager.getRestApiInterface().getAccessToken(
                    CLIENT_ID,
                    CLIENT_SECRET,
                    code,
                    this)
        }
    }

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
        val builder = OauthUtils.setOauthParameters(CLIENT_ID, REDIRECT_URI, SCOPES)

        val intent = Intent(context, OauthWebViewActivity::class.java)
        intent.putExtra(UpPlatformSdkConstants.AUTH_URI, builder.build())
        (context as FragmentActivity).startActivityForResult(intent, requestCodeDict[this])
    }

    override fun onDeactivate() {
        ApiManager.getRequestInterceptor().clearAccessToken()
        accessToken = null
        preferences.edit()
                .remove(UP_PLATFORM_ACCESS_TOKEN)
                .remove(UP_PLATFORM_REFRESH_TOKEN)
                .apply()
    }

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
        val token = preferences.getString(UP_PLATFORM_ACCESS_TOKEN, null)
        if (token != null) {
            ApiManager.getRequestInterceptor().setAccessToken(token)
            accessToken = token
            preparedHandler?.invoke(true)
        } else {
            preparedHandler?.invoke(false)
        }
    }

    override fun failure(error: RetrofitError) {
        println("Jawbone Request failed. ${error.message}")
        finishActivationProcess(false)
    }

    override fun success(result: OauthAccessTokenResponse, response: Response) {

        if (result.access_token != null) {
            val editor = preferences.edit()
            editor.putString(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN, result.access_token)
            editor.putString(UpPlatformSdkConstants.UP_PLATFORM_REFRESH_TOKEN, result.refresh_token)
            editor.apply()

            println("accessToken:" + result.access_token)
            accessToken = result.access_token
            finishActivationProcess(true)
        } else {
            println("accessToken not returned by Oauth call, exiting...")
            finishActivationProcess(false)
        }
    }

}