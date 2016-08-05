package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.ui.components.LocationPickerFragment

/**
 * Created by Young-Ho on 8/3/2016.
 */
class LocationInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<LatLng>(0, context, attrs) {
    override val typeId: Int = VIEW_TYPE_LOCATION

    override var value: LatLng
        get() = LatLng(0.0, 0.0)
        set(value) {

        }

    init {
        //val layout = findViewById(R.id.ui_container)
        val layout = FrameLayout(context)
        layout.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layout.id = View.generateViewId()
        val fragment = LocationPickerFragment()
        (context as Activity).fragmentManager.beginTransaction()
                .replace(layout.id, fragment)
                .commit()

        addView(layout)
    }

    override fun focus() {
    }


}