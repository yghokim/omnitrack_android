package kr.ac.snu.hcil.omnitrack.ui.activities

import android.support.v7.app.AppCompatActivity
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by younghokim on 2016. 11. 15..
 */
abstract class OTActivity : AppCompatActivity() {

    protected var isSessionLoggingEnabled = true

    private var resumedAt: Long = 0

    override fun onResume() {
        super.onResume()

        resumedAt = System.currentTimeMillis()
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
}