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
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.ThirdPartyAppDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import kr.ac.snu.hcil.omnitrack.utils.getDayOfMonth
import kr.ac.snu.hcil.omnitrack.utils.getYear
import kr.ac.snu.hcil.omnitrack.utils.getZeroBasedMonth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rx_activity_result2.RxActivityResult
import java.util.*


/**
 * Created by younghokim on 2017. 1. 24..
 */
class JawboneUpService(context: Context) : OTExternalService(context, "JawboneUpService", 9) {

    override fun isSupportedInSystem(): Boolean {
        return BuildConfig.JAWBONE_CLIENT_ID != null && BuildConfig.JAWBONE_CLIENT_SECRET != null && BuildConfig.JAWBONE_REDIRECT_URI != null
    }

    fun makeFormattedDateInteger(date: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        return cal.getYear() * 10000 + (cal.getZeroBasedMonth() + 1) * 100 + cal.getDayOfMonth()
    }

    fun makeIntraDayRequestQueryParams(start: Date, end: Date, data: HashMap<String, Long>?): HashMap<String, Long> {
        val result = data ?: HashMap()

        result.put("date", makeFormattedDateInteger(start.time).toLong())
        result.put("start_date", start.time)
        result.put("end_date", end.time)

        return result
    }

    val SCOPES: List<UpPlatformSdkConstants.UpPlatformAuthScope> by lazy {
        arrayListOf(
                UpPlatformSdkConstants.UpPlatformAuthScope.MOVE_READ,
                UpPlatformSdkConstants.UpPlatformAuthScope.WEIGHT_READ,
                UpPlatformSdkConstants.UpPlatformAuthScope.SLEEP_READ
        )
    }


    override fun onRegisterMeasureFactories(): Array<OTMeasureFactory> {
        return arrayOf(
                JawboneStepMeasureFactory(context, this),
                JawboneDistanceMeasureFactory(context, this))
    }

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return super.onRegisterDependencies() + arrayOf(
                JawboneAuthDependencyResolver(this),
                ThirdPartyAppDependencyResolver.Builder(context)
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

    override fun onDeactivate(): Completable {
        return Completable.create {
            ApiManager.getRequestInterceptor().clearAccessToken()
            ApiManager.getRestApiInterface().revokeUser(UpPlatformSdkConstants.API_VERSION_STRING, object : Callback<Any> {
                override fun onFailure(call: Call<Any>?, t: Throwable) {
                    it.onError(t)
                }

                override fun onResponse(call: Call<Any>?, response: Response<Any>?) {
                    println("successfully disconnected from UP server.")
                    accessToken = null
                    externalServiceManager.get().preferences.edit()
                            .remove(UP_PLATFORM_ACCESS_TOKEN)
                            .remove(UP_PLATFORM_REFRESH_TOKEN)
                            .apply()
                    it.onComplete()
                }

            })
        }
    }


    class JawboneAuthDependencyResolver(val parentService: JawboneUpService) : OTSystemDependencyResolver() {
        override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
            return Single.defer {
                val token = parentService.externalServiceManager.get().preferences.getString(UP_PLATFORM_ACCESS_TOKEN, null)
                if (token != null) {
                    ApiManager.getRequestInterceptor().setAccessToken(token)
                    Single.just(DependencyCheckResult(DependencyState.Passed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_passed, parentService.nameResourceId), ""))
                } else {
                    Single.just(DependencyCheckResult(DependencyState.FatalFailed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_failed, parentService.nameResourceId), context.getString(R.string.msg_sign_in)))
                }
            }
        }

        override fun tryResolve(activity: Activity): Single<Boolean> {
            return Single.defer {
                val builder = OauthUtils.setOauthParameters(BuildConfig.JAWBONE_CLIENT_ID, BuildConfig.JAWBONE_REDIRECT_URI, parentService.SCOPES)

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
                                                BuildConfig.JAWBONE_CLIENT_ID,
                                                BuildConfig.JAWBONE_CLIENT_SECRET,
                                                code,
                                                object : Callback<OauthAccessTokenResponse> {
                                                    override fun onFailure(call: Call<OauthAccessTokenResponse>?, t: Throwable?) {
                                                        println("Jawbone Request failed. ${t?.message}")
                                                        if (!subscriber.isDisposed) {
                                                            subscriber.onSuccess(false)
                                                        }
                                                    }

                                                    override fun onResponse(call: Call<OauthAccessTokenResponse>, response: Response<OauthAccessTokenResponse>) {
                                                        val res = response.body()!!
                                                        if (res.access_token != null) {
                                                            val editor = parentService.externalServiceManager.get().preferences.edit()
                                                            editor.putString(UpPlatformSdkConstants.UP_PLATFORM_ACCESS_TOKEN, res.access_token)
                                                            editor.putString(UpPlatformSdkConstants.UP_PLATFORM_REFRESH_TOKEN, res.refresh_token)
                                                            editor.apply()

                                                            println("accessToken:" + res.access_token)
                                                            parentService.accessToken = res.access_token
                                                            ApiManager.getRequestInterceptor().setAccessToken(parentService.accessToken)
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