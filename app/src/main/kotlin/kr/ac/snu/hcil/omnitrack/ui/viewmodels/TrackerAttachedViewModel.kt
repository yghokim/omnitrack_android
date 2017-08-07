package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.arch.lifecycle.ViewModel
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 2017. 8. 6..
 */
open class TrackerAttachedViewModel : ViewModel() {
    var tracker: OTTracker? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null) {
                    onTrackerAttached(value)
                } else {
                    onDispose()
                }
            }
        }

    protected val internalSubscriptions = CompositeSubscription()

    protected open fun onTrackerAttached(newTracker: OTTracker) {

    }

    protected open fun onDispose() {
        internalSubscriptions.clear()
    }

    override fun onCleared() {
        super.onCleared()
        onDispose()
        tracker = null
    }
}