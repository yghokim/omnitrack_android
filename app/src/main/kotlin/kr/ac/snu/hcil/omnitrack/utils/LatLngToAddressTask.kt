package kr.ac.snu.hcil.omnitrack.utils

import android.location.Address
import android.os.AsyncTask
import android.view.View
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapReverseGeoCoder

/**
 * Created by Young-Ho on 8/15/2016.
 */
class LatLngToAddressTask(private val listener: OnFinishListener, private val view: View) : AsyncTask<LatLng, Void, String?>(), MapReverseGeoCoder.ReverseGeoCodingResultListener {

    interface OnFinishListener {
        fun onAddressReceived(address: String?);
    }

    override fun onPostExecute(result: String?) {
        listener.onAddressReceived(result)
    }

    override fun doInBackground(vararg args: LatLng): String? {
        var googleAddress: Address? = null
        while (googleAddress == null) {
            googleAddress = args.first().getAddress(OmniTrackApplication.app)
        }

        if (googleAddress.countryCode == "KR") {
            val daumGeocoder = MapReverseGeoCoder("	83165b09ca2e119324c4f438c3ae0e5e", MapPoint.mapPointWithGeoCoord(args.first().latitude, args.first().longitude), this, view.getActivity())
            val address = daumGeocoder.findAddressForMapPointSync("83165b09ca2e119324c4f438c3ae0e5e", MapPoint.mapPointWithGeoCoord(args.first().latitude, args.first().longitude), MapReverseGeoCoder.AddressType.FullAddress)

            return address
        } else {
            return googleAddress.getAddressLine(0)
        }

    }


    override fun onReverseGeoCoderFailedToFindAddress(p0: MapReverseGeoCoder?) {
        println("Fail")
    }

    override fun onReverseGeoCoderFoundAddress(p0: MapReverseGeoCoder?, p1: String?) {
        println("Success")
    }

}