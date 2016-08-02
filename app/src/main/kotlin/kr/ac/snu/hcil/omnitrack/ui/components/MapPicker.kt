package kr.ac.snu.hcil.omnitrack.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.google.android.gms.maps.MapView
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 8/3/2016.
 */
class MapPicker(context: Context, attrs: AttributeSet? = null) : RelativeLayout(context, attrs) {

    private lateinit var mapView: MapView

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_location_picker, this, true)

    }
}