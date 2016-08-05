package kr.ac.snu.hcil.omnitrack.core.externals.shaomi.miband

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.zhaoxiaodan.miband.ActionCallback
import com.zhaoxiaodan.miband.MiBand
import com.zhaoxiaodan.miband.model.UserInfo
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.utils.AsyncTaskWithResultHandler

/**
 * Created by Young-Ho Kim on 2016-07-29.
 */

//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
object MiBandService : OTExternalService("ShaomiMiBand", 21) {

    const val PREFERENCE_KEY = "OmniTrack_MiBandService"
    const val PREFERENCE_VALUE_MAC = "deviceMac"

    override val thumbResourceId: Int = R.drawable.service_thumb_miband
    override val nameResourceId: Int = R.string.service_mi_band_name
    override val descResourceId: Int = R.string.service_mi_band_desc

    override val permissionGranted: Boolean
        get() = OmniTrackApplication.app.applicationContext.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private var device: BluetoothDevice? = null

    private var state: ServiceState = ServiceState.DEACTIVATED

    val band: MiBand by lazy {
        MiBand(OmniTrackApplication.app)
    }

    override fun getState(): ServiceState {
        return state
    }

    override fun activateAsync(connectedHandler: ((Boolean) -> Unit)?) {
            val task = ConnectionTask(connectedHandler)
            task.execute()
    }

    override fun deactivate() {

    }

    override fun grantPermissions(activity: Activity, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
        }
    }

    override fun grantPermissions(caller: Fragment, requestCode: Int) {
        caller.requestPermissions(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), requestCode)
    }

    private fun storeDeviceMac(address: String) {
        val pref = OmniTrackApplication.app.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(PREFERENCE_VALUE_MAC, address)
        editor.apply()
    }

    private fun restoreDeviceMac(): String? {
        val pref = OmniTrackApplication.app.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE)
        if (pref.contains(PREFERENCE_VALUE_MAC)) {
            return pref.getString(PREFERENCE_VALUE_MAC, "")
        } else return null
    }


    class ConnectionTask : AsyncTaskWithResultHandler {
        override fun doInBackground(vararg p0: Void?): Boolean {

            var device: BluetoothDevice? = null

            if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                val pairedDevices = adapter.bondedDevices

                for (d in pairedDevices) {
                    println("scan result: ${d.name}, ${d.address}")
                    if (d.address.startsWith("C8:0F:10") == true) {
                        println("found mi.")
                        storeDeviceMac(d.address)
                        device = d
                    }
                }

            }
/*
            val callback = object : ScanCallback() {
                override fun onScanFailed(errorCode: Int) {
                    super.onScanFailed(errorCode)
                }

                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    super.onScanResult(callbackType, result)
                    println("scan result: ${result?.device?.name}, ${result?.device?.address}")

                    if(result?.device?.address?.startsWith("C8:0F:10") == true)
                    {
                        println("found mi.")
                        MiBand.stopScan(this)
                        device = result?.device
                    }

                }
            }*/

            if (device != null) {
                storeDeviceMac(device.address)

                var connecting = true
                state = ServiceState.ACTIVATING


                band.connect(device, object : ActionCallback {
                    override fun onSuccess(data: Any?) {
                        println("Mi Band is successfully connected.")
                        state = ServiceState.ACTIVATED
                        connecting = false
                    }

                    override fun onFail(errorCode: Int, msg: String) {
                        connecting = false
                    }

                })

                while (connecting) {

                }


                band.setUserInfo(UserInfo(System.currentTimeMillis().toInt(), 1, 29, 178, 72, "Young-Ho", 0))
//                band.startVibration(VibrationMode.VIBRATION_10_TIMES_WITH_LED)

                return true
            } else {
                return false
            }

        }

        constructor(handler: ((Boolean) -> Unit)?) : super(handler)

    }

}