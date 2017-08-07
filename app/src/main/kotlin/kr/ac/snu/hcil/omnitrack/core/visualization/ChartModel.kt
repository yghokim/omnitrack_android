package kr.ac.snu.hcil.omnitrack.core.visualization

import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.IChartInterface
import kr.ac.snu.hcil.omnitrack.ui.components.visualization.AChartDrawer
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.subjects.BehaviorSubject
import rx.subscriptions.CompositeSubscription

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class ChartModel<T>() : IChartInterface<T> {
    override fun getDataPointAt(position: Int): T {
        return cachedData[position]
    }

    override fun getDataPoints(): List<T> {
        return cachedData
    }

    enum class State {
        Unloaded, Loading, Loaded
    }

    abstract val name: String

    protected val cachedData = ArrayList<T>()

    val numDataPoints: Int get() = cachedData.size

    private var _stateSubject: BehaviorSubject<State> = BehaviorSubject.create(State.Unloaded)
    val stateObservable: Observable<State> get() = _stateSubject

    private val internalSubscriptions = CompositeSubscription()

    private val queryRange = TimeSpan()
    protected var currentGranularity: Granularity = Granularity.WEEK
        private set

    var currentState = _stateSubject.value!!
        private set(value) {
            _stateSubject.onNext(value)
        }

    private var invalidated = false

    protected open fun onNewDataLoaded(data: List<T>) {
        this.cachedData.clear()
        this.cachedData.addAll(data)
    }

    fun reload() {
        if ((currentState == State.Unloaded) || (currentState == State.Loaded && invalidated)) {
            val lastState = currentState
            currentState = State.Loading
            invalidated = false
            internalSubscriptions.add(
                    reloadData().observeOn(AndroidSchedulers.mainThread()).subscribe({
                        data ->
                        onNewDataLoaded(data)
                        currentState = State.Loaded
                        if (invalidated == true) {
                            reload()
                        }
                    }, {
                        ex ->
                        ex.printStackTrace()
                        invalidated = true
                        currentState = lastState
                    })
            )
        } else {
            invalidate()
        }
    }

    open fun cancelLoading() {
        internalSubscriptions.clear()
    }

    abstract fun reloadData(): Observable<List<T>>

    open fun recycle() {
        internalSubscriptions.clear()
    }

    abstract fun getChartDrawer(): AChartDrawer

    fun invalidate() {
        invalidated = true
    }

    fun setTimeScope(time: Long, scope: Granularity) {
        scope.convertToRange(time, queryRange)
        currentGranularity = scope
        invalidate()
    }


    fun getCurrentScopeGranularity(): Granularity {
        return currentGranularity
    }

    fun getTimeScope(): TimeSpan {
        return queryRange
    }


}