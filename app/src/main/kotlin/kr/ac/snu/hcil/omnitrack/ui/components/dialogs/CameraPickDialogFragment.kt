package kr.ac.snu.hcil.omnitrack.ui.components.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.widget.AppCompatImageButton
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import com.flurgle.camerakit.CameraKit
import com.flurgle.camerakit.CameraView
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.utils.applyTint

/**
 * Created by Young-Ho Kim on 2017-03-07.
 */
class CameraPickDialogFragment : DialogFragment(), View.OnClickListener {

    companion object {
    }

    private lateinit var cameraView: CameraView
    private lateinit var shutterButton: Button
    private lateinit var cameraModeToggleButton: ToggleButton
    private lateinit var titleView: TextView
    private lateinit var cancelButton: AppCompatImageButton

    private val listener = CameraListener()

    private val cameraFrontDrawable by lazy {
        applyTint(DrawableCompat.wrap(OTApplication.app.getDrawable(R.drawable.camera_front)), Color.WHITE)
    }

    private val cameraRearDrawable by lazy {
        applyTint(DrawableCompat.wrap(OTApplication.app.getDrawable(R.drawable.camera_rear)), Color.WHITE)
    }

    private fun findViews(view: View) {
        cameraView = view.findViewById(R.id.ui_camera_view) as CameraView
        cameraView.setCameraListener(listener)

        shutterButton = view.findViewById(R.id.ui_camera_shutter) as Button
        shutterButton.setOnClickListener(this)

        cameraModeToggleButton = view.findViewById(R.id.ui_button_toggle_camera) as ToggleButton
        applyTintToCompoundDrawables(cameraModeToggleButton)
        cameraModeToggleButton.setOnCheckedChangeListener { compoundButton, checked ->
            if (checked) {
                //true: SelfieMode on
                compoundButton.setCompoundDrawablesRelativeWithIntrinsicBounds(cameraRearDrawable, null, null, null)
                cameraView.setFacing(CameraKit.Constants.FACING_FRONT)
            } else {
                compoundButton.setCompoundDrawablesRelativeWithIntrinsicBounds(cameraFrontDrawable, null, null, null)
                cameraView.setFacing(CameraKit.Constants.FACING_BACK)
            }
        }

        titleView = view.findViewById(R.id.title) as TextView
        titleView.setText(R.string.msg_camera_input_header)

        cancelButton = view.findViewById(R.id.ui_button_cancel) as AppCompatImageButton
        cancelButton.setOnClickListener(this)
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

        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.fragment_camera_input, null)

        findViews(view)

        return AlertDialog.Builder(activity)
                .setView(view)
                .create()
    }

    inner class CameraListener : com.flurgle.camerakit.CameraListener() {
        override fun onPictureTaken(jpeg: ByteArray?) {
            super.onPictureTaken(jpeg)
            if (jpeg != null) {

            }
        }

        override fun onCameraOpened() {
            super.onCameraOpened()
        }

        override fun onCameraClosed() {
            super.onCameraClosed()
        }

    }
}