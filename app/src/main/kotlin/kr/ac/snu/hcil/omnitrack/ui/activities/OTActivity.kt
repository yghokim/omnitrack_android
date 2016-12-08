package kr.ac.snu.hcil.omnitrack.ui.activities

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.ui.components.common.time.DurationPicker
import java.util.*

/**
 * Created by younghokim on 2016. 11. 15..
 */
abstract class OTActivity : AppCompatActivity() {

    protected var isSessionLoggingEnabled = true

    private var resumedAt: Long = 0

    val durationPickers = ArrayList<DurationPicker>()

    private var touchMoveAmount: PointF = PointF()

    override fun onResume() {
        super.onResume()

        resumedAt = System.currentTimeMillis()
    }

    override fun onDestroy() {
        super.onDestroy()
        durationPickers.clear()
    }

    override fun onPause() {
        super.onPause()

        if (isSessionLoggingEnabled) {
            val from = if (intent.hasExtra(OTApplication.INTENT_EXTRA_FROM)) {
                intent.getStringExtra(OTApplication.INTENT_EXTRA_FROM)
            } else null

            val contentObject = JsonObject()
            contentObject.addProperty("isFinishing", isFinishing)
            onSessionLogContent(contentObject)

            val now = System.currentTimeMillis()
            OTApplication.logger.writeSessionLog(this, now - resumedAt, now, from, contentObject.toString())
        }
    }

    protected open fun onSessionLogContent(contentObject: JsonObject) {
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            touchMoveAmount.x++
            touchMoveAmount.y++
        } else if (event.action == MotionEvent.ACTION_UP) {
            if (touchMoveAmount.x < 10 || touchMoveAmount.y < 10) {
                for (v in durationPickers) {
                    val outRect = Rect()
                    v.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        v.setInputMode(false, true)
                    }
                }
            }
            touchMoveAmount.x = 0f
            touchMoveAmount.y = 0f
        }

        return super.dispatchTouchEvent(event)
    }
}