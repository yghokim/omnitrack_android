package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.util.AttributeSet
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 8/3/2016.
 */
class LocationInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<LatLng>(R.layout.input_location, context, attrs) {
    override val typeId: Int = VIEW_TYPE_LOCATION

    override var value: LatLng
        get() = LatLng(0.0, 0.0)
        set(value) {

        }

    override fun focus() {
    }

}