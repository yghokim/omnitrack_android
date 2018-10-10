package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.patloew.rxlocation.RxLocation
import io.reactivex.BackpressureStrategy
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.component_location_picker.view.*
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.Nullable
import kr.ac.snu.hcil.omnitrack.utils.contains
import kr.ac.snu.hcil.omnitrack.utils.getAddress
import java.util.concurrent.TimeUnit

/**
 * Created by Young-Ho on 8/3/2016.
 *
 * https://github.com/googlesamples/android-play-places/blob/master/PlaceCompleteFragment/Application/src/main/java/com/example/google/playservices/placecompletefragment/MainActivity.java
 *
 */
class LocationInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<LatLng>(R.layout.component_location_picker, context, attrs), OnMapReadyCallback, View.OnClickListener, GoogleMap.OnCameraIdleListener {

    companion object {
        const val REQUEST_TYPE_GOOGLE_PLACE_PICKER = 2
    }

    override val typeId: Int = VIEW_TYPE_LOCATION

    private var _value: LatLng? = LatLng(0.0, 0.0)
    override var value: LatLng?
        get() = _value
        set(value) {
            setValue(value, false)
        }

    fun setValue(value: LatLng?, animate: Boolean) {
        if (_value != value) {
            _value = value
            if (value != null) {
                fitToValueLocation(animate)
            }

            onValueChanged(value)
            //addressView.text = value.getAddress(context)?.getAddressLine(0)
            reserveAddressChange(value)
        }
    }

    private var isAdjustMode: Boolean = false
        set(value) {
            if (field != value) {
                field = value

                if (value) // adjustMode
                {
                    controlPanelViews.forEach { it.visibility = View.GONE }

                    if (adjustPanel == null) {
                        adjustPanel = inflateAdjustPanel()
                    } else {
                        adjustPanel?.visibility = View.VISIBLE
                    }

                    colorFrame.setBackgroundResource(R.drawable.map_view_frame_adjust_mode)


                } else {
                    controlPanelViews.forEach { it.visibility = View.VISIBLE }

                    if (adjustPanel != null) {
                        adjustPanel?.visibility = View.GONE
                    }

                    colorFrame.setBackgroundResource(R.drawable.map_view_frame)
                }

                refreshMap()
            }
        }


    private val mapView: MapView = findViewById(R.id.ui_map)

    private val zoomInButton: View
    private val zoomOutButton: View
    private val adjustButton: View

    private val fitButton: View

    private val addressView: TextView = findViewById(R.id.ui_address)

    private val colorFrame: View = findViewById(R.id.ui_mapview_frame)

    private val addressBusyIndicator: ProgressBar = findViewById(R.id.ui_address_busy_indicator)

    private val adjustPanelStub: ViewStub = findViewById(R.id.ui_adjust_panel_stub)

    private var adjustPanel: View? = null

    private var adjustOkButton: View? = null
    private var adjustCancelButton: View? = null

    private var googleMap: GoogleMap? = null

    private var valueMarker: Marker? = null

    private val addressConversionTaskSubscription = SerialDisposable()
    private val myLocationTaskSubscription = SerialDisposable()

    private val controlPanelViews = ArrayList<View>()

    init {

        addressBusyIndicator.visibility = View.INVISIBLE

        zoomInButton = findViewById(R.id.ui_button_zoom_in)
        zoomOutButton = findViewById(R.id.ui_button_zoom_out)
        fitButton = findViewById(R.id.ui_button_fit)
        adjustButton = findViewById(R.id.ui_button_adjust)

        //searchButton = findViewById(R.id.ui_button_search)

        zoomInButton.setOnClickListener(this)
        zoomOutButton.setOnClickListener(this)
        fitButton.setOnClickListener(this)
        adjustButton.setOnClickListener(this)

        ui_button_my_location.setOnClickListener {
            setToMyLocation(true)
        }

        controlPanelViews.addAll(
                arrayOf(
                        zoomInButton,
                        zoomOutButton,
                        fitButton,
                        adjustButton,
                        ui_button_my_location
                )
        )
    }

    private fun setToMyLocation(animate: Boolean) {
        myLocationTaskSubscription.set(
                Single.defer {
                    val rxLocation = RxLocation(context)
                    val request = LocationRequest.create() //standard GMS LocationRequest
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setNumUpdates(2)
                            .setInterval(100)
                            .setMaxWaitTime(500)
                            .setExpirationDuration(2000)

                    try {
                        return@defer rxLocation.location().updates(request, BackpressureStrategy.LATEST)
                                .map { location -> LatLng(location.latitude, location.longitude) }
                                .timeout(2L, TimeUnit.SECONDS, rxLocation.location().lastLocation().map { location -> LatLng(location.latitude, location.longitude) }.toFlowable())
                                .firstOrError()
                                .map { loc -> Nullable(loc) }
                    } catch (ex: SecurityException) {
                        ex.printStackTrace()
                        return@defer Single.just(Nullable(LatLng(0.0, 0.0)))
                    } catch (ex: Exception) {
                        println("get my location global error")
                        ex.printStackTrace()
                        return@defer Single.just(Nullable(LatLng(0.0, 0.0)))
                    }
                }.subscribe({ (location) ->
                    setValue(location, animate)
                }, { err ->
                    println("get my location unhandled error")
                    err.printStackTrace()
                })
        )
    }

    private fun inflateAdjustPanel(): View {
        val view = adjustPanelStub.inflate()

        adjustOkButton = view.findViewById(R.id.ui_button_apply)
        adjustCancelButton = view.findViewById(R.id.ui_button_cancel)

        adjustOkButton?.setOnClickListener(this)
        adjustCancelButton?.setOnClickListener(this)

        return view
    }

    override fun onSetPreviewMode(mode: Boolean) {
        super.onSetPreviewMode(mode)
        if (mode) {
            setToMyLocation(false)

        }
    }

    override fun onClick(view: View) {
        if (view === zoomInButton) {
            googleMap?.animateCamera(CameraUpdateFactory.zoomBy(1.0f))
        } else if (view === zoomOutButton) {
            googleMap?.animateCamera(CameraUpdateFactory.zoomBy(-1.0f))
        } else if (view === fitButton) {
            fitToValueLocation(animate = true)
        } else if (view === adjustButton) {
            isAdjustMode = true
        } else if (view === adjustOkButton) {

            if (googleMap != null)
                value = googleMap!!.cameraPosition.target

            isAdjustMode = false

        } else if (view === adjustCancelButton) {
            isAdjustMode = false
            reserveAddressChange(value)
            fitToValueLocation(animate = true)
        }

        /*else if (view === searchButton) {
            val activity = getActivity()
            if (activity != null) {
                if (position >= 0) {
                    val intent = PlacePicker.IntentBuilder().build(activity)
                    activity.startActivityForResult(intent, makeActivityForResultRequestCode(position, REQUEST_TYPE_GOOGLE_PLACE_PICKER))
                }
            }
            //getActivity()?.startActivityForResult()

        }*/
    }

    private fun fitToValueLocation(animate: Boolean) {
        if (googleMap != null && value != null) {

            val update = CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                    .target(value)
                    .zoom(14.0f)
                    .bearing(0f)
                    .build())

            if (animate) {
                googleMap?.animateCamera(update)
            } else {
                googleMap?.moveCamera(update)
            }

            if (valueMarker == null) {
                valueMarker = googleMap?.addMarker(MarkerOptions().position(value!!).draggable(false))
            } else {
                valueMarker?.position = value
            }
        }
    }

    private fun refreshMap() {
        if (isAdjustMode) {
            valueMarker?.alpha = 0.5f
        } else {
            valueMarker?.alpha = 1.0f
        }
    }

    override fun onCameraIdle() {
        if (isAdjustMode) {
            reserveAddressChange(googleMap!!.cameraPosition.target)
        }
    }


    private fun reserveAddressChange(location: LatLng?) {
        if (location != null) {
            addressBusyIndicator.visibility = View.VISIBLE
            addressConversionTaskSubscription.set(
                    Single.create<Nullable<String>> { subscriber ->

                        val googleAddress = location.getAddress(context)

                        if (!subscriber.isDisposed) {
                            subscriber.onSuccess(
                                    Nullable(googleAddress?.getAddressLine(googleAddress.maxAddressLineIndex))
                            )
                        }
                    }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io()).subscribe { (address) ->
                        addressBusyIndicator.visibility = View.INVISIBLE
                        if (address != null) {
                            addressView.text = address
                        }
                    }
            )
        } else {
            addressConversionTaskSubscription.set(null)
            addressBusyIndicator.visibility = View.INVISIBLE
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

    override fun onMapReady(map: GoogleMap) {
        this.googleMap = map
        map.setMinZoomPreference(3.0f)
        map.setMaxZoomPreference(17.0f)

        map.uiSettings.isMyLocationButtonEnabled = false

        try {
            map.isMyLocationEnabled = true
        } catch (ex: SecurityException) {
            ex.printStackTrace()
        }

        map.setOnCameraIdleListener(this)

        fitToValueLocation(false)
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

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onResume() {
        println("resume map view")
        mapView.onResume()
    }

    override fun onPause() {
        println("pause map view")
        mapView.onPause()
    }

    override fun onDestroy() {
        println("destroy google map")
        googleMap?.clear()
        addressConversionTaskSubscription.set(null)
        myLocationTaskSubscription.set(null)
        try {
            mapView.onDestroy()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}