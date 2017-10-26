package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.AppCompatImageButton
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import com.wonderkiln.camerakit.CameraKit
import com.wonderkiln.camerakit.CameraView
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.ui.components.common.LoadingIndicatorBar
import kr.ac.snu.hcil.omnitrack.utils.applyTint

/**
 * Created by Young-Ho Kim on 2017-03-07.
 */
class CameraPickDialogFragment : DialogFragment(), View.OnClickListener {

    companion object {

        const val EXTRA_REQUEST_KEY = "req"
        const val EXTRA_IMAGE_DATA = "image"
        const val EXTRA_ACTION_PHOTO_TAKEN = "kr.ac.snu.hcil.omnitrack.ACTION_CAMERA_PHOTO_TAKEN"

        fun getInstance(requestKey: String): CameraPickDialogFragment {
            val fragment = CameraPickDialogFragment()
            val args = Bundle()
            args.putString(EXTRA_REQUEST_KEY, requestKey)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var cameraView: CameraView
    private lateinit var shutterButton: Button
    private lateinit var cameraModeToggleButton: ToggleButton
    private lateinit var titleView: TextView
    private lateinit var cancelButton: AppCompatImageButton

    private lateinit var loadingIndicator: LoadingIndicatorBar

    private val listener = CameraListener()

    private val cameraFrontDrawable by lazy {
        applyTint(DrawableCompat.wrap(ContextCompat.getDrawable(OTApp.instance, R.drawable.camera_front)!!), Color.WHITE)
    }

    private val cameraRearDrawable by lazy {
        applyTint(DrawableCompat.wrap(ContextCompat.getDrawable(OTApp.instance, R.drawable.camera_rear)!!), Color.WHITE)
    }

    private fun findViews(view: View) {
        cameraView = view.findViewById(R.id.ui_camera_view)
        cameraView.setCameraListener(listener)

        shutterButton = view.findViewById(R.id.ui_camera_shutter)
        shutterButton.setOnClickListener(this)

        cameraModeToggleButton = view.findViewById(R.id.ui_button_toggle_camera)
        applyTintToCompoundDrawables(cameraModeToggleButton)
        cameraModeToggleButton.setOnCheckedChangeListener { compoundButton, checked ->
            if (checked) {
                //true: SelfieMode on
                compoundButton.setCompoundDrawablesRelativeWithIntrinsicBounds(cameraRearDrawable, null, null, null)
                cameraView.facing = CameraKit.Constants.FACING_FRONT
            } else {
                compoundButton.setCompoundDrawablesRelativeWithIntrinsicBounds(cameraFrontDrawable, null, null, null)
                cameraView.facing = CameraKit.Constants.FACING_BACK
            }
        }

        titleView = view.findViewById(R.id.title)
        titleView.setText(R.string.msg_camera_input_header)

        cancelButton = view.findViewById(R.id.ui_button_cancel)
        cancelButton.setOnClickListener(this)

        loadingIndicator = view.findViewById(R.id.ui_loading_indicator)
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
                shutterButton.alpha = 0.5f
                loadingIndicator.show()
                cameraView.captureImage()
            } catch(e: Exception) {
                e.printStackTrace()
            }
        } else if (view === cancelButton) {
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        cameraView.stop()
        super.onPause()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val inflater = activity!!.layoutInflater
        val view = inflater.inflate(R.layout.fragment_camera_input, null)

        findViews(view)

        return AlertDialog.Builder(activity)
                .setView(view)
                .create()
    }

    inner class CameraListener : com.wonderkiln.camerakit.CameraListener() {
        override fun onPictureTaken(jpeg: ByteArray?) {
            super.onPictureTaken(jpeg)
            if (jpeg != null) {
                /*
                val activity = activity
                if (activity is OTActivity) {
                    arguments?.getInt(ARG_REQUEST_CODE)?.let {
                        val data = Intent()
                        data.putExtra(EXTRA_IMAGE_DATA, jpeg)
                        activity.performOnActivityResult(it, RESULT_OK, data)
                    }
                }*/

                arguments?.getString(EXTRA_REQUEST_KEY)?.let {
                    LocalBroadcastManager.getInstance(context!!)
                            .sendBroadcast(Intent(EXTRA_ACTION_PHOTO_TAKEN).putExtra(EXTRA_IMAGE_DATA, jpeg).putExtra(EXTRA_REQUEST_KEY, it))
                }
                dismiss()
            }
        }

    }
}