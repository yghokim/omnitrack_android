package kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band

import android.app.Activity
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import com.microsoft.band.UserConsent
import com.microsoft.band.sensors.BandHeartRateEvent
import com.microsoft.band.sensors.BandHeartRateEventListener
import com.microsoft.band.sensors.HeartRateConsentListener
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory

/**
 * Created by younghokim on 16. 7. 28..
 */
class MicrosoftBandHeartRateFactory : OTMeasureFactory<Int>() {

    override val nameResourceId: Int = R.string.measure_microsoft_band_heart_rate_name
    override val descResourceId: Int = R.string.measure_microsoft_band_heart_rate_desc

    private val sensorListener = SensorListener()

    private var pending = false
    private var acquiredValue = 0

    private var readStartedAt: Long = 0
    private val timeout = 1000

    init {
        val sensorManager = MicrosoftBandService.getClient()?.sensorManager
        if (sensorManager != null) {
            permissionGranted = sensorManager.currentHeartRateConsent == UserConsent.GRANTED
        }
    }

    override fun awaitRequestValue(): Int {

        val sensorManager = MicrosoftBandService.getClient()?.sensorManager
        if (sensorManager != null) {
            pending = true
            readStartedAt = System.currentTimeMillis()

            try {
                sensorManager.registerHeartRateEventListener(sensorListener)
                while (pending) {
                    println("pending")
                    if (System.currentTimeMillis() - readStartedAt > timeout) {
                        break;
                    }
                }

                sensorManager.unregisterHeartRateEventListener(sensorListener)
                return acquiredValue
            } catch(e: Exception) {
                e.printStackTrace()
                return 76
            }
        }

        return 0
    }

    override fun requestValueAsync(handler: (Int) -> Unit) {
        SensorReceptionTask(handler).execute()
    }

    override fun grantPermissions(activity: Activity, handler: ((Boolean) -> Unit)?) {
        super.grantPermissions(activity, handler)

        val sensorManager = MicrosoftBandService.getClient()?.sensorManager
        if (sensorManager != null) {
            println("current state : ${sensorManager.currentHeartRateConsent}")
            if (sensorManager.currentHeartRateConsent != UserConsent.GRANTED) {
                sensorManager.requestHeartRateConsent(activity) {
                    accepted ->
                    println("Heart rate consent accepted.")
                    permissionGranted = true
                    handler?.invoke(accepted)
                }
            }
        }
    }

    inner class SensorListener : BandHeartRateEventListener {
        override fun onBandHeartRateChanged(p0: BandHeartRateEvent?) {
            if (p0 != null) {
                println("acquiring band value...")
                acquiredValue = p0.heartRate
                pending = false
            }
        }

    }

    inner class SensorReceptionTask(val handler: ((Int) -> Unit)?) : AsyncTask<Void?, Void?, Int>() {
        override fun doInBackground(vararg params: Void?): Int {
            return awaitRequestValue()
        }

        override fun onPostExecute(result: Int) {
            super.onPostExecute(result)
            handler?.invoke(result)
        }
    }

}