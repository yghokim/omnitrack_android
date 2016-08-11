package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import android.content.Context
import com.google.android.gms.common.api.Api
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory
import java.util.*

/**
 * Created by younghokim on 16. 8. 8..
 */
object GoogleFitService : OTExternalService("GoogleFitService", 19) {

    override val nameResourceId: Int = R.string.service_googlefit_name
    override val descResourceId: Int = R.string.service_googlefit_desc
    override val thumbResourceId: Int = R.drawable.service_thumb_googlefit

    override val permissionGranted: Boolean = true

    private var client: GoogleApiClient? = null

    val usedApis: Array<Api<out Api.ApiOptions.NotRequiredOptions>> by lazy {

        val set = HashSet<Api<out Api.ApiOptions.NotRequiredOptions>>()
        for (m in _measureFactories) {
            if (m is GoogleFitMeasureFactory) {
                set.add(m.usedAPI)
            }
        }

        set.toTypedArray()
    }

    val usedScopes: Array<Scope> by lazy {

        val set = HashSet<Scope>()
        for (m in _measureFactories) {
            if (m is GoogleFitMeasureFactory) {
                set.add(m.usedScope)
            }
        }

        set.toTypedArray()
    }

    init {
        _measureFactories.add(GoogleFitStepsFactory())
    }

    override fun getState(): ServiceState {
        return if (client != null) {
            ServiceState.ACTIVATED
        } else {
            ServiceState.DEACTIVATED
        }
    }

    override fun activateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {

    }

    override fun deactivate() {

    }

    override fun prepareService() {

    }

    private fun buildClientBuilderBase(context: Context = OmniTrackApplication.app): GoogleApiClient.Builder {
        val builder = GoogleApiClient.Builder(context)

        for (api in usedApis) {
            builder.addApi(api)
        }

        for (scope in usedScopes) {
            builder.addScope(scope)
        }


        return builder
    }

    abstract class GoogleFitMeasureFactory<T> : OTMeasureFactory<T>() {
        abstract val usedAPI: Api<out Api.ApiOptions.NotRequiredOptions>
        abstract val usedScope: Scope
    }
}