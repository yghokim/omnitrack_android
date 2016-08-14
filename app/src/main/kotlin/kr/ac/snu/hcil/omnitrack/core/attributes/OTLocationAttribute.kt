package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.OmniTrackApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.LocationInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

/**
 * Created by Young-Ho on 8/2/2016.
 */
class OTLocationAttribute(objectId: String?, dbId: Long?, columnName: String, settingData: String?, connectionData: String?) : OTAttribute<LatLng>(objectId, dbId, columnName, TYPE_LOCATION, settingData, connectionData) {

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

    override val propertyKeys: Array<Int> = Array<Int>(0) { index -> 0 }

    override val typeNameResourceId: Int = R.string.type_location_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_location

    override fun createProperties() {

    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_LATITUDE_LONGITUDE

    override fun formatAttributeValue(value: Any): String {
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (LatLng) -> Unit) {
        val locationManager = OmniTrackApplication.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!gpsEnabled && !networkEnabled) {
            val cached = getCachedLocation(locationManager, false)
            resultHandler.invoke(if (cached == null) {
                LatLng(318693.57301624, 4147876.8541539) //Seoul National University
            } else LatLng(cached.latitude, cached.longitude))
        } else {
            var updatedLocation: Location? = null
            var updateCount = 0
            val listener = object : android.location.LocationListener {
                override fun onLocationChanged(p0: Location) {
                    if (updatedLocation != null) {
                        updatedLocation = if (updatedLocation!!.accuracy > p0.accuracy) {
                            updatedLocation
                        } else {
                            p0
                        }
                    } else updatedLocation = p0

                    updateCount++
                    if (updatedLocation != null) {

                        locationManager.removeUpdates(this)
                        resultHandler(LatLng(updatedLocation!!.latitude, updatedLocation!!.longitude))
                    }
                }

                override fun onProviderDisabled(p0: String?) {

                }

                override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

                }

                override fun onProviderEnabled(p0: String?) {

                }

            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0.0f, listener)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0.0f, listener)

        }
    }


    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_LOCATION
    }

    override fun refreshInputViewContents(inputView: AAttributeInputView<out Any>) {
        if (inputView is LocationInputView) {
            getAutoCompleteValueAsync {
                result ->
                println("location: $result")
                inputView.value = result
            }
        }
    }
}
