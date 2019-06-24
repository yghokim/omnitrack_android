package kr.ac.snu.hcil.android.common.view.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.otaliastudios.cameraview.*
import kr.ac.snu.hcil.android.common.getActionPrefix
import kr.ac.snu.hcil.android.common.view.InterfaceHelper
import kr.ac.snu.hcil.android.common.view.R
import kr.ac.snu.hcil.android.common.view.applyTint
import kr.ac.snu.hcil.android.common.view.indicator.LoadingIndicatorBar

/**
 * Created by Young-Ho Kim on 2017-03-07.
 */
class CameraPickDialogFragment : DialogFragment(), View.OnClickListener {

    companion object {

        const val EXTRA_REQUEST_KEY = "req"
        const val EXTRA_IMAGE_DATA = "image"

        const val SUFFIX_ACTION_PHOTO_TAKEN = "CAMERA_PHOTO_TAKEN"

        const val EXTRA_IMAGE_MAX_AREA = "max_area"

        fun getActionName(context: Context): String {
            return "${context.getActionPrefix()}.$SUFFIX_ACTION_PHOTO_TAKEN"
        }

        fun getInstance(requestKey: String, maxImageArea: Int?): CameraPickDialogFragment {
            val fragment = CameraPickDialogFragment()
            val args = Bundle()
            args.putString(EXTRA_REQUEST_KEY, requestKey)

            if(maxImageArea!=null)
                args.putInt(EXTRA_IMAGE_MAX_AREA, maxImageArea)

            fragment.arguments = args

            return fragment
        }
    }

    private lateinit var cameraView: CameraView
    private lateinit var shutterButton: View
    private lateinit var cameraModeToggleButton: ToggleButton
    private lateinit var titleView: TextView
    private lateinit var cancelButton: AppCompatImageButton

    private lateinit var loadingIndicator: LoadingIndicatorBar

    private val listener = CameraPickListener()

    private val cameraFrontDrawable by lazy {
        applyTint(DrawableCompat.wrap(ContextCompat.getDrawable(context!!, R.drawable.camera_front)!!), Color.WHITE)
    }

    private val cameraRearDrawable by lazy {
        applyTint(DrawableCompat.wrap(ContextCompat.getDrawable(context!!, R.drawable.camera_rear)!!), Color.WHITE)
    }

    private fun findViews(view: View) {
        cameraView = view.findViewById(R.id.ui_camera_view)
        cameraView.addCameraListener(listener)

        if (arguments?.containsKey(EXTRA_IMAGE_MAX_AREA) == true) {
            cameraView.setPictureSize(SizeSelectors.maxArea(arguments!!.getInt(EXTRA_IMAGE_MAX_AREA)))
        }

        shutterButton = view.findViewById(R.id.ui_camera_shutter)
        shutterButton.setOnClickListener(this)

        cameraModeToggleButton = view.findViewById(R.id.ui_button_toggle_camera)
        applyTintToCompoundDrawables(cameraModeToggleButton)
        cameraModeToggleButton.setOnCheckedChangeListener { compoundButton, checked ->
            if (checked) {
                //true: SelfieMode on
                compoundButton.setCompoundDrawablesRelativeWithIntrinsicBounds(cameraRearDrawable, null, null, null)
                cameraView.facing = Facing.FRONT
            } else {
                compoundButton.setCompoundDrawablesRelativeWithIntrinsicBounds(cameraFrontDrawable, null, null, null)
                cameraView.facing = Facing.BACK
            }
        }

        titleView = view.findViewById(R.id.title)
        titleView.setText(R.string.msg_camera_input_header)

        cancelButton = view.findViewById(R.id.ui_button_cancel)
        cancelButton.setOnClickListener(this)

        loadingIndicator = view.findViewById(R.id.ui_loading_indicator)
        loadingIndicator.setStateWithoutAnimation(false)
        loadingIndicator.setMessage(R.string.msg_indicator_message_processing)
    }

    private fun applyTintToCompoundDrawables(button: Button) {
        val compoundDrawables = button.compoundDrawablesRelative
        for (k in (0..3)) {
            compoundDrawables[k]?.let {
                compoundDrawables[k] = applyTint(it, Color.WHITE)
            }
        }
    }

    override fun onClick(view: View) {
        if (view === shutterButton) {
            try {
                shutterButton.isEnabled = false
                shutterButton.alpha = InterfaceHelper.ALPHA_INACTIVE
                loadingIndicator.show()
                cameraView.capturePicture()
            } catch(e: Exception) {
                e.printStackTrace()
            }
        } else if (view === cancelButton) {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            cameraView.start()
        } catch (ex: Exception) {
            if (ex is RuntimeException) {
                Toast.makeText(this.context, "You can't use the camera for now.", Toast.LENGTH_LONG).show()
                this.dismissAllowingStateLoss()
            }
        }
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.destroy()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.fragment_camera_input, null)

        findViews(view)

        return AlertDialog.Builder(activity)
                .setView(view)
                .create()
    }

    inner class CameraPickListener : CameraListener() {

        override fun onPictureTaken(jpeg: ByteArray?) {
            if (jpeg != null) {
                arguments?.getString(EXTRA_REQUEST_KEY)?.let {
                    LocalBroadcastManager.getInstance(context!!)
                            .sendBroadcast(Intent(getActionName(requireContext())).putExtra(EXTRA_IMAGE_DATA, jpeg).putExtra(EXTRA_REQUEST_KEY, it))
                }
                dismiss()
            }
        }

        override fun onCameraError(exception: CameraException) {
            Toast.makeText(context, exception.message, Toast.LENGTH_LONG).show()
        }
    }
}