package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import gun0912.tedbottompicker.TedBottomPicker
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.ui.components.common.ImagePicker
import kr.ac.snu.hcil.omnitrack.utils.getActivity

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class ImageInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<SynchronizedUri>(R.layout.input_image, context, attrs), ImagePicker.ImagePickerCallback {

    companion object{
        const val REQUEST_CODE_CAMERA = 2
        const val REQUEST_CODE_GALLERY = 4

        const val IMAGE_MAX_PIXELS = 720 * 1280

    }

    override val typeId: Int = VIEW_TYPE_IMAGE

    override var value: SynchronizedUri = SynchronizedUri()
        set(value)
        {
            if (field != value) {
                field = value
                picker.imageUri = field.primaryUri
            }
        }

    private val picker: ImagePicker = findViewById(R.id.ui_image_picker) as ImagePicker

    private var cameraCacheUri: Uri? = null

    init {
        picker.callback = this

        picker.uriChanged += {
            sender, uri->
            println("picker uri changed to ${uri.toString()}")
            value = SynchronizedUri(uri)
            onValueChanged(value)
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
        val activity = this.getActivity()
        if (activity != null) {
            val pickerBottomSheetBuilder = TedBottomPicker.Builder(activity.applicationContext)

                    .setSelectMaxCount(1)
                    .showCameraTile(false)
                    .showGalleryTile(true)

            pickerBottomSheetBuilder.setOnImageSelectedListener(object : TedBottomPicker.OnImageSelectedListener {
                override fun onImageSelected(uri: Uri) {

                    val resizedUri = Uri.fromFile(ImagePicker.createCacheImageFile(context))
                    resizeImage(uri, resizedUri)
                    this@ImageInputView.picker.imageUri = resizedUri
                    pickerBottomSheetBuilder.setOnImageSelectedListener(null)
                }
            })

            pickerBottomSheetBuilder.create().show(activity.supportFragmentManager)
            //ImagePicker.dispatchImagePickIntent(activity, makeActivityForResultRequestCode(position, REQUEST_CODE_GALLERY))
        }
    }

    fun resizeImage(source: Uri, dest: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, source)
        println("loaded bitmap original size: ${bitmap.width} X ${bitmap.height}")


        val numPixels = bitmap.width * bitmap.height
        val scale = Math.sqrt(IMAGE_MAX_PIXELS / numPixels.toDouble()).toFloat()

        if (scale < 1) {
            println("scale down camera image : ${scale * 100}%")

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale + 0.5f).toInt(), (bitmap.height * scale + 0.5f).toInt(), true)
            //val scaledBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
            bitmap.recycle()
            val outputStream = context.contentResolver.openOutputStream(dest)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            scaledBitmap.recycle()
        } else {
            val outputStream = context.contentResolver.openOutputStream(dest)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            bitmap.recycle()
        }
    }

    override fun setValueFromActivityResult(data: Intent, requestType: Int): Boolean {

        if (requestType == REQUEST_CODE_CAMERA)
        {
            if(cameraCacheUri != null) {

                resizeImage(cameraCacheUri!!, cameraCacheUri!!)

                this.picker.imageUri = cameraCacheUri!!
                cameraCacheUri = null
            }
            return true
        } else if (requestType == REQUEST_CODE_GALLERY) {
            if (data.data != null) {
                val uri = Uri.fromFile(ImagePicker.createCacheImageFile(context))
                resizeImage(data.data, uri)

                this.picker.imageUri = uri
            }

            return true
        }
        else return false


    }

}