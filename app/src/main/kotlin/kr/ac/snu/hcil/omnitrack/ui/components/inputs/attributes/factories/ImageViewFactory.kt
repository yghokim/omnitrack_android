package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.factories

import android.content.Context
import android.view.View
import com.squareup.picasso.Picasso
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.view.image.PlaceHolderImageView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager
import kr.ac.snu.hcil.omnitrack.core.attributes.helpers.OTImageAttributeHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.types.OTServerFile
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AttributeViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import org.jetbrains.anko.runOnUiThread

class ImageViewFactory(helper: OTImageAttributeHelper) : AttributeViewFactory<OTImageAttributeHelper>(helper) {


    override fun getInputViewType(previewMode: Boolean, attribute: OTAttributeDAO): Int = AAttributeInputView.VIEW_TYPE_IMAGE

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is ImageInputView) {
            inputView.picker.overrideLocalUriFolderPath = helper.localCacheManager.getDefaultItemCacheDir(attribute.trackerId
                    ?: "images", true)
        }
    }

    //item list========================================================

    override fun getViewForItemListContainerType(): Int {
        return OTAttributeManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {

        return if (recycledView is PlaceHolderImageView) {
            recycledView
        } else {
            val view = PlaceHolderImageView(context)
            view
        }
    }

    override fun applyValueToViewForItemList(context: Context, attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is PlaceHolderImageView) {
                view.currentMode = PlaceHolderImageView.Mode.EMPTY

                fun function(): Single<Boolean> {
                    if (value is OTServerFile) {
                        return helper.localCacheManager.getCachedUri(value).doOnSubscribe {
                            view.context.runOnUiThread {
                                view.currentMode = PlaceHolderImageView.Mode.LOADING
                            }
                        }.doOnError { error ->
                            error.printStackTrace()
                            view.context.runOnUiThread {
                                view.currentMode = PlaceHolderImageView.Mode.ERROR
                            }
                        }.doOnSuccess { (refreshed, localUri) ->
                            view.context.runOnUiThread {
                                view.currentMode = PlaceHolderImageView.Mode.IMAGE
                                Picasso.get().load(localUri)
                                        .error(R.drawable.img_error)
                                        .into(view.imageView)
                            }
                        }.map { true }
                    } else return Single.defer {
                        view.currentMode = PlaceHolderImageView.Mode.EMPTY
                        Single.just(false)
                    }
                }

                view.onRetryHandler = {
                    function().subscribe()
                }

                function()
            } else super.applyValueToViewForItemList(context, attribute, null, view)
        }
    }
}