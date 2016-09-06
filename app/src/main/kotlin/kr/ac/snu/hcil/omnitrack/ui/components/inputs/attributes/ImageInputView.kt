package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.ImagePicker

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class ImageInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<Uri>(R.layout.input_image, context, attrs) {
    override val typeId: Int = VIEW_TYPE_IMAGE

    override var value: Uri = Uri.EMPTY

    private val picker: ImagePicker

    init {
        picker = findViewById(R.id.ui_image_picker) as ImagePicker
    }

    override fun focus() {

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}