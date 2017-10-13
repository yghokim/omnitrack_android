package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.Manifest
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.core.database.local.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.PlaceHolderImageView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import kr.ac.snu.hcil.omnitrack.utils.net.NetworkHelper
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Single
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTImageAttributeHelper : OTFileInvolvedAttributeHelper() {

    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_IMAGE

    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun getValueNumericCharacteristics(attribute: OTAttributeDAO): NumericCharacteristics = NumericCharacteristics(false, false)

    override fun getTypeNameResourceId(attribute: OTAttributeDAO): Int {
        return R.string.type_image_name
    }

    override fun getTypeSmallIconResourceId(attribute: OTAttributeDAO): Int {
        return R.drawable.icon_small_image
    }

    override fun isExternalFile(attribute: OTAttributeDAO): Boolean {
        return true
    }

    override fun getRequiredPermissions(attribute: OTAttributeDAO): Array<String>? = permissions

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SYNCHRONIZED_URI

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is ImageInputView) {
            OTApplication.app.databaseManager.getUnManagedTrackerDao(attribute.trackerId, null)?.let {
                inputView.picker.overrideLocalUriFolderPath = it.getItemCacheDir(inputView.context, true)
            }
        }
    }

    //item list========================================================

    override fun getViewForItemListContainerType(): Int {
        return VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {
        val target = if (recycledView is PlaceHolderImageView) {
            recycledView
        } else {
            val view = PlaceHolderImageView(context)
            view
        }

        return target
    }

    override fun applyValueToViewForItemList(attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is PlaceHolderImageView) {
                view.currentMode = PlaceHolderImageView.Mode.EMPTY

                fun function(): Single<Boolean> {
                    if (value is SynchronizedUri && !value.isEmpty) {
                        if (value.isLocalUriValid) {
                            view.currentMode = PlaceHolderImageView.Mode.IMAGE
                            Glide.with(view.context)
                                    .load(value.localUri.toString())
                                    .apply(RequestOptions().override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
                                    .into(view.imageView)
                            return Single.just(true)
                        } else {
                            println("local uri is invalid. download server image.")
                            if (value.isSynchronized) {

                                println("value is synchronized. download server image from firebase ${value.serverUri}")

                                if (!NetworkHelper.isConnectedToInternet()) {
                                    println("internet is not connected.")
                                    view.setErrorMode(OTApplication.getString(R.string.msg_network_error_tap_to_retry))
                                    return@function Single.just(false)
                                }

                                view.currentMode = PlaceHolderImageView.Mode.LOADING
                                //OTApplication.app.storageHelper.downloadFileTo(value.serverUri.toString(), value.localUri).onErrorReturn{Uri.EMPTY}
                                return OTApplication.app.storageHelper.downloadFileTo(value.serverUri.toString(), value.localUri).doOnError { error ->
                                    error.printStackTrace()
                                    view.currentMode = PlaceHolderImageView.Mode.EMPTY
                                }
                                        .map { uri ->
                                            println("downloaded: ${uri}")
                                            Glide.with(view.context)
                                                    .load(uri)
                                                    .apply(RequestOptions.overrideOf(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL))
                                                    .listener(object : RequestListener<Drawable> {
                                                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                                            if (resource != null) {
                                                                view.currentMode = PlaceHolderImageView.Mode.IMAGE
                                                            }
                                                            return false
                                                        }

                                                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                                            view.currentMode = PlaceHolderImageView.Mode.ERROR
                                                            return false
                                                        }

                                                    }).into(view.imageView)
                                            true
                                        }.subscribeOn(Schedulers.io())
                            } else {
                                println("image is not synchronized. serverUri is empty.")
                                //not synchronized yet.
                                view.setErrorMode(OTApplication.getString(R.string.msg_network_error_tap_to_retry))
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
            } else super.applyValueToViewForItemList(attribute, null, view)
        }
    }

    override fun makeRelativeFilePathFromValue(attribute: OTAttributeDAO, value: Any?, uniqKey: String?): String {
        return "images/image_${attribute.localId}_${uniqKey ?: UUID.randomUUID().toString()}.jpg"
    }

}