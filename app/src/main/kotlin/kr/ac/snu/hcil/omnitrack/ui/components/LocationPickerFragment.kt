package kr.ac.snu.hcil.omnitrack.ui.components

import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import kr.ac.snu.hcil.omnitrack.R

/**
 * Created by younghokim on 16. 8. 3..
 */
class LocationPickerFragment : Fragment(), OnMapReadyCallback {

    private var mapView: MapView? = null

    private var map: GoogleMap? = null

    private var touchStartedHere = false


    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater!!.inflate(R.layout.fragment_location_picker, container, false)

        mapView = rootView.findViewById(R.id.ui_map) as MapView

        mapView?.onCreate(savedInstanceState)

        mapView?.getMapAsync(this)

        rootView.setOnTouchListener { view, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                touchStartedHere = true
                true
            } else if (motionEvent.action == MotionEvent.ACTION_MOVE) {
                touchStartedHere
            } else false
        }

        return rootView
    }

    override fun onMapReady(p0: GoogleMap) {
        println(p0)
        this.map = p0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

}