package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.view.View
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.MapImageView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.LocationRetrievalTask
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

    override val propertyKeys: IntArray = intArrayOf()

    override val typeNameResourceId: Int = R.string.type_location_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_location

    override fun createProperties() {

    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_LATITUDE_LONGITUDE

    override fun formatAttributeValue(value: Any): String {
        return value.toString()
    }

    override fun getAutoCompleteValueAsync(resultHandler: (LatLng) -> Unit): Boolean {

        LocationRetrievalTask(resultHandler).execute()

        return false
    }


    override fun getInputViewType(previewMode: Boolean): Int {
        return AAttributeInputView.VIEW_TYPE_LOCATION
    }

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {
        /*
        if (inputView is LocationInputView) {
            getAutoCompleteValueAsync {
                result ->
                println("location: $result")
                inputView.value = result
            }
        }*/
    }

    override fun getViewForItemListContainerType(): Int = VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE

    override fun getViewForItemList(context: Context, recycledView: View?): View {

        val target = if (recycledView is MapImageView) {
            recycledView
        } else {
            MapImageView(context)
        }



        return target
    }

    override fun applyValueToViewForItemList(value: Any?, view: View): Boolean {
        if (view is MapImageView && value != null) {
            if (value is LatLng) {
                view.location = value
                return true
            } else return false
        } else return super.applyValueToViewForItemList(value, view)
    }
}
