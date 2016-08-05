package kr.ac.snu.hcil.omnitrack.core.externals.device

import android.app.Activity
import android.support.v4.app.Fragment
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by younghokim on 16. 8. 4..
 */
object AndroidDeviceService : OTExternalService("AndroidDeviceService", 19) {


    override val permissionGranted: Boolean
        get() = true

    override fun activateAsync(connectedHandler: ((Boolean) -> Unit)?) {

    }

    override fun getState(): ServiceState {
        return ServiceState.DEACTIVATED
    }

    override fun deactivate() {

    }

    override fun grantPermissions(caller: Activity, requestCode: Int) {
    }

    override fun grantPermissions(caller: Fragment, requestCode: Int) {
    }


    override val thumbResourceId: Int = R.drawable.service_thumb_androiddevice
    override val nameResourceId: Int = R.string.service_device_name
    override val descResourceId: Int = R.string.service_device_desc

}