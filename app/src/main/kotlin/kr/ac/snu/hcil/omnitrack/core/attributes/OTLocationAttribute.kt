package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.view.View
import com.google.android.gms.maps.model.LatLng
import io.nlopez.smartlocation.SmartLocation
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.MapImageView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.Single
import rx.subscriptions.Subscriptions

/**
 * Created by Young-Ho on 8/2/2016.
 */
class OTLocationAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: Map<String, Any?>?, connectionData: String?) : OTAttribute<LatLng>(objectId, localKey, parentTracker, columnName, isRequired, TYPE_LOCATION, settingData, connectionData) {

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

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(false, true)

    override val propertyKeys: Array<String> = emptyArray()

    override val typeNameResourceId: Int = R.string.type_location_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_location

    override val isAutoCompleteValueStatic: Boolean = false

    override fun createProperties() {

    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_LATITUDE_LONGITUDE

    override fun formatAttributeValue(value: Any): CharSequence {
        return value.toString()
    }

    override fun getAutoCompleteValue(): Observable<LatLng> {

        return Observable.create {
            subscriber ->
            val sm = SmartLocation.with(OTApplication.app).location().oneFix()
            sm.start {
                location ->

                sm.stop()
                if (!subscriber.isUnsubscribed) {
                    subscriber.onNext(LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0))
                    subscriber.onCompleted()
                }
            }

            subscriber.add(Subscriptions.create { sm.stop() })
        }

        /*
        return ObservableFactory.from(SmartLocation.with(OTApplication.app).location())
                .map {
                    location ->
                    LatLng(location.latitude, location.longitude)
                }
                .onErrorReturn {
                    LatLng(0.0, 0.0)
                }.single()*/
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

        return recycledView as? MapImageView ?: MapImageView(context)
    }

    override fun applyValueToViewForItemList(value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is MapImageView && value != null) {
                if (value is LatLng) {
                    view.location = value
                    Single.just(true)
                } else Single.just(false)
            } else super.applyValueToViewForItemList(value, view)
        }
    }

    override fun onAddValueToTable(value: Any?, out: MutableList<String?>, uniqKey: String?) {
        if(value is LatLng)
        {
            out.add("${ Location.convert(value.latitude, Location.FORMAT_DEGREES)},${Location.convert(value.longitude, Location.FORMAT_DEGREES)}")
        }
        else out.add(null)
    }
}
