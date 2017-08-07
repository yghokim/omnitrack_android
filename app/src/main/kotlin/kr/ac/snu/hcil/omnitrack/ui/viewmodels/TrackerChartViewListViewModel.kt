package kr.ac.snu.hcil.omnitrack.ui.viewmodels

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

    private val currentChartViewModelList = ArrayList<ChartModel<out Any>>()
    private val chartViewModelListSubject: BehaviorSubject<List<ChartModel<out Any>>> = BehaviorSubject.create()
    val chartViewModels: Observable<List<ChartModel<out Any>>> get() = chartViewModelListSubject

    var granularity: Granularity?
        get() = currentGranularitySubject.value
        set(value) {
            currentGranularitySubject.onNext(value)
        }

    var point: Long?
        get() = currentPointSubject.value
        set(value) {
            currentPointSubject.onNext(value)
        }

    override fun onTrackerAttached(newTracker: OTTracker) {
        super.onTrackerAttached(newTracker)

    }

    private fun clearChartViewModels() {
        currentChartViewModelList.forEach {
            model ->
            model.recycle()
        }
        currentChartViewModelList.clear()

    }

    fun setScope(point: Long, granularity: Granularity) {
        this.granularity = granularity
        this.point = point
    }

    fun refreshCharts() {

    }


}