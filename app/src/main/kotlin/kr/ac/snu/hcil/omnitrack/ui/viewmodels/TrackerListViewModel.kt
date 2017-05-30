package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.arch.lifecycle.ViewModel
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.DatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.ItemCountTracer
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 2017-05-30.
 */

typealias TrackerWithIndex = Pair<OTTracker, Int>
class TrackerListViewModel : ViewModel() {
    var user: OTUser? = null
        set(value) {
            if (field != value) {
                field = value
                if (value != null) {
                    onUserChanged(value)
                }
            }
        }

    private fun onUserChanged(newUser: OTUser) {

    }

    class ItemStatisticsViewModel(val tracker: OTTracker) {
        val totalItemCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val todayCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val lastLoggingTime: BehaviorSubject<Long> = BehaviorSubject.create()

        private val countTracer: ItemCountTracer = ItemCountTracer(tracker)

        private val subscriptions = CompositeSubscription()

        init {
        }

        fun register() {
            subscriptions.add(
                    countTracer.itemCountObservable.subscribe {
                        count ->
                        totalItemCount.onNext(count)
                    }
            )

            subscriptions.add(
                    DatabaseManager.getLogCountOfDay(tracker).subscribe {
                        count ->
                        todayCount.onNext(count)
                    }
            )

            subscriptions.add(
                    DatabaseManager.getLastLoggingTime(tracker).subscribe {
                        time ->
                        lastLoggingTime.onNext(time)
                    }
            )

            countTracer.register()
            countTracer.notifyConnected()
        }

        fun unregister() {
            if (countTracer.isRegistered)
                countTracer.unregister()

            subscriptions.clear()
        }
    }

}