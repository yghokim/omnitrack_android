package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import android.view.View
import com.google.android.gms.maps.model.LatLng
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTLocationFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.ui.components.common.LiteMapView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory

class LocationViewFactory(helper: OTLocationFieldHelper) : OTFieldViewFactory<OTLocationFieldHelper>(helper) {

    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int = AFieldInputView.VIEW_TYPE_LOCATION

    //item list===========================================================================
    override fun getViewForItemListContainerType(): Int {
        return OTFieldManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(field: OTFieldDAO, context: Context, recycledView: View?): View {
        return recycledView as? LiteMapView ?: LiteMapView(context)
    }

    override fun applyValueToViewForItemList(context: Context, field: OTFieldDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is LiteMapView && value != null) {
                if (value is LatLng) {
                    view.location = value
                    Single.just(true)
                } else Single.just(false)
            } else super.applyValueToViewForItemList(context, field, value, view)
        }
    }
}