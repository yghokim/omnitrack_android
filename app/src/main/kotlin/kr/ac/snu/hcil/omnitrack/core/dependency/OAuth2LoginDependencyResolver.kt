package kr.ac.snu.hcil.omnitrack.core.dependency

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.FragmentActivity
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.TextHelper
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client

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
                return@defer Single.just(DependencyCheckResult(DependencyState.FatalFailed, TextHelper.fromHtml(TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_failed, serviceName)), authClient.context.getString(R.string.msg_sign_in)))
            }
        }
    }

    override fun tryResolve(activity: FragmentActivity): Single<Boolean> {
        return authClient.authorize(activity, serviceName)
                .doOnSuccess { credential ->
                    credential.store(containerPreferences, identifier)
                }
                .map { credential ->
                    true
                }.onErrorReturn { false }
    }
}