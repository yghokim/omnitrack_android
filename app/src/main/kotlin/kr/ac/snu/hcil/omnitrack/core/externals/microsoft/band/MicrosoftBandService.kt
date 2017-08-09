package kr.ac.snu.hcil.omnitrack.core.externals.microsoft.band

import android.os.AsyncTask
import com.microsoft.band.BandClient
import com.microsoft.band.BandClientManager
import com.microsoft.band.BandException
import com.microsoft.band.ConnectionState
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.externals.OTExternalService
import kr.ac.snu.hcil.omnitrack.core.externals.OTMeasureFactory

/**
 * Created by younghokim on 16. 7. 28..
 */
object MicrosoftBandService : OTExternalService("MicrosoftBandService", 19) {

    override val permissionGranted: Boolean = true

    override val thumbResourceId: Int = R.drawable.service_thumb_microsoftband
    override val nameResourceId: Int = R.string.service_microsoft_band_name
    override val descResourceId: Int = R.string.service_microsoft_band_desc

    private var connectionTask: ConnectionTask? = null

    private var connectionState: ConnectionState? = null

    override fun onRegisterMeasureFactories(): Array<OTMeasureFactory> {
        return arrayOf(MicrosoftBandHeartRateFactory())
    }

    /*
    override fun getState(): ServiceState {
        if (connectionState == ConnectionState.CONNECTED) {
            return ServiceState.ACTIVATED
        } else if (connectionTask != null) {
            return ServiceState.ACTIVATING
        } else return ServiceState.DEACTIVATED
    }*/

/*
    override fun onActivateAsync(context: Context, connectedHandler: ((Boolean) -> Unit)?) {
            val client = getClient()
            if(client!=null)
            {
                //val permission = PermissionChecker.checkSelfPermission(OTApplication.app, "com.microsoft.band.service.access.BIND_BAND_SERVICE")

                connectionTask = ConnectionTask(client, connectedHandler)
                connectionTask?.execute(null);
            }

    }*/

    override fun onDeactivate() {

    }

    fun getClient(): BandClient? {
        val pairedBands = BandClientManager.getInstance().pairedBands
        println("${pairedBands.size} bands are paired.")
        if (pairedBands.size > 0) {
            return BandClientManager.getInstance().create(OTApplication.app, pairedBands[0])
        } else return null
    }

    class ConnectionTask(val client: BandClient, val handler: ((Boolean) -> Unit)?) : AsyncTask<Void?, Void?, ConnectionState?>() {

        override fun doInBackground(vararg params: Void?): ConnectionState? {
            val pendingState = client.connect()
            try {
                val connectionState = pendingState.await()
                return connectionState
            } catch(interruption: InterruptedException) {
                interruption.printStackTrace()
                return null
            } catch(bandEx: BandException) {
                bandEx.printStackTrace()
                return null
            }
        }

        override fun onPostExecute(result: ConnectionState?) {
            super.onPostExecute(result)

            handler?.invoke(result == ConnectionState.CONNECTED)
            connectionState = result

            println("MS Band connection: ${result == ConnectionState.CONNECTED}")
        }

    }

}