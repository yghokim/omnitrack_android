package kr.ac.snu.hcil.omnitrack.utils

import android.location.Location
import android.os.AsyncTask
import com.google.android.gms.maps.model.LatLng
import io.nlopez.smartlocation.OnLocationUpdatedListener
import io.nlopez.smartlocation.SmartLocation
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by younghokim on 16. 8. 21..
 */
class LocationRetrievalTask(val listener: (LatLng) -> Unit) : AsyncTask<Void?, Void?, LatLng?>(), OnLocationUpdatedListener {


    private var location: Location? = null

    override fun doInBackground(vararg args: Void?): LatLng {
        location = null
        SmartLocation.with(OTApplication.app).location().oneFix().start(this)
        while (location == null && !isCancelled) {
            if (isCancelled) {
                val last = SmartLocation.with(OTApplication.app).location().lastLocation
                return LatLng(last?.latitude ?: 0.0, last?.longitude ?: 0.0)
            }
        }

        return LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
    }

    override fun onLocationUpdated(p0: Location?) {
        location = p0
    }

    override fun onPostExecute(result: LatLng?) {
        listener.invoke(result ?: LatLng(318693.57301624, 4147876.8541539))
    }


}