package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import gun0912.tedbottompicker.TedBottomPicker
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.database.SynchronizedUri
import kr.ac.snu.hcil.omnitrack.ui.components.common.ImagePicker
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.CameraPickDialogFragment
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import rx.internal.util.SubscriptionList

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
                subscriptions.clear()
                field = value
                if (value.isLocalUriValid) {
                    picker.uriChanged.suspend = true
                    picker.imageUri = value.localUri
                    picker.uriChanged.suspend = false
                } else if (value.isSynchronized) {
                    picker.isEnabled = false
                    subscriptions.add(
                            OTApplication.app.storageHelper.downloadFileTo(value.serverUri.path, value.localUri).subscribe({
                                uri ->
                                picker.uriChanged.suspend = true
                                picker.imageUri = uri
                                picker.uriChanged.suspend = false
                                picker.isEnabled = true
                            }, {
                                error ->
                                error?.printStackTrace()
                                picker.uriChanged.suspend = true
                                picker.imageUri = Uri.EMPTY
                                picker.uriChanged.suspend = false
                                picker.isEnabled = true
                            })

                    )
                } else {
                    picker.imageUri = Uri.EMPTY
                }
            }
        }

    val picker: ImagePicker = findViewById(R.id.ui_image_picker) as ImagePicker
    private val subscriptions = SubscriptionList()

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
            picker.showCameraPickDialog(activity, makeActivityForResultRequestCode(position, REQUEST_CODE_CAMERA))
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

                    val resizedUri = Uri.fromFile(picker.createCacheImageFile(context))
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
        resizeImage(bitmap, dest)
    }

    fun resizeImage(bitmap: Bitmap, dest: Uri)
    {
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
            val imageBytes = data.getByteArrayExtra(CameraPickDialogFragment.EXTRA_IMAGE_DATA)

            println("camerapick result: ${imageBytes}")

            if (imageBytes != null) {
                try {
                    val image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                    val resizedUri = Uri.fromFile(picker.createCacheImageFile(context))
                    resizeImage(image, resizedUri)

                    this.picker.imageUri = resizedUri!!
                } catch(ex: Exception) {
                    ex.printStackTrace()
                }
            }
            return true
        } else if (requestType == REQUEST_CODE_GALLERY) {
            if (data.data != null) {
                val uri = Uri.fromFile(picker.createCacheImageFile(context))
                resizeImage(data.data, uri)

                this.picker.imageUri = uri
            }

            return true
        }
        else return false


    }

}