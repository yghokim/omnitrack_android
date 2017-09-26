package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.support.v7.util.DiffUtil
import kr.ac.snu.hcil.omnitrack.OTApplication
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.OTUser
import kr.ac.snu.hcil.omnitrack.core.database.ItemCountTracer
import kr.ac.snu.hcil.omnitrack.core.triggers.OTTrigger
import rx.Observable
import rx.Subscription
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 2017-05-30.
 */

typealias TrackerWithIndex = Pair<OTTracker, Int>
class TrackerListViewModel : UserAttachedViewModel() {

    private var trackerViewModelListSubject: BehaviorSubject<List<TrackerInformationViewModel>> = BehaviorSubject.create()

    private val currentTrackerViewModelList = ArrayList<TrackerInformationViewModel>()

    val trackerViewModels: Observable<List<TrackerInformationViewModel>>
        get() = trackerViewModelListSubject

    override fun onUserAttached(newUser: OTUser) {
        super.onUserAttached(newUser)

        clearTrackerViewModelList()
        val viewModels = newUser.trackers.map { TrackerInformationViewModel(it).apply { this.register() } }
        currentTrackerViewModelList.addAll(viewModels)
        trackerViewModelListSubject.onNext(
                currentTrackerViewModelList
        )

        internalSubscriptions.add(
                newUser.trackerAdded.subscribe {
                    trackerIndexPair ->
                    currentTrackerViewModelList.add(TrackerInformationViewModel(trackerIndexPair.first).apply { this.register() })
                    trackerViewModelListSubject.onNext(currentTrackerViewModelList)
                }
        )

        internalSubscriptions.add(
                newUser.trackerRemoved.subscribe {
                    trackerIndexPair ->

                    currentTrackerViewModelList.find { it.tracker == trackerIndexPair.first }
                            ?.let {
                                it.unregister()
                                currentTrackerViewModelList.remove(it)
                                trackerViewModelListSubject.onNext(currentTrackerViewModelList)
                            }
                }
        )
    }

    override fun onDispose() {
        super.onDispose()
        clearTrackerViewModelList()
    }

    private fun clearTrackerViewModelList() {
        currentTrackerViewModelList.forEach {
            it.unregister()
        }
        currentTrackerViewModelList.clear()
        trackerViewModelListSubject.onNext(emptyList())
    }

    class TrackerInformationViewModel(val tracker: OTTracker) {
        val totalItemCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val todayCount: BehaviorSubject<Long> = BehaviorSubject.create()

        val lastLoggingTime: BehaviorSubject<Long> = BehaviorSubject.create()

        val trackerName: BehaviorSubject<String> = BehaviorSubject.create()
        val trackerColor: BehaviorSubject<Int> = BehaviorSubject.create()

        val activeNotificationCount: BehaviorSubject<Int> = BehaviorSubject.create()

        val trackerEditable: BehaviorSubject<Boolean> = BehaviorSubject.create()

        private val countTracer: ItemCountTracer = ItemCountTracer(tracker)

        private val subscriptions = CompositeSubscription()

        private val reminderSubscriptionDict = android.support.v4.util.ArrayMap<String, CompositeSubscription>()

        var isActive: Boolean = false
            private set

        init {
        }

        private fun addSubscriptionToTrigger(trigger: OTTrigger, subscription: Subscription) {
            if (!reminderSubscriptionDict.contains(trigger.objectId)) {
                reminderSubscriptionDict[trigger.objectId] = CompositeSubscription()
            }
            reminderSubscriptionDict[trigger.objectId]?.add(subscription)
        }

        fun register() {
            subscriptions.add(
                    countTracer.itemCountObservable.subscribe {
                        count ->
                        totalItemCount.onNext(count)
                    }
            )

            subscriptions.add(
                    OTApplication.app.databaseManager.getLogCountOfDay(tracker).subscribe {
                        count ->
                        todayCount.onNext(count)
                    }
            )

            subscriptions.add(
                    OTApplication.app.databaseManager.getLastLoggingTime(tracker).subscribe {
                        time ->
                        lastLoggingTime.onNext(time)
                    }
            )

            subscriptions.add(
                    tracker.colorChanged.subscribe {
                        color ->
                        trackerColor.onNext(color.second)
                    }
            )
            trackerColor.onNext(tracker.color)

            subscriptions.add(
                    tracker.nameChanged.subscribe {
                        name ->
                        trackerName.onNext(name.second)
                    }
            )
            trackerName.onNext(tracker.name)

            trackerEditable.onNext(tracker.isEditable)


            tracker.owner?.let {
                user ->
                fun updateReminderCount() {
                    activeNotificationCount.onNext(user.triggerManager.getAttachedTriggers(tracker, OTTrigger.ACTION_NOTIFICATION).filter { it.isOn == true }.size)
                }
                updateReminderCount()

                val attachedTriggers = user.triggerManager.getAttachedTriggers(tracker, OTTrigger.ACTION_NOTIFICATION)
                attachedTriggers.forEach {
                    trigger ->
                    addSubscriptionToTrigger(trigger,
                            trigger.switchTurned.subscribe {
                                updateReminderCount()
                            })
                }

                subscriptions.add(
                        tracker.reminderAdded.subscribe { reminder ->
                            addSubscriptionToTrigger(reminder.second,
                                    reminder.second.switchTurned.subscribe {
                                        updateReminderCount()
                                    })
                            updateReminderCount()
                        }
                )

                subscriptions.add(
                        tracker.reminderRemoved.subscribe { reminder ->
                            reminderSubscriptionDict[reminder.second.objectId]?.clear()
                            reminderSubscriptionDict.remove(reminder.second.objectId)
                            updateReminderCount()
                        }
                )
            }

            countTracer.register()
            countTracer.notifyConnected()

            isActive = true
        }

        fun unregister() {
            if (countTracer.isRegistered)
                countTracer.unregister()

            subscriptions.clear()
            reminderSubscriptionDict.forEach { entry -> entry.value?.clear() }
            reminderSubscriptionDict.clear()

            isActive = false
        }
    }

    class TrackerViewModelListDiffUtilCallback(val oldList: List<TrackerInformationViewModel>, val newList: List<TrackerInformationViewModel>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].tracker.objectId == newList[newItemPosition].tracker.objectId
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldItemPosition, newItemPosition)
        }


    }

}