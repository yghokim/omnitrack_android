package kr.ac.snu.hcil.omnitrack.ui.activities

import android.support.v4.app.Fragment
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApplication

/**
 * Created by younghokim on 2016. 11. 15..
 */
open class OTFragment : Fragment() {

    private var shownAt: Long? = null

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        if (userVisibleHint) {
            logSession(true)
        }
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        if (isVisibleToUser) {
            shownAt = System.currentTimeMillis()
        } else {
            if (shownAt != null) {
                logSession(activityPausing = false)
            }
        }
    }

    private fun logSession(activityPausing: Boolean) {
        if (shownAt != null) {


            val elapsed = System.currentTimeMillis() - shownAt!!

            val contentObject = JsonObject()
            contentObject.addProperty("caused_by_activity_pause", activityPausing)
            contentObject.addProperty("activity", activity.javaClass.simpleName)
            onSessionLogContent(contentObject)

            val now = System.currentTimeMillis()
            OTApplication.logger.writeSessionLog(this, elapsed, now, null, contentObject.toString())

            println("finished fragment ${this.javaClass.simpleName}. uptime: $elapsed, ${contentObject.toString()}")
        }
    }

    protected open fun onSessionLogContent(contentObject: JsonObject) {
    }
}