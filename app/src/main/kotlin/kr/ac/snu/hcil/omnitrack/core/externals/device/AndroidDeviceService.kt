package kr.ac.snu.hcil.omnitrack.core.externals.device

import android.content.Context
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by younghokim on 16. 8. 4..
 */
object AndroidDeviceService : OTExternalService("AndroidDeviceService", 19) {
    override fun handleActivityActivationResult(resultCode: Int) {
    }

    override fun prepareServiceAsync(preparedHandler: ((Boolean) -> Unit)?) {

    }

    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {

    }

    override fun onDeactivate() {

    }

    override val thumbResourceId: Int = R.drawable.service_thumb_androiddevice
    override val nameResourceId: Int = R.string.service_device_name
    override val descResourceId: Int = R.string.service_device_desc

}