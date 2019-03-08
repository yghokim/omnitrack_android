package kr.ac.snu.hcil.omnitrack.core.attributes.helpers

import android.Manifest
import android.content.Context
import android.view.View
import com.squareup.picasso.Picasso
import io.reactivex.Single
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.attributes.OTAttributeManager.Companion.VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
import kr.ac.snu.hcil.omnitrack.core.database.models.OTAttributeDAO
import kr.ac.snu.hcil.omnitrack.core.datatypes.OTServerFile
import kr.ac.snu.hcil.omnitrack.statistics.NumericCharacteristics
import kr.ac.snu.hcil.omnitrack.ui.components.common.PlaceHolderImageView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.AAttributeInputView
import kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes.ImageInputView
import org.jetbrains.anko.runOnUiThread
import java.util.*

/**
 * Created by Young-Ho on 10/7/2017.
 */
class OTImageAttributeHelper(context: Context) : OTFileInvolvedAttributeHelper(context) {

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

    override fun getRequiredPermissions(attribute: OTAttributeDAO?): Array<String>? = permissions

    override fun refreshInputViewUI(inputView: AAttributeInputView<out Any>, attribute: OTAttributeDAO) {
        if (inputView is ImageInputView) {
            inputView.picker.overrideLocalUriFolderPath = localCacheManager.getDefaultItemCacheDir(attribute.trackerId ?: "images", true)
        }
    }

    //item list========================================================

    override fun getViewForItemListContainerType(): Int {
        return VIEW_FOR_ITEM_LIST_CONTAINER_TYPE_MULTILINE
    }

    override fun getViewForItemList(attribute: OTAttributeDAO, context: Context, recycledView: View?): View {

        return if (recycledView is PlaceHolderImageView) {
            recycledView
        } else {
            val view = PlaceHolderImageView(context)
            view
        }
    }

    override fun applyValueToViewForItemList(attribute: OTAttributeDAO, value: Any?, view: View): Single<Boolean> {
        return Single.defer {
            if (view is PlaceHolderImageView) {
                view.currentMode = PlaceHolderImageView.Mode.EMPTY

                fun function(): Single<Boolean> {
                    if (value is OTServerFile) {
                        return localCacheManager.getCachedUri(value).doOnSubscribe {
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
            } else super.applyValueToViewForItemList(attribute, null, view)
        }
    }

    override fun makeRelativeFilePathFromValue(attribute: OTAttributeDAO, value: Any?, uniqKey: String?): String {
        return "images/image_${attribute.localId}_${uniqKey ?: UUID.randomUUID().toString()}.jpg"
    }

}