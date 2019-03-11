package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.android.common.onNextIfDifferAndNotNull
import kr.ac.snu.hcil.omnitrack.OTAndroidApp
import kr.ac.snu.hcil.omnitrack.OTApp
import kr.ac.snu.hcil.omnitrack.core.di.global.ResearchSync
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.core.workers.OTResearchSynchronizationWorker
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import javax.inject.Inject
import javax.inject.Provider

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


    @field:[Inject ResearchSync]
    protected lateinit var researchSyncRequest: Provider<OneTimeWorkRequest>

    val syncStateObservable: Observable<Int> get() = _syncStateSubject

    private val pullSyncSubscription = SerialDisposable()

    private val _syncStateSubject = BehaviorSubject.createDefault(SYNC_STATE_SUCCESSFUL)

    override fun onInject(app: OTAndroidApp) {
        getApplication<OTApp>().applicationComponent.inject(this)
    }

    override fun onUserDisposed() {
        super.onUserDisposed()
        pullSyncSubscription.set(null)
    }

    fun startPullSync():Boolean{
        if(pullSyncSubscription.get() != null)
        {
            return false
        }else{
            pullSyncSubscription.replace(
                    syncManager.makeSynchronizationTask(SyncQueueDbHelper.AggregatedSyncQueue(IntArray(0), ESyncDataType.values().map { Triple(it, SyncDirection.DOWNLOAD, false) }.toTypedArray()))
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

    fun syncResearch(): Boolean {
        WorkManager.getInstance().enqueueUniqueWork(OTResearchSynchronizationWorker.TAG, ExistingWorkPolicy.REPLACE, researchSyncRequest.get())
        return true
    }
}