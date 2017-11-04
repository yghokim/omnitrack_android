package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.database.local.RealmDatabaseManager
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.ESyncDataType
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncDirection
import kr.ac.snu.hcil.omnitrack.core.database.synchronization.SyncQueueDbHelper
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import javax.inject.Qualifier

/**
 * Created by younghokim on 2017. 11. 4..
 */
@ApplicationScope
@Module
class SynchronizationModule {

    @Provides
    @ApplicationScope
    fun provideSyncDbHelper(appContext: Context): SyncQueueDbHelper
    {
        return SyncQueueDbHelper(appContext, "db_synchronization_queue")
    }

    @Provides
    @ApplicationScope
    fun provideClientSideApi(backendDatabaseManager: RealmDatabaseManager): ISynchronizationClientSideAPI
    {
        return backendDatabaseManager
    }

    @Provides
    @ApplicationScope
    @FullSync
    fun provideFullSyncQueue(): SyncQueueDbHelper.AggregatedSyncQueue
    {
        return SyncQueueDbHelper.AggregatedSyncQueue(
                IntArray(0),
                ESyncDataType.values().map { Pair(it, SyncDirection.BIDIRECTIONAL) }.toTypedArray()
        )
    }
}


@Qualifier
@Retention(AnnotationRetention.RUNTIME) annotation class FullSync


