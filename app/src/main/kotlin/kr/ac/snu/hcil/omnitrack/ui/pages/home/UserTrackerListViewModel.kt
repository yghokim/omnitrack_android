package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.arch.lifecycle.ViewModel
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import rx.subjects.BehaviorSubject

/**
 * Created by younghokim on 2017-05-30.
 */
typealias TrackerWithIndex = Pair<OTTracker, Int>

class UserTrackerListViewModel : ViewModel() {

    var user: OTUser? = null

    val trackerAdded: BehaviorSubject<TrackerWithIndex> = BehaviorSubject.create<TrackerWithIndex>()
    val trackerRemoved: BehaviorSubject<TrackerWithIndex> = BehaviorSubject.create<TrackerWithIndex>()


    override fun onCleared() {
        super.onCleared()
        user = null
    }
}