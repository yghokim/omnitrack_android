package kr.ac.snu.hcil.omnitrack.ui.activities

import android.os.Bundle
import android.support.v4.content.ContextCompat
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

    //private val startSubscriptions = SubscriptionList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trackerObjectId = intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (!trackerObjectId.isNullOrBlank()) {
            this.trackerObjectId = trackerObjectId
            creationSubscriptions.add(
                    signedInUserObservable.flatMap {
                        user ->
                        user.getTrackerObservable(trackerObjectId)
                    }.doOnNext { tracker ->
                        this._tracker = tracker
                    }.subscribe({
                        tracker ->
                        setHeaderColor(tracker.color, true)
                        onTrackerLoaded(tracker)

                    }, {
                        ex ->
                        ex.printStackTrace()
                        println("tracker loading error")
                        setHeaderColor(ContextCompat.getColor(this, R.color.colorPrimary), false)
                    })
            )
        }
    }

    protected open fun onRestoredInstanceStateWithTracker(savedInstanceState: Bundle, tracker: OTTracker) {

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        val trackerId = if (tracker == null) {
            intent.getStringExtra(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        } else tracker?.objectId

        println("saving instance state of tracker : ${trackerId}")

        outState?.putString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER, trackerId)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        val trackerId = savedInstanceState?.getString(OTApplication.INTENT_EXTRA_OBJECT_ID_TRACKER)
        if (trackerId != null) {
            println("loading tracker with restored id")
            creationSubscriptions.add(
                    getUserOrGotoSignIn().toObservable().flatMap { user -> user.getTrackerObservable(trackerId) }.subscribe({
                        tracker ->
                    println("restored tracker.")
                    this._tracker = tracker
                    onRestoredInstanceStateWithTracker(savedInstanceState, tracker)
            }, { println("user error. to go sign in.") })
            )
        }
    }

    protected open fun onTrackerLoaded(tracker: OTTracker) {
    }

    override fun onSessionLogContent(contentObject: Bundle) {
        super.onSessionLogContent(contentObject)
        contentObject.putString("tracker_id", tracker?.objectId)
        contentObject.putString("tracker_name", tracker?.name)
    }

}