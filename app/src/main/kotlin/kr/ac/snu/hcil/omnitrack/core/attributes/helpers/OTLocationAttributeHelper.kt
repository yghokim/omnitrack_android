package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.Manifest
import android.content.Context
import android.view.View
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.MapImageView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper

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

    /* TODO switch to Location library usign RxJava2
    override fun makeIntrinsicDefaultValue(attribute: OTAttributeDAO): Single<out Any> {
        return Single.defer {
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
    }*/

    override fun makeIntrinsicDefaultValueMessage(attribute: OTAttributeDAO): CharSequence {
        return OTApplication.getString(R.string.msg_intrinsic_location)
    }

    override fun initialize(attribute: OTAttributeDAO) {
        attribute.fallbackValuePolicy = OTAttributeDAO.DEFAULT_VALUE_POLICY_FILL_WITH_INTRINSIC_VALUE
    }

    //item list===========================================================================
    override fun getViewForItemListContainerType(): Int {
        return OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {
        return recycledView as? MapImageView ?: MapImageView(context)
    }

    override fun applyValueToViewForItemList(attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is MapImageView && value != null) {
                if (value is LatLng) {
                    view.location = value
                    Single.just(true)
                } else Single.just(false)
            } else super.applyValueToViewForItemList(attribute, value, view)
        }
    }
}