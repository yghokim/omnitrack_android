package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories

import android.content.Context
import android.view.View
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTLocationAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.ui.components.common.LiteMapView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactory

class LocationViewFactory(helper: OTLocationAttributeHelper) : AttributeViewFactory<OTLocationAttributeHelper>(helper) {

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_LOCATION

    //item list===========================================================================
    override fun getViewForItemListContainerType(): Int {
        return OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {
        return recycledView as? LiteMapView ?: LiteMapView(context)
    }

    override fun applyValueToViewForItemList(context: Context, attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is LiteMapView && value != null) {
                if (value is LatLng) {
                    view.location = value
                    Single.just(true)
                } else Single.just(false)
            } else super.applyValueToViewForItemList(context, attribute, value, view)
        }
    }
}