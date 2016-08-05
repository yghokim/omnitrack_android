package kr.ac.snu.hcil.omnitrack.core.externals.device

import android.app.Activity
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService

/**
 * Created by younghokim on 16. 8. 4..
 */
object AndroidDeviceService : OTExternalService("AndroidDeviceService", 19) {

    override var permissionGranted: Boolean
        get() = throw UnsupportedOperationException()
        set(value) {
        }

    override fun isActivated(): Boolean {
        return false
    }

    override fun isActivating(): Boolean {
        return false
    }

    override fun activateAsync(connectedHandler: ((Boolean) -> Unit)?) {

    }

    override fun deactivate() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun grantPermissions(activity: Activity, handler: ((Boolean) -> Unit)?) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val nameResourceId: Int = R.string.service_device_name
    override val descResourceId: Int = R.string.service_device_desc

}