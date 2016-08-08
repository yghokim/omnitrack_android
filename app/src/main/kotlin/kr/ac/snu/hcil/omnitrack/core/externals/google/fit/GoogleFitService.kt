package kr.ac.snu.hcil.omnitrack.core.externals.google.fit

import android.app.Activity
import android.content.Context
import android.support.v4.app.Fragment
import com.google.android.gms.common.api.GoogleApiClient
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by younghokim on 16. 8. 8..
 */
object GoogleFitService : OTExternalService("GoogleFitService", 19) {

    override val nameResourceId: Int = R.string.service_googlefit_name
    override val descResourceId: Int = R.string.service_googlefit_desc
    override val thumbResourceId: Int = R.drawable.service_thumb_googlefit

    override val permissionGranted: Boolean = true

    private var client: GoogleApiClient? = null

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

    override fun grantPermissions(caller: Fragment, requestCode: Int) {

    }

    override fun grantPermissions(caller: Activity, requestCode: Int) {

    }

    override fun prepareService() {

    }

    private fun buildClientBuilderBase(context: Context = OmniTrackApplication.app): GoogleApiClient.Builder {
        return GoogleApiClient.Builder(context)

    }
}