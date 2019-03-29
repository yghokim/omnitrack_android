package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.FragmentActivity
import io.reactivex.Completable
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.TextHelper
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.ThirdPartyAppDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory

/**
 * Created by Young-Ho on 9/1/2016.
 */
class MisfitService(context: Context, pref: SharedPreferences) : OTExternalService(context, pref, "MisfitService", 0) {

    companion object {
        const val PREFERENCE_ACCESS_TOKEN = "misfit_access_token"

        const val KEY_APP_KEY = "MISFIT_APP_KEY"
        const val KEY_APP_SECRET = "MISFIT_APP_SECRET"
    }

    val api: MisfitApi by lazy {
        MisfitApi(context)
    }

    override fun isSupportedInSystem(serviceManager: OTExternalServiceManager): Boolean {
        return !serviceManager.getApiKey(KEY_APP_KEY).isNullOrBlank() && !serviceManager.getApiKey(KEY_APP_SECRET).isNullOrBlank()
    }

    override fun onRegisterMeasureFactories(): Array<OTServiceMeasureFactory> {
        return arrayOf(MisfitStepMeasureFactory(context, this), MisfitSleepMeasureFactory(context, this))
    }

    fun getStoredAccessToken(): String? {
        if (preferences.contains(PREFERENCE_ACCESS_TOKEN)) {
            return preferences.getString(PREFERENCE_ACCESS_TOKEN, "")
        } else return null
    }

    override fun onDeactivate(): Completable {
        return Completable.defer {
            preferences.edit().remove(PREFERENCE_ACCESS_TOKEN).apply()
            return@defer Completable.complete()
        }
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_misfit

    override val nameResourceId: Int = R.string.service_misfit_name
    override val descResourceId: Int = R.string.service_misfit_desc

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return super.onRegisterDependencies() + arrayOf(
                MisfitAuthResolver(this),
                ThirdPartyAppDependencyResolver.Builder(context)
                        .isMandatory(false)
                        .setAppName("Misfit")
                        .setPackageName("com.misfitwearables.prometheus")
                        .build()
        )
    }

    class MisfitAuthResolver(val parentService: MisfitService) : OTSystemDependencyResolver() {
        override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
            return Single.defer {
                if (parentService.getStoredAccessToken() != null) {
                    Single.just(DependencyCheckResult(DependencyState.Passed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_passed, parentService.nameResourceId), ""))
                } else {
                    Single.just(DependencyCheckResult(DependencyState.FatalFailed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_failed, parentService.nameResourceId), context.getString(R.string.msg_sign_in)))
                }
            }
        }

        override fun tryResolve(activity: FragmentActivity): Single<Boolean> {
            return parentService.api.authorize(activity).doOnSuccess {
                token ->
                parentService.preferences.edit().putString(MisfitService.PREFERENCE_ACCESS_TOKEN, token).apply()
            }.map { token -> !token.isBlank() }.onErrorReturn { err -> false }
        }

    }
}