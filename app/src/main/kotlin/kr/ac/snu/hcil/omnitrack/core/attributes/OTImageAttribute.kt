package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.net.Uri
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.PlaceHolderImageView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers
import java.lang.Exception
import java.util.*

/**
 * Created by younghokim on 16. 9. 6..
 */
class OTImageAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: Map<String, Any?>?, connectionData: String?)
    : OTSynchronizedUriAttribute(objectId, localKey, parentTracker, columnName, isRequired, TYPE_IMAGE, settingData, connectionData) {

    override val valueNumericCharacteristics: NumericCharacteristics = NumericCharacteristics(false, false)

    override fun createProperties() {

    }

    override fun formatAttributeValue(value: Any): CharSequence {
        println("formatted image uri : ${value.javaClass}")
        return value.toString()
    }

    override fun getAutoCompleteValue(): Observable<SynchronizedUri> {
        return Observable.just(SynchronizedUri())
    }


    override fun getInputViewType(previewMode: Boolean): Int = AAttributeInputView.VIEW_TYPE_IMAGE

    override val propertyKeys: Array<String> = emptyArray()

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>) {
        if (inputView is ImageInputView) {
            inputView.picker.overrideLocalUriFolderPath = tracker?.getItemCacheDir(inputView.context)
        }
    }

    override fun getViewForItemListContainerType(): Int = VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE

    override fun getViewForItemList(context: Context, recycledView: View?): View {
        val target = if (recycledView is PlaceHolderImageView) {
            recycledView
        } else {
            val view = PlaceHolderImageView(context)
            view
        }

        return target
    }

    override fun applyValueToViewForItemList(value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is PlaceHolderImageView) {
                view.currentMode = PlaceHolderImageView.Mode.EMPTY

                fun function(): Single<Boolean> {
                    if (value is SynchronizedUri && !value.isEmpty) {
                        if (value.isLocalUriValid) {
                            view.currentMode = PlaceHolderImageView.Mode.IMAGE
                            Glide.with(view.context)
                                    .load(value.localUri.toString())
                                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                    .into(view.imageView)
                            return Single.just(true)
                        } else {
                            println("local uri is invalid. download server image.")
                            if (value.isSynchronized) {

                                println("value is synchronized. download server image from firebase ${value.serverUri}")

                                if (!NetworkHelper.isConnectedToInternet()) {
                                    println("internet is not connected.")
                                    view.setErrorMode(OTApplication.app.getString(R.string.msg_network_error_tap_to_retry))
                                    return@function Single.just(false)
                                }

                                view.currentMode = PlaceHolderImageView.Mode.LOADING
                                //OTApplication.app.storageHelper.downloadFileTo(value.serverUri.toString(), value.localUri).onErrorReturn{Uri.EMPTY}
                                return OTApplication.app.storageHelper.downloadFileTo(value.serverUri.toString(), value.localUri).doOnError {
                                    error ->
                                    error.printStackTrace()
                                    view.currentMode = PlaceHolderImageView.Mode.EMPTY
                                }
                                        .map {
                                            uri ->
                                            println("downloaded: ${uri}")
                                            Glide.with(view.context)
                                                    .load(uri)
                                                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                                    .listener(object : RequestListener<Uri, GlideDrawable> {
                                                        override fun onException(e: Exception?, model: Uri?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                                                            view.currentMode = PlaceHolderImageView.Mode.ERROR
                                                            return false
                                                        }

                                                        override fun onResourceReady(resource: GlideDrawable?, model: Uri?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                                                            if (resource != null) {
                                                                view.currentMode = PlaceHolderImageView.Mode.IMAGE
                                                            }
                                                            return false
                                                        }

                                                    }).into(view.imageView)
                                            true
                                        }.subscribeOn(Schedulers.io())
                            } else {
                                println("image is not synchronized. serverUri is empty.")
                                //not synchronized yet.
                                view.setErrorMode(OTApplication.app.getString(R.string.msg_network_error_tap_to_retry))
                                return Single.just(false)
                            }
                        }
                    } else {
                        view.currentMode = PlaceHolderImageView.Mode.EMPTY
                        return Single.just(false)
                    }
                }

                view.onRetryHandler = {
                    function().subscribe()
                }

                function()
            } else super.applyValueToViewForItemList(null, view)
        }
    }

    override fun makeRelativeFilePathFromValue(value: Any?, uniqKey: String?): String {
        return "images/${objectId}_${uniqKey ?: UUID.randomUUID().toString()}_image.jpg"
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SYNCHRONIZED_URI

    override val typeNameResourceId: Int = R.string.type_image_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_image

}