package kr.ac.snu.hcil.omnitrack.ui.activities

import android.support.v4.content.ContextCompat
import com.google.gson.JsonObject
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.R
import kr.ac.snu.hcil.omnitrack.core.OTTracker

/**
 * Created by younghokim on 2016. 11. 17..
 */
abstract class OTTrackerAttachedActivity(layoutId: Int) : MultiButtonActionBarActivity(layoutId) {

    private var trackerObjectId: String? = null
    private var _tracker: OTTracker? = null

    protected val tracker: OTTracker? get() = _tracker

    override fun onStart() {
        super.onStart()
        trackerObjectId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        reloadTracker()
    }

    override fun onResume() {
        super.onResume()
    }

    protected fun reloadTracker() {
        if (trackerObjectId != null) {
            _tracker = OTApplication.app.currentUser[trackerObjectId!!]
            if (_tracker != null) {
                setHeaderColor(tracker!!.color, true)
                onTrackerLoaded(tracker!!)
            } else {
                setHeaderColor(ContextCompat.getColor(this, R.color.colorPrimary), false)
            }
        }
    }

    protected open fun onTrackerLoaded(tracker: OTTracker) {
    }

    override fun onSessionLogContent(contentObject: JsonObject) {
        super.onSessionLogContent(contentObject)
        contentObject.addProperty("tracker_id", tracker?.objectId)
        contentObject.addProperty("tracker_name", tracker?.name)
    }

}