package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.Manifest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTLocationAttribute
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import pl.charmas.android.reactivelocation.ReactiveLocationProvider
import rx.Observable
import java.util.concurrent.TimeUnit

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTLocationAttributeHelper : OTAttributeHelper() {

    private val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(false, true)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int = R.string.type_location_name

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int = R.drawable.icon_small_location

    override fun isIntrinsicDefaultValueVolatile(attribute: OTAttributeDAO): Boolean = true

    override fun getRequiredPermissions(attribute: OTAttributeDAO): Array<String>? = permissions

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_LATITUDE_LONGITUDE

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_LOCATION

    override fun isIntrinsicDefaultValueSupported(attribute: OTAttributeDAO): Boolean {
        return true
    }

    override fun makeIntrinsicDefaultValue(attribute: OTAttributeDAO): Observable<out Any> {
        return Observable.defer {
            val locationProvider = ReactiveLocationProvider(OTApplication.app)

            val request = LocationRequest.create() //standard GMS LocationRequest
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setNumUpdates(OTLocationAttribute.NUM_UPDATES)
                    .setInterval(100)
                    .setMaxWaitTime(500)
                    .setExpirationDuration(2000)

            locationProvider.getUpdatedLocation(request).map { location ->
                LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
            }.timeout(2L, TimeUnit.SECONDS, locationProvider.lastKnownLocation.map { location -> LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0) }).first()
        }
    }

    override fun makeIntrinsicDefaultValueMessage(attribute: OTAttributeDAO): CharSequence {
        return OTApplication.getString(R.string.msg_intrinsic_location)
    }
}