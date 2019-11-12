package kr.ac.snu.hcil.omnitrack.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kr.ac.snu.hcil.omnitrack.core.database.BackendDbManager
import kr.ac.snu.hcil.omnitrack.core.net.ISynchronizationClientSideAPI
import kr.ac.snu.hcil.omnitrack.core.synchronization.SyncQueueDbHelper
import javax.inject.Singleton

/**
 * Created by younghokim on 2017. 11. 4..
 */
@Module(includes = [BackendDatabaseModule::class])
class SynchronizationModule {

    @Provides
    @Singleton
    fun provideSyncDbHelper(appContext: Context): SyncQueueDbHelper
    {
        return SyncQueueDbHelper(appContext, "db_synchronization_queue.sqlite")
    }

    @Provides
    @Singleton
    fun provideClientSideApi(backendDatabaseManager: BackendDbManager): ISynchronizationClientSideAPI
    {
        return backendDatabaseManager
    }
}



