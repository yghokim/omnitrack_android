package kr.ac.snu.hcil.omnitrack.core.attributes

import android.content.Context
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.firebase.ui.storage.images.FirebaseImageLoader
import com.google.firebase.storage.FirebaseStorage
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.utils.serialization.TypeStringSerializationHelper
import rx.Observable
import java.lang.Exception

/**
 * Created by younghokim on 16. 9. 6..
 */
class OTImageAttribute(objectId: String?, localKey: Int?, parentTracker: OTTracker?, columnName: String, isRequired: Boolean, settingData: Map<String, Any?>?, connectionData: String?)
    : OTAttribute<SynchronizedUri>(objectId, localKey, parentTracker, columnName, isRequired, TYPE_IMAGE, settingData, connectionData) {

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

    override fun applyValueToViewForItemList(value: Any?, view: View): Boolean {
        if (view is ImageView && value != null) {
            if (value is SynchronizedUri) {
                if (value.primaryUri != Uri.EMPTY) {
                    Glide.with(view.context)
                            .load(value.localUri.toString())
                            .listener(object : RequestListener<String, GlideDrawable> {
                                override fun onResourceReady(resource: GlideDrawable?, model: String?, target: Target<GlideDrawable>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                                    return false
                                }

                                override fun onException(e: Exception, model: String?, target: Target<GlideDrawable>?, isFirstResource: Boolean): Boolean {
                                    if (isFirstResource && e is java.io.FileNotFoundException) {
                                        println("local uri failed. retry with server uri.")
                                        println("server uri: ${value.serverUri}")
                                        if (value.serverUri != Uri.EMPTY) {
                                            val firebaseFileRef = FirebaseStorage.getInstance().reference.child(value.serverUri.toString())
                                            Glide.with(view.context).using(FirebaseImageLoader())
                                                    .load(firebaseFileRef)
                                                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                                    .into(view)
                                        }
                                    }
                                    return false
                                }


                            })
                            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                            .into(view)
                } else {
                    view.setImageURI(Uri.EMPTY)
                }
                return true
            } else return false
        } else return super.applyValueToViewForItemList(value, view)
    }

    override val typeNameForSerialization: String = TypeStringSerializationHelper.TYPENAME_SYNCHRONIZED_URI

    override val typeNameResourceId: Int = R.string.type_image_name

    override val typeSmallIconResourceId: Int = R.drawable.icon_small_image

}