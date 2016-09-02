package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by younghokim on 16. 9. 2..
 */
class FitbitService : OTExternalService("FitbitService", 0) {
    override fun handleActivityActivationResultOk(resultData: Intent?) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDeactivate() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_googlefit
    override val descResourceId: Int = R.string.service_fitbit_name
    override val nameResourceId: Int = R.string.service_fitbit_desc

}