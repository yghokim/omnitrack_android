package kr.ac.snu.hcil.omnitrack.core.event.device

import android.Manifest
import android.content.Context
import com.github.salomonbrys.kotson.toJson
import com.google.gson.JsonObject
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.JsonDictDouble
import kr.ac.snu.hcil.android.common.JsonDictString
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.event.OTEventFactory
import kr.ac.snu.hcil.android.common.getStringCompat

class OTLocationProximityEventFactory(context: Context) : OTEventFactory(context, "location_proximity"){
//https://developer.android.com/training/location/geofencing

    companion object{
        const val KEY_EVENT_TYPE = "type"

        const val EVENT_TYPE_ENTER = "enter"
        const val EVENT_TYPE_DWELL = "dwell"
        const val EVENT_TYPE_EXIT = "exite"

        const val KEY_GEOFENCE_LAT = "lat"
        const val KEY_GEOFENCE_LNG = "lng"
        const val KEY_GEOFENCE_RADIUS_METER = "radius"
    }

    //event type: enter, exit
    //geofences: position, radius

    override fun getCategoryName(): String = "Location"

    override fun makeAttachable(arguments: JsonObject?): OTAttachableEvent {
        return OTLocationProximityEvent(this, arguments)
    }

    override val nameResourceId: Int = R.string.event_location_proximity_name
    override val descResourceId: Int = R.string.event_location_proximity_desc

    override val requiredPermissions: Array<String> = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    class OTLocationProximityEvent(factory: OTLocationProximityEventFactory, arguments: JsonObject?): OTAttachableEvent(factory, arguments){

        var eventType: String? by JsonDictString(arguments, KEY_EVENT_TYPE)
        var geofenceLat: Double? by JsonDictDouble(arguments, KEY_GEOFENCE_LAT)
        var geofenceLng: Double? by JsonDictDouble(arguments, KEY_GEOFENCE_LNG)
        var geofenceRadius: Double? by JsonDictDouble(arguments, KEY_GEOFENCE_RADIUS_METER)


        override fun unsubscribe(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun subscribe(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}