package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import android.app.Activity
import android.content.Context
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.BuildConfig
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.ThirdPartyAppDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import kr.ac.snu.hcil.omnitrack.utils.TextHelper

/**
 * Created by Young-Ho on 9/1/2016.
 */
object MisfitService : OTExternalService("MisfitService", 0) {

    override fun isSupportedInSystem(): Boolean {
        return BuildConfig.MISFIT_APP_KEY != null && BuildConfig.MISFIT_APP_SECRET != null
    }

    const val PREFERENCE_ACCESS_TOKEN = "misfit_access_token"

    override fun onRegisterMeasureFactories(): Array<OTMeasureFactory> {
        return arrayOf(MisfitStepMeasureFactory, MisfitSleepMeasureFactory)
    }

    fun getStoredAccessToken(): String? {
        if (preferences.contains(PREFERENCE_ACCESS_TOKEN)) {
            return preferences.getString(PREFERENCE_ACCESS_TOKEN, "")
        } else return null
    }

    override fun onDeactivate() {
        preferences.edit().remove(PREFERENCE_ACCESS_TOKEN).apply()
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_misfit

    override val nameResourceId: Int = R.string.service_misfit_name
    override val descResourceId: Int = R.string.service_misfit_desc

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return super.onRegisterDependencies() + arrayOf(
                MisfitAuthResolver(),
                ThirdPartyAppDependencyResolver.Builder(OTApp.instance)
                        .isMandatory(false)
                        .setAppName("Misfit")
                        .setPackageName("com.misfitwearables.prometheus")
                        .build()
        )
    }

    class MisfitAuthResolver : OTSystemDependencyResolver() {
        override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
            return Single.defer {
                if (getStoredAccessToken() != null) {
                    Single.just(DependencyCheckResult(DependencyState.Passed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_passed, nameResourceId), ""))
                } else {
                    Single.just(DependencyCheckResult(DependencyState.FatalFailed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_failed, nameResourceId), context.getString(R.string.msg_sign_in)))
                }
            }
        }

        override fun tryResolve(activity: Activity): Single<Boolean> {
            return MisfitApi.authorize(activity).doOnSuccess {
                token ->
                OTExternalService.preferences.edit().putString(MisfitService.PREFERENCE_ACCESS_TOKEN, token).apply()
            }.map { token -> !token.isBlank() }.onErrorReturn { err -> false }
        }

    }
}