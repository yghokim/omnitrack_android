package kr.ac.snu.hcil.omnitrack.core.externals.fitbit

import android.content.Context
import android.content.Intent
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by younghokim on 16. 9. 2..
 */
object FitbitService : OTExternalService("FitbitService", 0) {

    const val SCOPE_ACTIVITY = "activity"
    const val SCOPE_HEARTRATE = "heartrate"
    const val SCOPE_SLEEP = "sleep"

    val DEFAULT_SCOPES = arrayOf(SCOPE_ACTIVITY, SCOPE_SLEEP, SCOPE_HEARTRATE)

    override fun handleActivityActivationResultOk(resultData: Intent?) {
    }

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
    }

    override fun onDeactivate() {
    }

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {
    }

    override val thumbResourceId: Int = R.drawable.service_thumb_googlefit
    override val descResourceId: Int = R.string.service_fitbit_name
    override val nameResourceId: Int = R.string.service_fitbit_desc

}