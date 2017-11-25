package kr.ac.snu.hcil.omnitrack.core.visualization

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.realm.Realm
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.datatypes.TimeSpan
import kr.ac.snu.hcil.omnitrack.core.visualization.interfaces.IChartInterface
import javax.inject.Inject

/**
 * Created by younghokim on 16. 9. 7..
 */
abstract class ChartModel<T>(val realm: Realm) : IChartInterface<T> {

    @Inject
    protected lateinit var dbManager: RealmDatabaseManager

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

    private var _stateSubject: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Unloaded)
    val stateObservable: Observable<State> get() = _stateSubject

    private val internalSubscriptions = CompositeDisposable()

    private val queryRange = TimeSpan()
    protected var currentGranularity: Granularity = Granularity.WEEK
        private set

    var currentState = _stateSubject.value!!
        private set(value) {
            _stateSubject.onNext(value)
        }

    private var invalidated = false

    protected open fun onNewDataLoaded(data: List<T>) {
        println("new chart data loaded and cache : ${data.size}")
        println(data)
        this.cachedData.clear()
        this.cachedData.addAll(data)
        currentState = State.Loaded
    }

    fun reload() {
        if ((currentState == State.Unloaded) || (currentState == State.Loaded && invalidated)) {
            val lastState = currentState
            currentState = State.Loading
            invalidated = false
            println("start loading chart data: ${this.name}")
            internalSubscriptions.add(
                    reloadData().observeOn(AndroidSchedulers.mainThread()).subscribe({
                        data ->

                        println("chart data loading finished: ${this.name}")
                        onNewDataLoaded(data)
                        if (invalidated) {
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

            println("chart loading is already doing. invalidate current configuration instead: ${this.name}")
            invalidate()
        }
    }

    open fun cancelLoading() {
        internalSubscriptions.clear()
    }

    abstract fun reloadData(): Single<List<T>>

    open fun recycle() {
        internalSubscriptions.clear()
    }

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