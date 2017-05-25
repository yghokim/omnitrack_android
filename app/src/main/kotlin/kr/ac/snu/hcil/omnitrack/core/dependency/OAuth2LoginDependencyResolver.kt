package kr.ac.snu.hcil.omnitrack.core.dependency

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.auth.OAuth2Client
import rx.Single

/**
 * Created by younghokim on 2017. 5. 25..
 */
class OAuth2LoginDependencyResolver(val authClient: OAuth2Client, val identifier: String, val containerPreferences: SharedPreferences) : OTSystemDependencyResolver() {
    override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
        return Single.defer {
            val credential = OAuth2Client.Credential.restore(containerPreferences, identifier)
            if (credential != null) {
                return@defer Single.just(DependencyCheckResult(DependencyState.Passed, "You are currently logged in.", ""))
            } else {
                return@defer Single.just(DependencyCheckResult(DependencyState.FatalFailed, "You are not logged in.", OTApplication.getString(R.string.msg_sign_in)))
            }
        }
    }

    override fun tryResolve(activity: Activity): Single<Boolean> {
        TODO()
    }
}