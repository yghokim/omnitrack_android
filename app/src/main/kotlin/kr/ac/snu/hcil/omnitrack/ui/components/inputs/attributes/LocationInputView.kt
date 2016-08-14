package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by Young-Ho on 8/3/2016.
 */
class LocationInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<LatLng>(R.layout.component_location_picker, context, attrs), OnMapReadyCallback {

    override val typeId: Int = VIEW_TYPE_LOCATION

    override var value: LatLng
        get() = LatLng(0.0, 0.0)
        set(value) {

        }

    private val mapView: MapView

    init {
        mapView = findViewById(R.id.ui_map) as MapView


        setOnTouchListener { view, motionEvent ->
            println(motionEvent.action)
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                parent.requestDisallowInterceptTouchEvent(true)
            } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                parent.requestDisallowInterceptTouchEvent(false)
            }
            true
        }
    }

    override fun focus() {
    }

    override fun onMapReady(map: GoogleMap?) {

    }


    override fun onLowMemory() {
        mapView.onLowMemory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        mapView.onSaveInstanceState(outState)
    }

    override fun onResume() {
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
    }


}