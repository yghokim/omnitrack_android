package kr.ac.snu.hcil.omnitrack.core.externals.jawbone

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.jawbone.upplatformsdk.api.ApiManager
import com.jawbone.upplatformsdk.api.response.OauthAccessTokenResponse
import com.jawbone.upplatformsdk.oauth.OauthUtils
import com.jawbone.upplatformsdk.oauth.OauthWebViewActivity
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN
import com.jawbone.upplatformsdk.utils.UpPlatformSdkConstants.UP_PLATFORM_REFRESH_TOKEN
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.ThirdPartyAppDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import kr.ac.snu.hcil.omnitrack.utils.getDayOfMonth
import kr.ac.snu.hcil.omnitrack.utils.getYear
import kr.ac.snu.hcil.omnitrack.utils.getZeroBasedMonth
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import rx_activity_result2.RxActivityResult
import java.util.*


/**
 * Created by younghokim on 2017. 1. 24..
 */
object JawboneUpService : OTExternalService("JawboneUpService", 9) {

    private const val CLIENT_ID = BuildConfig.JAWBONE_CLIENT_ID
    private const val CLIENT_SECRET = BuildConfig.JAWBONE_CLIENT_SECRET
    private const val REDIRECT_URI = BuildConfig.JAWBONE_REDIRECT_URI

    fun makeFormattedDateInteger(date: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return cal.getYear() * 10000 + (cal.getZeroBasedMonth() + 1) * 100 + cal.getDayOfMonth()
    }

    fun makeIntraDayRequestQueryParams(start: Date, end: Date, data: HashMap<String, Long>?): HashMap<String, Long> {
        val result = data ?: HashMap<String, Long>()

        result.put("date", makeFormattedDateInteger(start.time).toLong())
        result.put("start_date", start.time)
        result.put("end_date", end.time)

        return result
    }

    private val SCOPES: List<UpPlatformSdkConstants.UpPlatformAuthScope> by lazy {
        arrayListOf(
                UpPlatformSdkConstants.UpPlatformAuthScope.MOVE_READ,
                UpPlatformSdkConstants.UpPlatformAuthScope.WEIGHT_READ,
                UpPlatformSdkConstants.UpPlatformAuthScope.SLEEP_READ
        )
    }


    override fun onRegisterMeasureFactories(): Array<OTMeasureFactory> {
        return arrayOf(JawboneStepMeasureFactory, JawboneDistanceMeasureFactory)
    }

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return super.onRegisterDependencies() + arrayOf(
                JawboneAuthDependencyResolver(),
                ThirdPartyAppDependencyResolver.Builder(OTApp.instance)
                        .setAppName("UP®")
                        .setPackageName("com.jawbone.upopen")
                        .isMandatory(false)
                        .build()
        )
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_up
    override val descResourceId: Int = R.string.service_jawbone_desc
    override val nameResourceId: Int = R.string.service_jawbone_name

    internal var accessToken: String? = null

    override fun onDeactivate() {
        ApiManager.getRequestInterceptor().clearAccessToken()
        ApiManager.getRestApiInterface().revokeUser(UpPlatformSdkConstants.API_VERSION_STRING, object : Callback<Any> {
            override fun failure(error: RetrofitError?) {

            }

            override fun success(t: Any?, response: Response?) {
                println("successfully disconnected from UP server.")
            }

        })
        accessToken = null
        preferences.edit()
                .remove(UP_PLATFORM_ACCESS_TOKEN)
                .remove(UP_PLATFORM_REFRESH_TOKEN)
                .apply()
    }

    class JawboneAuthDependencyResolver : OTSystemDependencyResolver() {
        override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
            return Single.defer {
                val token = preferences.getString(UP_PLATFORM_ACCESS_TOKEN, null)
                if (token != null) {
                    ApiManager.getRequestInterceptor().setAccessToken(token)
                    Single.just(DependencyCheckResult(DependencyState.Passed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_passed, nameResourceId), ""))
                } else {
                    Single.just(DependencyCheckResult(DependencyState.FatalFailed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_failed, nameResourceId), context.getString(R.string.msg_sign_in)))
                }
            }
        }

        override fun tryResolve(activity: Activity): Single<Boolean> {
            return Single.defer {
                val builder = OauthUtils.setOauthParameters(CLIENT_ID, REDIRECT_URI, SCOPES)

                val intent = Intent(activity, OauthWebViewActivity::class.java)
                intent.putExtra(UpPlatformSdkConstants.AUTH_URI, builder.build())
                return@defer RxActivityResult.on(activity).startIntent(intent)
                        .firstOrError()
                        .flatMap { result ->
                            if (result.resultCode() == Activity.RESULT_OK) {
                                Single.create<Boolean> {
                                    subscriber ->
                                    val code = result.data().getStringExtra(UpPlatformSdkConstants.ACCESS_CODE)
                                    if (code != null) {
                                        //first clear older accessToken, if it exists..
                                        ApiManager.getRequestInterceptor().clearAccessToken()

                                        ApiManager.getRestApiInterface().getAccessToken(
                                                CLIENT_ID,
                                                CLIENT_SECRET,
                                                code,
                                                object : Callback<OauthAccessTokenResponse> {
                                                    override fun failure(error: RetrofitError) {
                                                        println("Jawbone Request failed. ${error.message}")
                                                        if (!subscriber.isDisposed) {
                                                            subscriber.onSuccess(false)
                                                        }
                                                    }

                                                    override fun success(result: OauthAccessTokenResponse, response: Response) {

                                                        if (result.access_token != null) {
                                                            val editor = preferences.edit()
                                                            editor.putString(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN, result.access_token)
                                                            editor.putString(UpPlatformSdkConstants.UP_PLATFORM_REFRESH_TOKEN, result.refresh_token)
                                                            editor.apply()

                                                            println("accessToken:" + result.access_token)
                                                            accessToken = result.access_token
                                                            ApiManager.getRequestInterceptor().setAccessToken(accessToken)
                                                            if (!subscriber.isDisposed) {
                                                                subscriber.onSuccess(true)
                                                            }
                                                        } else {
                                                            println("accessToken not returned by Oauth call, exiting...")
                                                            if (!subscriber.isDisposed) {
                                                                subscriber.onSuccess(false)
                                                            }
                                                        }
                                                    }
                                                })
                                    }
                                }
                            } else {
                                Single.just(false)
                            }
                        }.onErrorReturn { false }
            }
        }

    }

    /*
    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
        val token = preferences.getString(UP_PLATFORM_ACCESS_TOKEN, null)
        if (token != null) {
            ApiManager.getRequestInterceptor().setAccessToken(token)
            accessToken = token
            preparedHandler?.invoke(true)
        } else {
            preparedHandler?.invoke(false)
        }
    }*/

}