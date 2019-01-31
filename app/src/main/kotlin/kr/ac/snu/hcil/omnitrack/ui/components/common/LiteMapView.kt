package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.component_lite_mapview.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.IActivityLifeCycle
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 2017-11-29.
 */
class LiteMapView : ConstraintLayout, IActivityLifeCycle, OnMapReadyCallback {
    var location: LatLng? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null) {
                    if (map != null) {
                        onSetLocation(value)
                    }
                }
            }
        }

    private var map: GoogleMap? = null
    private var marker: Marker? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        inflateContent(R.layout.component_lite_mapview, true)
        ui_map.getMapAsync(this)
    }


    override fun onMapReady(map: GoogleMap) {
        this.map = map
        map.setOnMapClickListener {
            this@LiteMapView.performClick()
        }
        map.uiSettings.setAllGesturesEnabled(false)
        map.uiSettings.isMapToolbarEnabled = false
        location?.let { onSetLocation(it) }

    }

    private fun onSetLocation(loc: LatLng) {
        val update = CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                .target(loc)
                .zoom(17f)
                .bearing(0f)
                .build())

        map?.moveCamera(update)

        if (marker == null) {
            marker = map?.addMarker(MarkerOptions().position(loc).draggable(false))
        } else {
            marker?.position = loc
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ui_map.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        ui_map.onSaveInstanceState(outState)
    }

    override fun onResume() {
        ui_map.onResume()
    }

    override fun onPause() {
        ui_map.onPause()
    }

    override fun onDestroy() {
        ui_map.onDestroy()
    }

    override fun onLowMemory() {
        ui_map.onLowMemory()
    }


    override fun onStart() {
        ui_map.onStart()
    }

    override fun onStop() {
        ui_map.onStop()
    }

}