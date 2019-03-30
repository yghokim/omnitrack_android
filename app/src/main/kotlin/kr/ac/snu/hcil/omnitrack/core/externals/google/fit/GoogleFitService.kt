package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kr.ac.snu.hcil.android.common.TextHelper
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.dependency.OTSystemDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.dependency.ThirdPartyAppDependencyResolver
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalServiceManager
import kr.ac.snu.hcil.omnitrack.core.externals.OTServiceMeasureFactory
import rx_activity_result2.RxActivityResult
import java.util.*

/**
 * Created by Young-Ho Kim on 16. 8. 8
 */
class GoogleFitService(context: Context, pref: SharedPreferences) : OTExternalService(context, pref, "GoogleFitService", 19) {

    override val requiredApiKeyNames: Array<String> by lazy {
        emptyArray<String>()
    }

    override fun isSupportedInSystem(serviceManager: OTExternalServiceManager): Boolean {
        return true
    }

    override val nameResourceId: Int = R.string.service_googlefit_name
    override val descResourceId: Int = R.string.service_googlefit_desc
    override val thumbResourceId: Int = R.drawable.service_thumb_googlefit

    private var client: GoogleApiClient? = null

    val usedApis: Array<Api<out Api.ApiOptions.NotRequiredOptions>> by lazy {

        val set = HashSet<Api<out Api.ApiOptions.NotRequiredOptions>>()
        for (m in measureFactories) {
            if (m is GoogleFitMeasureFactory) {
                set.add(m.usedAPI)
            }
        }

        set.toTypedArray()
    }

    val usedScopes: Array<Scope> by lazy {

        val set = HashSet<Scope>()
        for (m in measureFactories) {
            if (m is GoogleFitMeasureFactory) {
                set.add(m.usedScope)
            }
        }

        set.toTypedArray()
    }

    override fun onRegisterMeasureFactories(): Array<OTServiceMeasureFactory> {
        return arrayOf(GoogleFitStepsFactory(context, this))
    }

    override fun onRegisterDependencies(): Array<OTSystemDependencyResolver> {
        return arrayOf(
                GoogleFitAuthDependencyResolver(this),
                ThirdPartyAppDependencyResolver.Builder(context)
                        .setPackageName("com.google.android.apps.fitness")
                        .isMandatory(false)
                        .setAppName(R.string.service_googlefit_app_name)
                        .build()
        )
    }

    override fun onDeactivate(): Completable {
        return Completable.defer {
            client?.disconnect()
            client = null
            return@defer Completable.complete()
        }
    }

    /*
    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
        getConnectedClient().subscribe({
            client ->
            preparedHandler?.invoke(true)
        }, {
            preparedHandler?.invoke(false)
        })
    }*/

    fun getConnectedClient(): Single<GoogleApiClient> {
        return getConnectedClientOrResolve(null)
    }

    fun getConnectedClientOrResolve(callerActivity: Activity?): Single<GoogleApiClient> {
        return Single.defer {
            return@defer if (client?.isConnected == true) {
                Single.just(client!!)
            } else {
                Single.create<ConnectionResult> { subscriber ->
                    if (client == null) {
                        client = buildClientBuilderBase().build()
                    }
                    val result = client!!.blockingConnect()
                    if (!subscriber.isDisposed) {
                        subscriber.onSuccess(result)
                    }
                }.subscribeOn(Schedulers.io()).flatMap { result ->
                    if (result.isSuccess) {
                        return@flatMap Single.just(client!!)
                    } else if (result.hasResolution() && callerActivity != null) {
                        val resolutionPendingIntent = result.resolution!!
                        return@flatMap RxActivityResult.on(callerActivity)
                                .startIntentSender(resolutionPendingIntent.intentSender, Intent(), 0, 0, 0)
                                .firstOrError()
                                .flatMap {
                                    if (it.resultCode() == Activity.RESULT_OK) {
                                        getConnectedClientOrResolve(callerActivity)
                                    } else Single.error(Exception("Resolution activity was canceled."))
                                }
                    } else {
                        return@flatMap Single.error<GoogleApiClient>(Exception("errorCode: ${result.errorCode}, message: ${result.errorMessage}"))
                    }
                }
            }
        }
    }

    private fun buildClientBuilderBase(): GoogleApiClient.Builder {
        val builder = GoogleApiClient.Builder(context)

        for (api in usedApis) {
            builder.addApi(api)
        }

        for (scope in usedScopes) {
            builder.addScope(scope)
        }
        return builder
    }

    abstract class GoogleFitMeasureFactory(context: Context, service: GoogleFitService, typeKey: String) : OTServiceMeasureFactory(context, service, typeKey) {
        abstract val usedAPI: Api<out Api.ApiOptions.NotRequiredOptions>
        abstract val usedScope: Scope
    }

    class GoogleFitAuthDependencyResolver(val parentService: GoogleFitService) : OTSystemDependencyResolver() {
        override fun checkDependencySatisfied(context: Context, selfResolve: Boolean): Single<DependencyCheckResult> {
            return parentService.getConnectedClient().observeOn(AndroidSchedulers.mainThread()).map {
                client ->
                DependencyCheckResult(DependencyState.Passed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_passed, parentService.nameResourceId), "")
            }.onErrorReturn { err -> DependencyCheckResult(DependencyState.FatalFailed, TextHelper.formatWithResources(context, R.string.msg_format_dependency_sign_in_failed, parentService.nameResourceId), context.getString(R.string.msg_sign_in)) }
        }

        override fun tryResolve(activity: FragmentActivity): Single<Boolean> {
            return parentService.getConnectedClientOrResolve(activity).map { true }.onErrorReturn { err ->
                err.printStackTrace()
                false
            }
        }
    }
}