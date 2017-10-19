package kr.ac.snu.hcil.omnitrack.ui.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import butterknife.bindView
import com.badoo.mobile.util.WeakHandler
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.receivers.ScreenReceiverBase


/**
 * Created by younghokim on 2017. 4. 19..
 */
class ReminderPopupActivity : OTActivity(false, false), View.OnClickListener {

    companion object {
        const val TAG = "ReminderPopupActivity"

        private val VISIBILITY_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        fun makeIntent(context: Context, triggerId: String, triggerTime: Long): Intent {
            return Intent(context, ReminderPopupActivity::class.java).apply {
                this.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                this.putExtra(OTApp.INTENT_EXTRA_OBJECT_ID_TRIGGER, triggerId)
                this.putExtra(OTApp.INTENT_EXTRA_TRIGGER_TIME, triggerTime)
            }
        }
    }

    private val skipButton: Button by bindView(R.id.ui_button_skip)
    private val snoozeButton: Button by bindView(R.id.ui_button_snooze)
    private val proceedButton: Button by bindView(R.id.ui_button_proceed)

    private val screenReceiver = ScreenReceiver()


    private var currentTriggerTime: Long = -1
    private val currentTrackers = ArrayList<String>()

    private var resultAction: Action? = null

    enum class Action {
        Dismiss, Skip, Snooze, Proceed
    }

    private var finishingAction: Action? = null

    private var turnOffHandler: WeakHandler? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate")
        setContentView(R.layout.layout_reminder_popup_dialog)
        window.decorView.systemUiVisibility = VISIBILITY_FLAGS

        skipButton.setOnClickListener(this)
        snoozeButton.setOnClickListener(this)
        proceedButton.setOnClickListener(this)

        /*
        val root = findViewById(R.id.root)
        root.setOnClickListener {
            finish()
        }*/
        registerReceiver(screenReceiver, ScreenReceiverBase.filter)


    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()

        onInitialize(intent)
    }

    private fun onInitialize(intent: Intent) {
        window.setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,

                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)


        window.attributes = window.attributes.apply { this.screenBrightness = -1f }

        resultAction = null

        Handler().postDelayed({
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }, 1000)

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        Log.d(TAG, "new Intent")

        onInitialize(intent)
    }


    override fun onResume() {
        super.onResume()

        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()

        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()

        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        unregisterReceiver(screenReceiver)
    }


    override fun onClick(v: View?) {
        if (v === skipButton) {
            onFinishWithAction(Action.Skip)
            finish()
        } else if (v === snoozeButton) {
            onFinishWithAction(Action.Snooze)
            finish()
        } else if (v === proceedButton) {
            onFinishWithAction(Action.Proceed)
            finish()
        }
    }

    private fun onFinishWithAction(action: Action) {
        this.resultAction = action
        Log.d(TAG, "Result Action: ${action}")


    }

    override fun finish() {
        Log.d(TAG, "finish")
        if (resultAction == null) {
            onFinishWithAction(Action.Dismiss)
        }

        super.finish()
    }

    inner class ScreenReceiver : ScreenReceiverBase() {
        override fun onScreenOff() {
            super.onScreenOff()
            Log.d(TAG, "screen Off")
            onFinishWithAction(Action.Dismiss)
            finish()
        }

        override fun onScreenOn() {
            super.onScreenOn()
            Log.d(TAG, "screen On")
        }
    }
}