package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.Application
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
import javax.inject.Inject

/**
 * Created by younghokim on 2017-11-09.
 */
class HomeScreenViewModel(app: Application):UserAttachedViewModel(app) {

    companion object {
        const val SYNC_STATE_SUCCESSFUL = 0
        const val SYNC_STATE_SYNCHRONIZING = 1
        const val SYNC_STATE_FAILED = 2
    }

    @Inject
    protected lateinit var syncManager: OTSyncManager

    val syncStateObservable: Observable<Int> get() = _syncStateSubject

    private val pullSyncSubscription = SerialDisposable()

    private val _syncStateSubject = BehaviorSubject.createDefault(SYNC_STATE_SUCCESSFUL)

    override fun onInject(app: OTApp) {
        app.synchronizationComponent.inject(this)
    }

    fun startPullSync():Boolean{
        if(pullSyncSubscription.get() != null)
        {
            return false
        }else{
            pullSyncSubscription.replace(
                    syncManager.makeSynchronizationTask(SyncQueueDbHelper.AggregatedSyncQueue(IntArray(0), ESyncDataType.values().map { Pair(it, SyncDirection.DOWNLOAD) }.toTypedArray()))
                            .doOnSubscribe {
                                _syncStateSubject.onNextIfDifferAndNotNull(SYNC_STATE_SYNCHRONIZING)
                            }
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({
                                _syncStateSubject.onNextIfDifferAndNotNull(SYNC_STATE_SUCCESSFUL)
                            }, {
                                _syncStateSubject.onNextIfDifferAndNotNull(SYNC_STATE_FAILED)})
            )
            return true
        }
    }
}