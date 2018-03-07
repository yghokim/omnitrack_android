package kr.ac.snu.hcil.omnitrack.ui.pages.home

import android.app.Application
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Job
import dagger.Lazy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.SerialDisposable
import io.reactivex.subjects.BehaviorSubject
import kr.ac.snu.hcil.omnitrack.core.configuration.ConfiguredContext
import kr.ac.snu.hcil.omnitrack.core.di.configured.ResearchSync
import kr.ac.snu.hcil.omnitrack.core.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.synchronization.OTSyncManager
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.ui.viewmodels.UserAttachedViewModel
import kr.ac.snu.hcil.omnitrack.utils.onNextIfDifferAndNotNull
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

    @Inject
    protected lateinit var dispatcher: Lazy<FirebaseJobDispatcher>

    @field:[Inject ResearchSync]
    protected lateinit var researchSyncJob: Provider<Job>

    val syncStateObservable: Observable<Int> get() = _syncStateSubject

    private val pullSyncSubscription = SerialDisposable()

    private val _syncStateSubject = BehaviorSubject.createDefault(SYNC_STATE_SUCCESSFUL)

    override fun onInject(configuredContext: ConfiguredContext) {
        configuredContext.configuredAppComponent.inject(this)
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
        dispatcher.get().mustSchedule(researchSyncJob.get())
        return true
    }
}