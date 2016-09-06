package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.animation.LayoutTransition
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.inflateContent

/**
 * Created by younghokim on 16. 9. 6..
 */
class ImagePicker : FrameLayout, View.OnClickListener {

    var imageUri: Uri = Uri.EMPTY
        set(value) {
            if (field != value) {
                field = value

                imageView.setImageURI(value)
                if (value == Uri.EMPTY) {
                    removeButton.visibility = View.INVISIBLE
                    buttonGroup.visibility = View.VISIBLE
                } else {
                    removeButton.visibility = View.VISIBLE
                    buttonGroup.visibility = View.INVISIBLE
                }
            }
        }

    private val cameraButton: View
    private val galleryButton: View
    private val removeButton: View

    private val buttonGroup: ViewGroup
    private val imageView: ImageView


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        val view = inflateContent(R.layout.component_image_picker, false) as ViewGroup
        view.layoutTransition = LayoutTransition()
        addView(view)

        cameraButton = findViewById(R.id.ui_button_camera)
        galleryButton = findViewById(R.id.ui_button_gallery)
        removeButton = findViewById(R.id.ui_button_cancel)
        buttonGroup = findViewById(R.id.ui_button_container) as ViewGroup
        imageView = findViewById(R.id.ui_image_view) as ImageView

        cameraButton.setOnClickListener(this)
        galleryButton.setOnClickListener(this)
        removeButton.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        if (view === cameraButton) {

        } else if (view === galleryButton) {

        } else if (view === removeButton) {
            imageUri = Uri.EMPTY
        }
    }

}