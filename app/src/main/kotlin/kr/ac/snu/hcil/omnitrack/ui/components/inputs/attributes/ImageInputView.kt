package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.ImagePicker
import kr.ac.snu.hcil.omnitrack.utils.getActivity

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class ImageInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<Uri>(R.layout.input_image, context, attrs), ImagePicker.ImagePickerCallback {

    companion object{
        const val REQUEST_CODE_CAMERA = 2
        const val REQUEST_CODE_GALLERY = 4
    }

    override val typeId: Int = VIEW_TYPE_IMAGE

    override var value: Uri
        get() = picker.imageUri
        set(value)
        {
            picker.imageUri = value
        }

    private val picker: ImagePicker

    private var cameraCacheUri: Uri? = null

    init {
        picker = findViewById(R.id.ui_image_picker) as ImagePicker
        picker.callback = this

        picker.uriChanged += {
            sender, uri->
            println("picker uri changed to ${uri.toString()}")
            onValueChanged(uri)
        }
    }

    override fun focus() {

    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onRequestCameraImage(view: ImagePicker) {
        val activity = this.getActivity()
        if(activity != null) {
            cameraCacheUri = ImagePicker.dispatchCameraIntent(activity,  makeActivityForResultRequestCode(position, REQUEST_CODE_CAMERA))
        }
    }

    override fun onRequestGalleryImage(view: ImagePicker) {

    }

    override fun setValueFromActivityResult(data: Intent, requestType: Int): Boolean {

        if(requestType == REQUEST_CODE_CAMERA || requestType == REQUEST_CODE_GALLERY)
        {
            if(cameraCacheUri != null) {
                this.picker.imageUri = cameraCacheUri!!
                cameraCacheUri = null
            }
            return true
        }
        else return false
    }

}