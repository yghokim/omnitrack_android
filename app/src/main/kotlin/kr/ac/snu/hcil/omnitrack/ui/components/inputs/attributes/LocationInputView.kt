package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.contains

/**
 * Created by Young-Ho on 8/3/2016.
 *
 * https://github.com/googlesamples/android-play-places/blob/master/PlaceCompleteFragment/Application/src/main/java/com/example/google/playservices/placecompletefragment/MainActivity.java
 *
 */
class LocationInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<LatLng>(R.layout.component_location_picker, context, attrs), OnMapReadyCallback, View.OnClickListener {

    override val typeId: Int = VIEW_TYPE_LOCATION

    override var value: LatLng = LatLng(0.0, 0.0)
        set(value) {
            field = value
            fitToValueLocation()
        }

    private val mapView: MapView

    private val controlPanel: View

    private var isAdjustMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value

                if (value == true) // adjustMode
                {
                    controlPanel.visibility = View.GONE

                    if (adjustPanel == null) {
                        adjustPanel = inflateAdjustPanel()
                    } else {
                        adjustPanel?.visibility = View.VISIBLE
                    }

                    colorFrame.setBackgroundResource(R.drawable.map_view_frame_adjust_mode)

                } else {
                    controlPanel.visibility = View.VISIBLE

                    if (adjustPanel != null) {
                        adjustPanel?.visibility = View.GONE
                    }

                    colorFrame.setBackgroundResource(R.drawable.map_view_frame)
                }
            }
        }

    private val zoomInButton: View
    private val zoomOutButton: View
    private val adjustButton: View

    private val fitButton: View

    private val colorFrame: View

    private val adjustPanelStub: ViewStub

    private var adjustPanel: View? = null

    private var adjustOkButton: View? = null
    private var adjustCancelButton: View? = null


    private var googleMap: GoogleMap? = null


    private var marker: Marker? = null

    init {
        mapView = findViewById(R.id.ui_map) as MapView

        colorFrame = findViewById(R.id.ui_mapview_frame)

        controlPanel = findViewById(R.id.ui_control_panel)
        adjustPanelStub = findViewById(R.id.ui_adjust_panel_stub) as ViewStub

        zoomInButton = findViewById(R.id.ui_button_zoom_in)
        zoomOutButton = findViewById(R.id.ui_button_zoom_out)
        fitButton = findViewById(R.id.ui_button_fit)
        adjustButton = findViewById(R.id.ui_button_adjust)

        zoomInButton.setOnClickListener(this)
        zoomOutButton.setOnClickListener(this)
        fitButton.setOnClickListener(this)
        adjustButton.setOnClickListener(this)
    }

    private fun inflateAdjustPanel(): View {
        val view = adjustPanelStub.inflate()

        adjustOkButton = view.findViewById(R.id.ui_button_apply)
        adjustCancelButton = view.findViewById(R.id.ui_button_cancel)

        adjustOkButton?.setOnClickListener(this)
        adjustCancelButton?.setOnClickListener(this)

        return view
    }

    override fun onClick(view: View) {
        if (view === zoomInButton) {
            googleMap?.animateCamera(CameraUpdateFactory.zoomBy(1.0f))
        } else if (view === zoomOutButton) {
            googleMap?.animateCamera(CameraUpdateFactory.zoomBy(-1.0f))
        } else if (view === fitButton) {
            fitToValueLocation()
        } else if (view === adjustButton) {
            isAdjustMode = true
        } else if (view === adjustOkButton) {
            isAdjustMode = false
        } else if (view === adjustCancelButton) {
            isAdjustMode = false
        }
    }

    private fun fitToValueLocation() {
        if (googleMap != null) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(value, 14.0f));

            if (marker == null) {
                googleMap?.addMarker(MarkerOptions().position(value).draggable(false))
            } else {
                marker?.position = value
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {

        //find parent recyclerview
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (mapView.contains(ev.x, ev.y)) {
                parent.requestDisallowInterceptTouchEvent(true)
            }
        } else if (ev.action == MotionEvent.ACTION_CANCEL || ev.action == MotionEvent.ACTION_UP) {
            parent.requestDisallowInterceptTouchEvent(false)
        }

        return super.dispatchTouchEvent(ev)
    }

    override fun focus() {
    }

    override fun onMapReady(map: GoogleMap?) {
        this.googleMap = map
        this.googleMap?.setMinZoomPreference(3.0f)
        this.googleMap?.setMaxZoomPreference(17.0f)

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