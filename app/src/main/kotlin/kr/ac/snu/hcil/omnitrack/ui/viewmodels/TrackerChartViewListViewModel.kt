package kr.ac.snu.hcil.omnitrack.ui.viewmodels

import android.support.v7.util.DiffUtil
import kr.ac.snu.hcil.omnitrack.core.OTTracker
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import rx.Observable
import rx.subjects.BehaviorSubject

/**
 * Created by younghokim on 2017. 8. 6..
 */
class TrackerChartViewListViewModel : TrackerAttachedViewModel() {

    val currentGranularitySubject: BehaviorSubject<Granularity> = BehaviorSubject.create()
    val currentPointSubject: BehaviorSubject<Long> = BehaviorSubject.create(System.currentTimeMillis())
    val isBusySubject: BehaviorSubject<Boolean> = BehaviorSubject.create(false)

    private val currentChartViewModelList = ArrayList<ChartModel<*>>()
    private val chartViewModelListSubject: BehaviorSubject<List<ChartModel<*>>> = BehaviorSubject.create()
    val chartViewModels: Observable<List<ChartModel<*>>> get() = chartViewModelListSubject

    var suspendApplyingScope = false

    var granularity: Granularity?
        get() = currentGranularitySubject.value
        set(value) {
            if (currentGranularitySubject.value != value) {
                currentGranularitySubject.onNext(value)
                applyScopeToChartViewModels()
            }
        }

    var point: Long?
        get() = currentPointSubject.value
        set(value) {
            if (currentPointSubject.value != value) {
                currentPointSubject.onNext(value)
                applyScopeToChartViewModels()
            }
        }

    override fun onTrackerAttached(newTracker: OTTracker) {
        super.onTrackerAttached(newTracker)
        clearChartViewModels(false)
        currentChartViewModelList.addAll(newTracker.getRecommendedChartModels())
        chartViewModelListSubject.onNext(currentChartViewModelList)
    }

    fun reloadChartData() {
        applyScopeToChartViewModels(true)
    }

    private fun applyScopeToChartViewModels(force: Boolean = false) {
        if (!suspendApplyingScope || force) {
            val point = point
            val granularity = granularity
            if (point != null && granularity != null) {
                currentChartViewModelList.forEach {
                    model ->
                    model.setTimeScope(point, granularity)
                    model.reload()
                }
            }
        }
    }

    override fun onDispose() {
        super.onDispose()
        clearChartViewModels()
    }

    private fun clearChartViewModels(pushEmptyList: Boolean = true) {
        currentChartViewModelList.forEach {
            model ->
            model.recycle()
        }
        currentChartViewModelList.clear()

        if (pushEmptyList)
            chartViewModelListSubject.onNext(emptyList())
    }

    fun setScope(point: Long, granularity: Granularity) {
        val suspending = suspendApplyingScope
        suspendApplyingScope = true
        this.granularity = granularity
        this.point = point
        if (!suspending) {
            suspendApplyingScope = false
            applyScopeToChartViewModels()
        }
    }

    class ChartViewModelListDiffUtilCallback(val oldList: List<ChartModel<*>>, val newList: List<ChartModel<*>>) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] === newList[newItemPosition]
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