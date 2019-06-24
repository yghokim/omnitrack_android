package kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.factories

import android.content.Context
import android.view.View
import com.squareup.picasso.Picasso
import io.reactivex.Single
import kr.ac.snu.hcil.android.common.view.image.PlaceHolderImageView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.fields.OTFieldManager
import kr.ac.snu.hcil.omnitrack.core.fields.helpers.OTImageFieldHelper
import kr.ac.snu.hcil.omnitrack.core.database.models.OTFieldDAO
import kr.ac.snu.hcil.omnitrack.core.types.OTServerFile
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.AFieldInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.OTFieldViewFactory
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.fields.ImageInputView
import org.jetbrains.anko.runOnUiThread

class ImageViewFactory(helper: OTImageFieldHelper) : OTFieldViewFactory<OTImageFieldHelper>(helper) {


    override fun getInputViewType(previewMode: Boolean, field: OTFieldDAO): Int = AFieldInputView.VIEW_TYPE_IMAGE

    override fun refreshInputViewUI(inputView: AFieldInputView<out Any>, field: OTFieldDAO) {
        if (inputView is ImageInputView) {
            inputView.picker.overrideLocalUriFolderPath = helper.localCacheManager.getDefaultItemCacheDir(field.trackerId
                    ?: "images", true)
        }
    }

    //item list========================================================

    override fun getViewForItemListContainerType(): Int {
        return OTFieldManager.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(field: OTFieldDAO, context: Context, recycledView: View?): View {

        return if (recycledView is PlaceHolderImageView) {
            recycledView
        } else {
            val view = PlaceHolderImageView(context)
            view
        }
    }

    override fun applyValueToViewForItemList(context: Context, field: OTFieldDAO, value: Any?, view: View): Single<Boolean> {
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
            } else super.applyValueToViewForItemList(context, field, null, view)
        }
    }
}