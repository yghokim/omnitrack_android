package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.model.LatLng
import kr.ac.snu.hcil.omnitrack.OTApp

/**
 * Created by Young-Ho Kim on 16. 8. 18
 */
class MapImageView : PlaceHolderImageView /*FutureCallback<ImageView>*/ {

    companion object {

        fun makeGoogleMapQuery(location: LatLng, zoom: Int, width: Int, height: Int): String {
            return "https://maps.googleapis.com/maps/api/staticmap?center=${location.latitude},${location.longitude}&zoom=$zoom&size=${width / 3}x${height / 3}&markers=size:small|${location.latitude},${location.longitude}&scale=2&key=${OTApp.instance.googleApiKey}"
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
        if ((width > 0)) {
            val desiredHeight = (width * aspectRatio).toInt()
            if (isLoadingMap) {
                if (ongoingLocation != location || ongoingZoom != zoom || ongoingWidth != width || ongoingHeight != desiredHeight)
                    reservedReloading = true
            } else {
                isLoadingMap = true
                ongoingLocation = location
                ongoingZoom = zoom
                ongoingWidth = width
                ongoingHeight = desiredHeight


                Glide.with(context).load(makeGoogleMapQuery(location, zoom, width, desiredHeight))
                        .apply(
                                RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.RESOURCE)
                                        .dontAnimate()
                        )
                        .listener(object : RequestListener<Drawable> {
                            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                handler?.post {
                                    if (resource != null) {
                                        currentMode = PlaceHolderImageView.Mode.IMAGE
                                        imageView.setImageDrawable(resource)
                                    }
                                    onBitmapRetrieved()
                                }
                                return true
                            }

                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                handler?.post {
                                    currentMode = PlaceHolderImageView.Mode.ERROR
                                    onBitmapRetrieved()
                                }
                                return false
                            }

                        }).into(imageView)
                /*
            Ion.with(this)
                    .load(makeGoogleMapQuery(location, zoom, width, height)).setCallback(this)*/
            }
        }
    }


    fun onBitmapRetrieved() {
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

    /*
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

    }*/
}