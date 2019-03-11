package kr.ac.snu.hcil.omnitrack.core.dependency

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.FragmentActivity
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.TextHelper
import kr.ac.snu.hcil.android.common.net.AuthConstants
import kr.ac.snu.hcil.android.common.net.OAuth2Client
import kr.ac.snu.hcil.android.common.net.WebServiceLoginActivity
import okhttp3.HttpUrl
import rx_activity_result2.RxActivityResult

/**
 * Created by younghokim on 2017. 5. 25..
 */
class OAuth2LoginDependencyResolver(val authClient: OAuth2Client, val identifier: String, val containerPreferences: SharedPreferences, val serviceName: String = "Service") : OTSystemDependencyResolver() {
    override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
        return Single.defer {
            val credential = OAuth2Client.Credential.restore(containerPreferences, identifier)
            if (credential != null) {
                return@defer Single.just(DependencyCheckResult(DependencyState.Passed, TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_passed, serviceName)), ""))
            } else {
                return@defer Single.just(DependencyCheckResult(DependencyState.FatalFailed, TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_failed, serviceName)), authClient.context.getString(kr.ac.snu.hcil.omnitrack.core.dependency.R.string.msg_sign_in)))
            }
        }
    }


    private fun authorize(authClient: OAuth2Client, activity: Activity, serviceName: String? = null): Single<OAuth2Client.Credential> {
        val uri = HttpUrl.parse(authClient.config.authorizationUrl)!!.newBuilder()
                .addQueryParameter(AuthConstants.PARAM_CLIENT_ID, authClient.config.clientId)
                .addQueryParameter(AuthConstants.PARAM_RESPONSE_TYPE, AuthConstants.VALUE_RESPONSE_TYPE_CODE)
                .addQueryParameter(AuthConstants.PARAM_REDIRECT_URI, authClient.config.redirectUri)
                .addQueryParameter(AuthConstants.PARAM_SCOPE, authClient.config.scope)
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
                        return@flatMap authClient.exchangeToken(code)
                    } else {
                        return@flatMap Single.error<OAuth2Client.Credential>(Exception("Authentication process was canceled by user."))
                    }
                }


        //activity.startActivityForResult(WebServiceLoginActivity.makeIntent(uri.toString(), serviceName ?: "Service", activity), activityRequestCode)
    }

    override fun tryResolve(activity: FragmentActivity): Single<Boolean> {
        return authorize(authClient, activity, serviceName)
                .doOnSuccess { credential ->
                    credential.store(containerPreferences, identifier)
                }
                .map { credential ->
                    true
                }.onErrorReturn { false }
    }
}