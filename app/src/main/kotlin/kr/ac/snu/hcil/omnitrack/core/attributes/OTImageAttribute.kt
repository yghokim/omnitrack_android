package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import rx.Single
import rx.schedulers.Schedulers

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
        val target = if (recycledView is ImageView) {
            recycledView
        } else {
            val view = ImageView(context)
            view
        }

        target.adjustViewBounds = true
            target.scaleType = ImageView.ScaleType.FIT_CENTER
            target.setBackgroundColor(ContextCompat.getColor(context, R.color.editTextFormBackground))

            val padding = (8 * context.resources.displayMetrics.density).toInt()
            target.setPadding(padding, padding, padding, padding)

            target.minimumHeight = (100 * context.resources.displayMetrics.density).toInt()

        return target
    }

    override fun applyValueToViewForItemList(value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is ImageView) {
                if (value is SynchronizedUri && !value.isEmpty) {
                    view.setImageResource(android.R.drawable.stat_sys_download)
                    if (value.isLocalUriValid) {
                        Glide.with(view.context)
                                .load(value.localUri.toString())
                                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                .into(view)
                        Single.just(true)
                    } else {
                        println("local uri is invalid. download server image.")
                        if (value.isSynchronized) {
                            //OTApplication.app.storageHelper.downloadFileTo(value.serverUri.toString(), value.localUri).onErrorReturn{Uri.EMPTY}
                            OTApplication.app.storageHelper.downloadFileTo(value.serverUri.toString(), value.localUri).onErrorReturn {
                                error ->
                                error.printStackTrace()
                                Uri.EMPTY
                            }
                                    .map {
                                        uri ->
                                        println("downloaded: ${uri}")
                                        Glide.with(view.context)
                                                .load(uri)
                                                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                                .into(view)
                                        true
                                    }.subscribeOn(Schedulers.io())
                        } else {
                            view.setImageResource(0)
                            Single.just(false)
                        }
                    }
                } else {
                    view.setImageResource(0)
                    Single.just(false)
                }
            } else super.applyValueToViewForItemList(null, view)
        }
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SYNCHRONIZED_URI

    override val typeNameResourceId: Int = R.string.type_image_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_image

}