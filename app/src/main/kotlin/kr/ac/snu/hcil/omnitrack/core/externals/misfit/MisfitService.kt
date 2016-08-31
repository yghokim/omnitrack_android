package kr.ac.snu.hcil.omnitrack.core.externals.misfit

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by Young-Ho on 9/1/2016.
 */
object MisfitService : OTExternalService("MisfitService", 0) {

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
    }

    override fun onDeactivate() {
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_misfit

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
    }

    override fun handleActivityActivationResult(resultCode: Int) {
    }

    override val nameResourceId: Int = R.string.service_misfit_name
    override val descResourceId: Int = R.string.service_misfit_desc
}