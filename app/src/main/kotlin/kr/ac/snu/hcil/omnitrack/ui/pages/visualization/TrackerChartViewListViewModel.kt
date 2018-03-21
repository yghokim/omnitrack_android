package kr.ac.snu.hcil.omnitrack.ui.pages.visualization

import android.app.Application
import android.support.v7.util.DiffUtil
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.TrackerHelper
import kr.ac.snu.hcil.omnitrack.core.visualization.ChartModel
import kr.ac.snu.hcil.omnitrack.core.visualization.Granularity
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.RealmViewModel

/**
 * Created by younghokim on 2017. 8. 6..
 */
class TrackerChartViewListViewModel(app: Application) : RealmViewModel(app) {

    val currentGranularitySubject: BehaviorSubject<Granularity> = BehaviorSubject.createDefault(Granularity.WEEK_REL)
    val currentPointSubject: BehaviorSubject<Long> = BehaviorSubject.createDefault(System.currentTimeMillis())
    val isBusySubject: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    private val currentChartViewModelList = ArrayList<ChartModel<*>>()
    private val chartViewModelListSubject: BehaviorSubject<List<ChartModel<*>>> = BehaviorSubject.create()
    val chartViewModels: Observable<List<ChartModel<*>>> get() = chartViewModelListSubject

    var suspendApplyingScope = false

    private var currentTrackerId: String? = null

    val trackerNameSubject = BehaviorSubject.createDefault<String>("")

    var trackerName: String
        get() = trackerNameSubject.value!!
        private set(value) {
            if (trackerNameSubject.value != value) {
                trackerNameSubject.onNext(value)
            }
        }

    var granularity: Granularity
        get() = currentGranularitySubject.value!!
        set(value) {
            if (currentGranularitySubject.value != value) {
                currentGranularitySubject.onNext(value)
                applyScopeToChartViewModels()
            }
        }

    var point: Long
        get() = currentPointSubject.value!!
        set(value) {
            if (currentPointSubject.value != value) {
                currentPointSubject.onNext(value)
                applyScopeToChartViewModels()
            }
        }


    fun init(trackerId: String) {

        if (trackerId != currentTrackerId) {
            currentTrackerId = trackerId

            clearChartViewModels(false)
            val trackerDao = dbManager.get().getTrackerQueryWithId(trackerId, realm).findFirst()
            if (trackerDao != null) {
                currentChartViewModelList.addAll(TrackerHelper.makeRecommendedChartModels(trackerDao, realm, getApplication<OTApp>().currentConfiguredContext))
                chartViewModelListSubject.onNext(currentChartViewModelList)
            }

        }
    }

    fun reloadChartData() {
        applyScopeToChartViewModels(true)
    }

    private fun applyScopeToChartViewModels(force: Boolean = false) {
        if (!suspendApplyingScope || force) {
            val point = point
            val granularity = granularity
            currentChartViewModelList.forEach { model ->
                model.setTimeScope(point, granularity)
                model.reload()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearChartViewModels(false)
    }

    private fun clearChartViewModels(pushEmptyList: Boolean = true) {
        currentChartViewModelList.forEach { model ->
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