package kr.ac.snu.hcil.omnitrack.ui.activities

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.widget.Button
import butterknife.bindView
import kr.ac.snu.hcil.omnitrack.R


/**
 * Created by younghokim on 2017. 4. 19..
 */
class ReminderPopupActivity : OTActivity(false, false), View.OnClickListener {

    private val skipButton: Button by bindView(R.id.ui_button_skip)
    private val snoozeButton: Button by bindView(R.id.ui_button_snooze)
    private val proceedButton: Button by bindView(R.id.ui_button_proceed)

    enum class Action {
        Dismiss, Skip, Snooze, Proceed
    }

    private var finishingAction: Action? = null

    private fun isSleeping(powerManager: PowerManager): Boolean {
        if (Build.VERSION.SDK_INT <= 19) {
            return !powerManager.isScreenOn
        } else {
            return !powerManager.isInteractive
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_reminder_popup_dialog)

        window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or +WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or +WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        val root = findViewById(R.id.root)
        root.setOnClickListener {

        }

    }

    override fun onStart() {
        super.onStart()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        println("Popup start. IsSleeping: ${isSleeping(powerManager)}")
        if (isSleeping(powerManager)) {
            //val wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK)

        }

        skipButton.setOnClickListener(this)
        snoozeButton.setOnClickListener(this)
        proceedButton.setOnClickListener(this)
    }

    override fun onPause() {
        super.onPause()
        println("Popup onPause")
    }

    override fun onStop() {
        super.onStop()
        println("Popup onStop")
    }


    override fun onClick(v: View?) {
        if (v === skipButton) {

            finish()
        } else if (v === snoozeButton) {
            finish()
        } else if (v === proceedButton) {
            finish()
        }
    }


    protected fun onFinishWithAction(action: Action) {

    }
}