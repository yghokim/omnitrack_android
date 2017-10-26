package kr.ac.snu.hcil.omnitrack.ui.activities

import android.os.Bundle
import android.support.v4.app.Fragment
import io.reactivex.disposables.CompositeDisposable
import kr.ac.snu.hcil.omnitrack.core.database.EventLoggingManager

/**
 * Created by younghokim on 2016. 11. 15..
 */
open class OTFragment : Fragment() {

    private var shownAt: Long? = null

    protected val creationSubscriptions = CompositeDisposable()
    protected val createViewSubscriptions = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        creationSubscriptions.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        createViewSubscriptions.clear()
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

            val contentObject = Bundle()
            contentObject.putBoolean("caused_by_activity_pause", activityPausing)
            contentObject.putString("parent_activity", activity?.javaClass?.simpleName)
            onSessionLogContent(contentObject)

            val now = System.currentTimeMillis()
            EventLoggingManager.logSession(this, elapsed, now, null, contentObject)

            println("finished fragment ${this.javaClass.simpleName}. uptime: $elapsed, $contentObject")
        }
    }

    protected open fun onSessionLogContent(contentObject: Bundle) {
    }
}