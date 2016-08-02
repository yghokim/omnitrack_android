package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ShortTextInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 8/2/2016.
 */
class OTLocationAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?) : OTAttribute<LatLng>(objectId, dbId, columnName, Companion.TYPE_LOCATION, settingData) {
    override val keys: Array<Int>
        get() = throw UnsupportedOperationException()

    override val typeNameResourceId: Int = R.string.type_location_name

    override fun createProperties() {

    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_LATITUDE_LONGITUDE

    override fun formatAttributeValue(value: Any): String {
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (LatLng) -> Unit) {
        val task = GetCurrentLocationTask {
            result ->
            resultHandler.invoke(result)
        }

        task.execute()
    }


    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_SHORT_TEXT
    }

    override fun refreshInputViewContents(inputView: AAttributeInputView<out Any>) {
        if (inputView is ShortTextInputView) {
            getAutoCompleteValueAsync {
                result ->
                inputView.value = result.toString()
            }
        }
    }

    class GetCurrentLocationTask(val handler: (LatLng) -> Unit) : AsyncTask<Void?, Void?, LatLng>() {

        companion object {
            fun getCachedLocation(lm: LocationManager, enabledOnly: Boolean): Location? {
                var bestLocation: Location? = null
                for (provider in lm.getProviders(enabledOnly)) {
                    val l = lm.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        // Found best last known location: %s", l);
                        bestLocation = l
                    }
                }
                return bestLocation
            }
        }

        private var searchingLocation: Boolean = false
        private var updatedLocation: Location? = null
        private var updateCount = 0

        override fun doInBackground(vararg p0: Void?): LatLng {
            val locationManager = OmniTrackApplication.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!gpsEnabled && !networkEnabled) {
                val cached = getCachedLocation(locationManager, false)
                if (cached == null) {
                    return LatLng(318693.57301624, 4147876.8541539) //Seoul National University
                } else return LatLng(cached.latitude, cached.longitude)
            } else {
                searchingLocation = true
                val bestProvider = locationManager.getBestProvider(Criteria(), true)
                locationManager.requestLocationUpdates(bestProvider, 0, 0.0f, LocationListener())

                while (searchingLocation) {

                }

                if (updatedLocation == null) {
                    return LatLng(318693.57301624, 4147876.8541539) //Seoul National University
                } else return LatLng(updatedLocation!!.latitude, updatedLocation!!.longitude)
            }
        }

        override fun onPostExecute(result: LatLng) {
            super.onPostExecute(result)
            handler.invoke(result)
        }

        inner class LocationListener : android.location.LocationListener {
            override fun onLocationChanged(p0: Location) {
                if (updatedLocation != null) {
                    updatedLocation = if (updatedLocation!!.accuracy > p0.accuracy) {
                        updatedLocation
                    } else {
                        p0
                    }
                } else updatedLocation = p0

                updateCount++
                if (updateCount >= 2) {
                    searchingLocation = false
                }
            }

            override fun onProviderDisabled(p0: String?) {

            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

            }

            override fun onProviderEnabled(p0: String?) {

            }

        }

    }

}
