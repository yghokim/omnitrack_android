package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.database.CacheHelper

/**
 * Created by Young-Ho Kim on 16. 8. 18
 */
class MapImageView : ImageView, CacheHelper.ICachedBitmapListener /*FutureCallback<ImageView>*/ {

    companion object {

        fun makeGoogleMapQuery(location: LatLng, zoom: Int, width: Int, height: Int): String {
            return "https://maps.googleapis.com/maps/api/staticmap?center=${location.latitude},${location.longitude}&zoom=$zoom&size=${width / 3}x${height / 3}&markers=size:small|${location.latitude},${location.longitude}&scale=2&key=${OTApplication.app.googleApiKey}"
        }
    }

    var location: LatLng = LatLng(0.0, 0.0)
        set(value) {
            if (field != value) {
                field = value
                if (!suspendReloading)
                    reloadMap()
            }
        }

    var zoom: Int = 16
        set(value) {
            if (field != value) {
                field = value
                if (!suspendReloading)
                    reloadMap()
            }
        }

    var aspectRatio: Float = 0.5f
        set(value) {
            if (field != value) {
                field = value
            }
        }

    private var isLoadingMap = false
    private var ongoingZoom: Int = -1
    private var ongoingLocation: LatLng = LatLng(0.0, 0.0)
    private var ongoingWidth: Int = -1
    private var ongoingHeight: Int = -1


    private var reservedReloading = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
    }

    fun set(location: LatLng, zoom: Int) {
        suspendReloading = true
        this.location = location
        this.zoom = zoom
        suspendReloading = false
        reloadMap()
    }

    private var suspendReloading = false

    private fun reloadMap(location: LatLng, zoom: Int) {
        if ((width > 0 && height > 0)) {
            if (isLoadingMap) {
                if (ongoingLocation != location || ongoingZoom != zoom || ongoingWidth != width || ongoingHeight != height)
                    reservedReloading = true
            } else {
                isLoadingMap = true
                ongoingLocation = location
                ongoingZoom = zoom
                ongoingWidth = width
                ongoingHeight = height

                OTApplication.app.cacheHelper.downloadBitmapAsync(this.context,
                        makeGoogleMapQuery(location, zoom, width, height),
                        this)
                /*
            Ion.with(this)
                    .load(makeGoogleMapQuery(location, zoom, width, height)).setCallback(this)*/
            }
        }
    }


    override fun onBitmapRetrieved(uri: Uri?) {
        setImageURI(uri)

        isLoadingMap = false

        ongoingZoom = 0
        ongoingHeight = 0
        ongoingWidth = 0
        ongoingLocation = LatLng(0.0, 0.0)

        if (reservedReloading) {
            reservedReloading = false
            reloadMap()
        }
    }

    /*
    override fun onCompleted(e: Exception?, result: ImageView?) {


        isLoadingMap = false

        if (reservedReloading) {
            reservedReloading = false
            reloadMap()
        }
    }
*/

    fun reloadMap() {
        reloadMap(location, zoom)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            reloadMap()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val originalWidth = MeasureSpec.getSize(widthMeasureSpec)
        //val widthMode = MeasureSpec.getMode(widthMeasureSpec)

        val originalHeight = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)

        val calculatedHeight = (originalWidth * aspectRatio).toInt()

        val finalWidth: Int
        val finalHeight: Int


        finalWidth = originalWidth
        finalHeight = when (heightMode) {
            MeasureSpec.UNSPECIFIED -> calculatedHeight
            MeasureSpec.EXACTLY -> originalHeight
            MeasureSpec.AT_MOST -> Math.min(calculatedHeight, originalHeight)
            else -> calculatedHeight
        }

        setMeasuredDimension(finalWidth, finalHeight)

    }
}