package kr.ac.snu.hcil.omnitrack.utils

import android.location.Address
import android.os.AsyncTask
import android.view.View
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by Young-Ho on 8/15/2016.
 */
class LatLngToAddressTask(private val listener: OnFinishListener, private val view: View) : AsyncTask<LatLng, Void, String?>() {

    interface OnFinishListener {
        fun onAddressReceived(address: String?);
    }

    override fun onPostExecute(result: String?) {
        listener.onAddressReceived(result)
    }

    override fun doInBackground(vararg args: LatLng): String? {
        var googleAddress: Address? = null
        while (googleAddress == null) {
            googleAddress = args.first().getAddress(OTApplication.app)
        }

        return googleAddress.getAddressLine(googleAddress.maxAddressLineIndex)
    }
}