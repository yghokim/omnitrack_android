package kr.ac.snu.hcil.omnitrack.ui.components.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.FrameLayout
import android.widget.ImageView
import com.koushikdutta.ion.Ion
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.events.Event
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by younghokim on 16. 9. 6..
 */
class ImagePicker : FrameLayout, View.OnClickListener {

    //https://developer.android.com/training/camera/photobasics.html
    companion object{
        fun dispatchCameraIntent(activity: AppCompatActivity, requestCode: Int): Uri?{
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            //ensure there is a camera activity
            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {

                val uri = createCacheImageFileUri(activity)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)

                activity.startActivityForResult(takePictureIntent, requestCode)

                return uri
            }
            else return null
        }

        fun dispatchImagePickIntent(activity: AppCompatActivity, requestCode: Int) {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"

            val pickIntent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"

            val chooserIntent = Intent.createChooser(getIntent, "Select Image")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(pickIntent))

            activity.startActivityForResult(chooserIntent, requestCode)
        }


        fun createCacheImageFileUri(context: Context): Uri {
            return FileProvider.getUriForFile(context, "kr.ac.snu.hcil.omnitrack.fileprovider", createCacheImageFile(context))
        }

        fun createCacheImageFile(context: Context): File {
            // Create an image file name
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            val image = File.createTempFile(
                    imageFileName, /* prefix */
                    ".jpg", /* suffix */
                    storageDir      /* directory */)

            println("camera output : ${image.absolutePath}")

            // Save a file: path for use with ACTION_VIEW intents
            return image
        }
    }

    interface ImagePickerCallback {
        fun onRequestCameraImage(view: ImagePicker)
        fun onRequestGalleryImage(view: ImagePicker)
    }

    var callback: ImagePickerCallback? = null

    var imageUri: Uri = Uri.EMPTY
        set(value) {
            println("new uri _ ${value.toString()}")
            if (field != value) {
                field = value

                if (URLUtil.isNetworkUrl(value.toString())) {
                    println("uri is network. download the image and put it to the imageview.")
                    Ion.with(imageView)
                            .load(value.toString())
                } else {
                    imageView.setImageURI(value)
                }

                if (value == Uri.EMPTY) {
                    removeButton.visibility = View.INVISIBLE
                    buttonGroup.visibility = View.VISIBLE
                    imageView.visibility = View.GONE
                } else {
                    removeButton.visibility = View.VISIBLE
                    buttonGroup.visibility = View.INVISIBLE
                    imageView.visibility = View.VISIBLE
                }
                uriChanged.invoke(this, value)
            }
        }

    val uriChanged = Event<Uri>()

    private val cameraButton: View
    private val galleryButton: View
    private val removeButton: View

    private val buttonGroup: ViewGroup
    private val imageView: ImageView


    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.component_image_picker, this, true)
        this.setBackgroundResource(R.drawable.hatching_repeated_wide_gray)

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
            callback?.onRequestCameraImage(this)
        } else if (view === galleryButton) {
            callback?.onRequestGalleryImage(this)
        } else if (view === removeButton) {
            imageUri = Uri.EMPTY
        }
    }

}