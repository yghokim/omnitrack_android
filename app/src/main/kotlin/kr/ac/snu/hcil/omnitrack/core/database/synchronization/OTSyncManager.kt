package kr.ac.snu.hcil.omnitrack.core.database.synchronization

import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.Job
import dagger.Lazy
import kr.ac.snu.hcil.omnitrack.core.di.ApplicationScope
import kr.ac.snu.hcil.omnitrack.core.di.ServerSyncOneShot
import javax.inject.Inject
import javax.inject.Provider

/**
 * Created by younghokim on 2017. 9. 27..
 */
@ApplicationScope
class OTSyncManager @Inject constructor(val syncQueueDbHelper: SyncQueueDbHelper, @ServerSyncOneShot val oneShotJobProvider: Provider<Job>, val dispatcher: Lazy<FirebaseJobDispatcher>) {

    fun registerSyncQueue(type: ESyncDataType, direction: SyncDirection, reserveService: Boolean = true) {
        syncQueueDbHelper.insertNewEntry(type, direction, System.currentTimeMillis())
        if (reserveService) {
            reserveSyncServiceNow()
        }
    }

    fun queueFullSync() {
        ESyncDataType.values().forEach { type ->
            syncQueueDbHelper.insertNewEntry(type, SyncDirection.BIDIRECTIONAL, System.currentTimeMillis())
        }
    }

    fun reserveSyncServiceNow() {
        dispatcher.get().mustSchedule(oneShotJobProvider.get())
    }
}