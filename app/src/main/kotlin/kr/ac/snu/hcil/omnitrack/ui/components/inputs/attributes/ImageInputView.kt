package kr.ac.snu.hcil.omnitrack.ui.components.inputs.attributes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.content.LocalBroadcastManager
import android.util.AttributeSet
import dagger.Lazy
import gun0912.tedbottompicker.TedBottomPicker
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.SerialDisposable
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.datatypes.OTServerFile
import kr.ac.snu.hcil.omnitrack.core.net.OTLocalMediaCacheManager
import kr.ac.snu.hcil.omnitrack.ui.components.common.ImagePicker
import kr.ac.snu.hcil.omnitrack.ui.components.dialogs.CameraPickDialogFragment
import kr.ac.snu.hcil.omnitrack.utils.getActivity
import org.jetbrains.anko.runOnUiThread
import javax.inject.Inject

/**
 * Created by Young-Ho Kim on 2016-07-22.
 */
class ImageInputView(context: Context, attrs: AttributeSet? = null) : AAttributeInputView<OTServerFile>(R.layout.input_image, context, attrs), ImagePicker.ImagePickerCallback {

    companion object {
        const val REQUEST_CODE_CAMERA = 2
        const val REQUEST_CODE_GALLERY = 4

        const val IMAGE_MAX_PIXELS = 720 * 1280

        val eventIntentFilter: IntentFilter by lazy {
            IntentFilter(CameraPickDialogFragment.EXTRA_ACTION_PHOTO_TAKEN)
        }

    }

    @Inject
    lateinit var localCacheManager: Lazy<OTLocalMediaCacheManager>

    override val typeId: Int = VIEW_TYPE_IMAGE

    override var value: OTServerFile? = null
        set(rawValue) {
            println("image serverUri rawvalue: ${rawValue}")
            val value = if (rawValue?.serverPath?.isBlank() == true) null else rawValue
            if (field != value) {
                field = value
                if (value == null) {
                    picker.uriChanged.suspend = true
                    picker.imageUri = Uri.EMPTY
                    picker.uriChanged.suspend = false
                } else {
                    loadingSubscription.set(
                            localCacheManager.get().getCachedUri(value).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe {
                                context.runOnUiThread {
                                    //TODO mode: Loading
                                    picker.isEnabled = false
                                }
                            }.subscribe({ (refreshed, localUri) ->
                                picker.uriChanged.suspend = true
                                picker.imageUri = localUri
                                picker.uriChanged.suspend = false
                                picker.isEnabled = true
                            }, {
                                picker.uriChanged.suspend = true
                                picker.imageUri = Uri.EMPTY
                                picker.uriChanged.suspend = false
                                picker.isEnabled = true
                            })
                    )
                }

                onValueChanged(value)
            }
        }

    val picker: ImagePicker = findViewById(R.id.ui_image_picker)
    private val loadingSubscription = SerialDisposable()
    private val subscriptions = CompositeDisposable()

    private val eventReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                println("receiver image dialog event")
                when (intent.action) {
                    CameraPickDialogFragment.EXTRA_ACTION_PHOTO_TAKEN -> {
                        val requestKey = intent.getStringExtra(CameraPickDialogFragment.EXTRA_REQUEST_KEY)
                        if (requestKey != null) {
                            if (requestKey == this@ImageInputView.boundAttributeObjectId) {
                                val imageData = intent.getByteArrayExtra(CameraPickDialogFragment.EXTRA_IMAGE_DATA)
                                this@ImageInputView.handleCameraInputData(imageData)
                            }
                        }
                    }
                }
            }

        }
    }

    init {
        (context.applicationContext as OTApp).currentConfiguredContext.configuredAppComponent.inject(this)

        picker.callback = this

        picker.uriChanged += {
            sender, uri ->
            println("picker uri changed to $uri")
            if (uri == Uri.EMPTY) {
                value = null
            } else if (!uri.equals(value?.let { localCacheManager.get().getCachedUriImmediately(it.serverPath) } ?: Uri.EMPTY)) {
                val newServerPath = localCacheManager.get().generateRandomServerPath(uri)
                val newServerFile = OTServerFile.fromLocalFile(newServerPath, uri, context)
                subscriptions.add(
                        localCacheManager.get().insertOrUpdateNewLocalMedia(uri, newServerFile).subscribe { serverFile ->
                            value = serverFile
                        })
            }
        }
    }

    override fun focus() {

    }

    override fun onRequestCameraImage(view: ImagePicker) {
        val activity = this.getActivity()
        if (activity != null) {
            picker.showCameraPickDialog(activity, boundAttributeObjectId ?: "")
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        LocalBroadcastManager.getInstance(context).registerReceiver(eventReceiver, eventIntentFilter)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        LocalBroadcastManager.getInstance(context).unregisterReceiver(eventReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        loadingSubscription.set(null)
        subscriptions.clear()
    }

    fun resizeImage(source: Uri, dest: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, source)
        resizeImage(bitmap, dest)
    }

    fun resizeImage(bitmap: Bitmap, dest: Uri) {
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

    private fun handleCameraInputData(imageBytes: ByteArray): Boolean {
        println("camerapick result: ${imageBytes}")

        try {
            val image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val resizedUri = Uri.fromFile(picker.createCacheImageFile(context))
            resizeImage(image, resizedUri)

            this.picker.imageUri = resizedUri!!
            return true
        } catch(ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    override fun setValueFromActivityResult(data: Intent, requestType: Int): Boolean {

        if (requestType == REQUEST_CODE_CAMERA) {
            return handleCameraInputData(data.getByteArrayExtra(CameraPickDialogFragment.EXTRA_IMAGE_DATA))
        } else if (requestType == REQUEST_CODE_GALLERY) {
            if (data.data != null) {
                val uri = Uri.fromFile(picker.createCacheImageFile(context))
                resizeImage(data.data, uri)

                this.picker.imageUri = uri
            }

            return true
        } else return false


    }

}